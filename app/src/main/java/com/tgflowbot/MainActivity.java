package com.tgflowbot;

import android.content.ClipData;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.OpenableColumns;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import com.tgflowbot.model.Connection;
import com.tgflowbot.model.FlowNode;
import com.tgflowbot.model.NodeType;
import com.tgflowbot.model.Workflow;
import com.tgflowbot.telegram.ExtensionModule;
import com.tgflowbot.telegram.MethodRegistry;
import com.tgflowbot.telegram.ParamDef;
import com.tgflowbot.telegram.TelegramMethod;
import com.tgflowbot.telegram.TelegramHelper;
import com.tgflowbot.telegram.ai.AiChatHelper;
import com.tgflowbot.telegram.ai.AiProvider;
import com.tgflowbot.view.FlowCanvasView;
import com.tgflowbot.view.NodeAdapter;
import com.tgflowbot.view.WorkflowAdapter;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private FlowCanvasView canvas;
    private Workflow workflow;
    private FlowNode selectedNode;
    private String lastChatId;
    private final ArrayList<String> logEntries = new ArrayList<>();
    private final Object logLock = new Object();
    private LogAdapter logAdapter;
    private boolean isRunning = false;
    private boolean pollingInProgress = false;
    private boolean dirty = false;
    private MenuItem runMenuItem;
    private final Map<String, String> variables = new HashMap<>();
    private Map<String, String> currentMsgData = new HashMap<>();
    private final Handler bgHandler = new Handler(Looper.getMainLooper());
    private final Runnable pollRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isRunning) return;
            pollMessagesOnce(false);
        }
    };

    private static final String PREFS_NAME = "tgflowbot";
    private static final String WORKFLOW_KEY = "workflow";

    private DrawerLayout drawerLayout;
    private NodeAdapter triggerAdapter, actionTgAdapter, actionAiAdapter,
            actionMathAdapter, actionTextAdapter, actionVarsAdapter, actionFlowAdapter,
            actionPhoneAdapter, actionListAdapter, actionOpAdapter, actionFileAdapter,
            actionHttpAdapter, conditionAdapter, outputAdapter;

    private final ActivityResultLauncher<String> exportLauncher =
            registerForActivityResult(new ActivityResultContracts.CreateDocument("application/json"), uri -> {
                if (uri != null) exportWorkflowToUri(uri);
            });
    private final ActivityResultLauncher<String[]> importLauncher =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri != null) importWorkflowFromUri(uri);
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawerLayout = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.open_drawer, R.string.close_drawer);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        canvas = findViewById(R.id.canvas);
        workflow = new Workflow();

        String workflowData = getIntent().getStringExtra("workflow_data");
        String workflowName = getIntent().getStringExtra("workflow_name");
        if (workflowData != null && !workflowData.isEmpty()) {
            try {
                Workflow loaded = new Gson().fromJson(workflowData, Workflow.class);
                if (loaded != null) {
                    workflow = loaded;
                    backfillSubcategories(workflow);
                    workflow.deduplicateConnections();
                }
            } catch (Exception ignored) {}
        }
        if (workflowName != null && !workflowName.isEmpty()) {
            toolbar.setTitle(workflowName);
        }
        canvas.setWorkflow(workflow);

        canvas.setNodeActionListener(new FlowCanvasView.OnNodeActionListener() {
            @Override
            public void onNodeSelected(FlowNode node) {
                selectedNode = node;
            }

            @Override
            public void onNodeDoubleTap(FlowNode node) {
                openNodeEditor(node);
            }

            @Override
            public void onNodeLongPress(FlowNode node) {
                showNodeActions(node);
            }

            @Override
            public void onConnectionCreated(Connection connection) {
                Snackbar.make(canvas, "Koneksi dibuat", Snackbar.LENGTH_SHORT).show();
            }

            @Override
            public void onNodeDropped(FlowNode node) {
                String methodName = node.getProperty("_method");
                if (node.getType() == NodeType.ACTION && methodName != null && node.getProperty("_subcat") == null) {
                    node.putProperty("_subcat", getActionSubcategory(methodName));
                }
                setDirty();
                Snackbar.make(canvas, node.getLabel() + " ditambahkan", Snackbar.LENGTH_SHORT).show();
            }

            @Override
            public void onCanvasLongPress() {
                drawerLayout.openDrawer(findViewById(R.id.drawer_panel));
            }
        });

        lastChatId = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .getString("last_chat_id", "");

        loadAndRegisterExtensions();
        setupNodePalette();
    }

    private void loadAndRegisterExtensions() {
        Set<String> installed = loadInstalledPackageIds();
        List<ExtensionModule> marketplace = ExtensionModule.getMarketplaceExtensions();
        if (installed.isEmpty()) {
            // First run: auto-install every bundled extension so all nodes
            // are available out of the box. Users can still remove any of
            // them later from the Extension Marketplace.
            for (ExtensionModule ext : marketplace) {
                installed.add(ext.packageId);
            }
            saveInstalledPackageIds(installed);
        }
        MethodRegistry.clearExtensions();
        for (ExtensionModule ext : marketplace) {
            if (installed.contains(ext.packageId)) {
                MethodRegistry.registerExtension(ext);
            }
        }
    }

    private Set<String> loadInstalledPackageIds() {
        String json = getSharedPreferences("extensions", MODE_PRIVATE)
                .getString("installed_packages", "[]");
        Type type = new TypeToken<Set<String>>(){}.getType();
        Set<String> set = new Gson().fromJson(json, type);
        if (set == null) set = new HashSet<>();
        return set;
    }

    private void saveInstalledPackageIds(Set<String> ids) {
        getSharedPreferences("extensions", MODE_PRIVATE)
                .edit()
                .putString("installed_packages", new Gson().toJson(ids))
                .apply();
    }

    private void setupNodePalette() {
        List<NodeAdapter.NodeItem> triggers = new ArrayList<>();
        List<NodeAdapter.NodeItem> actionsTg = new ArrayList<>();
        List<NodeAdapter.NodeItem> actionsAi = new ArrayList<>();
        List<NodeAdapter.NodeItem> actionsMath = new ArrayList<>();
        List<NodeAdapter.NodeItem> actionsText = new ArrayList<>();
        List<NodeAdapter.NodeItem> actionsVars = new ArrayList<>();
        List<NodeAdapter.NodeItem> actionsFlow = new ArrayList<>();
        List<NodeAdapter.NodeItem> actionsPhone = new ArrayList<>();
        List<NodeAdapter.NodeItem> actionsList = new ArrayList<>();
        List<NodeAdapter.NodeItem> actionsOp = new ArrayList<>();
        List<NodeAdapter.NodeItem> actionsFile = new ArrayList<>();
        List<NodeAdapter.NodeItem> actionsHttp = new ArrayList<>();
        List<NodeAdapter.NodeItem> conditions = new ArrayList<>();
        List<NodeAdapter.NodeItem> outputs = new ArrayList<>();

        for (TelegramMethod m : MethodRegistry.getAllMethods()) {
            String subCat = m.nodeType == NodeType.ACTION ? getActionSubcategory(m.apiName) : null;
            NodeAdapter.NodeItem item = new NodeAdapter.NodeItem(
                    m.displayName, m.description, m.nodeType, m.apiName, subCat);
            if (m.nodeType == NodeType.ACTION) {
                switch (subCat) {
                    case "tg": actionsTg.add(item); break;
                    case "ai": actionsAi.add(item); break;
                    case "math": actionsMath.add(item); break;
                    case "text": actionsText.add(item); break;
                    case "vars": actionsVars.add(item); break;
                    case "flow": actionsFlow.add(item); break;
                    case "phone": actionsPhone.add(item); break;
                    case "list": actionsList.add(item); break;
                    case "op": actionsOp.add(item); break;
                    case "file": actionsFile.add(item); break;
                    case "http": actionsHttp.add(item); break;
                    default: actionsTg.add(item);
                }
            } else {
                switch (m.nodeType) {
                    case TRIGGER: triggers.add(item); break;
                    case CONDITION: conditions.add(item); break;
                    case OUTPUT: outputs.add(item); break;
                }
            }
        }

        NodeAdapter.OnNodeClickListener clickListener = item -> {
            drawerLayout.closeDrawer(findViewById(R.id.drawer_panel));
            FlowNode node = new FlowNode(item.name, item.type, 100f, 100f);
            if (item.methodName != null) node.putProperty("_method", item.methodName);
            if (item.subcategory != null) node.putProperty("_subcat", item.subcategory);
            addNodeToCanvas(node);
        };

        NodeAdapter.OnNodeDragListener dragListener = (item, view) -> {
            String label = item.name + "|" + item.type.name();
            ClipData clipData = ClipData.newPlainText(label, item.methodName != null ? item.methodName : "");
            View.DragShadowBuilder shadow = new View.DragShadowBuilder(view);
            view.startDragAndDrop(clipData, shadow, null, 0);
        };

        setupDropdown(findViewById(R.id.section_trigger), (RecyclerView) findViewById(R.id.rv_triggers), triggers.isEmpty());
        triggerAdapter = new NodeAdapter(triggers, clickListener);
        triggerAdapter.setOnNodeDragListener(dragListener);
        ((RecyclerView) findViewById(R.id.rv_triggers)).setAdapter(triggerAdapter);

        setupDropdown(findViewById(R.id.sub_tg), (RecyclerView) findViewById(R.id.rv_actions_tg), actionsTg.isEmpty());
        actionTgAdapter = new NodeAdapter(actionsTg, clickListener);
        actionTgAdapter.setOnNodeDragListener(dragListener);
        ((RecyclerView) findViewById(R.id.rv_actions_tg)).setAdapter(actionTgAdapter);

        setupDropdown(findViewById(R.id.sub_ai), (RecyclerView) findViewById(R.id.rv_actions_ai), actionsAi.isEmpty());
        actionAiAdapter = new NodeAdapter(actionsAi, clickListener);
        actionAiAdapter.setOnNodeDragListener(dragListener);
        ((RecyclerView) findViewById(R.id.rv_actions_ai)).setAdapter(actionAiAdapter);

        setupDropdown(findViewById(R.id.sub_math), (RecyclerView) findViewById(R.id.rv_actions_math), actionsMath.isEmpty());
        actionMathAdapter = new NodeAdapter(actionsMath, clickListener);
        actionMathAdapter.setOnNodeDragListener(dragListener);
        ((RecyclerView) findViewById(R.id.rv_actions_math)).setAdapter(actionMathAdapter);

        setupDropdown(findViewById(R.id.sub_text), (RecyclerView) findViewById(R.id.rv_actions_text), actionsText.isEmpty());
        actionTextAdapter = new NodeAdapter(actionsText, clickListener);
        actionTextAdapter.setOnNodeDragListener(dragListener);
        ((RecyclerView) findViewById(R.id.rv_actions_text)).setAdapter(actionTextAdapter);

        setupDropdown(findViewById(R.id.sub_vars), (RecyclerView) findViewById(R.id.rv_actions_vars), actionsVars.isEmpty());
        actionVarsAdapter = new NodeAdapter(actionsVars, clickListener);
        actionVarsAdapter.setOnNodeDragListener(dragListener);
        ((RecyclerView) findViewById(R.id.rv_actions_vars)).setAdapter(actionVarsAdapter);

        setupDropdown(findViewById(R.id.sub_flow), (RecyclerView) findViewById(R.id.rv_actions_flow), actionsFlow.isEmpty());
        actionFlowAdapter = new NodeAdapter(actionsFlow, clickListener);
        actionFlowAdapter.setOnNodeDragListener(dragListener);
        ((RecyclerView) findViewById(R.id.rv_actions_flow)).setAdapter(actionFlowAdapter);

        setupDropdown(findViewById(R.id.sub_phone), (RecyclerView) findViewById(R.id.rv_actions_phone), actionsPhone.isEmpty());
        actionPhoneAdapter = new NodeAdapter(actionsPhone, clickListener);
        actionPhoneAdapter.setOnNodeDragListener(dragListener);
        ((RecyclerView) findViewById(R.id.rv_actions_phone)).setAdapter(actionPhoneAdapter);

        setupDropdown(findViewById(R.id.sub_list), (RecyclerView) findViewById(R.id.rv_actions_list), actionsList.isEmpty());
        actionListAdapter = new NodeAdapter(actionsList, clickListener);
        actionListAdapter.setOnNodeDragListener(dragListener);
        ((RecyclerView) findViewById(R.id.rv_actions_list)).setAdapter(actionListAdapter);

        setupDropdown(findViewById(R.id.sub_op), (RecyclerView) findViewById(R.id.rv_actions_op), actionsOp.isEmpty());
        actionOpAdapter = new NodeAdapter(actionsOp, clickListener);
        actionOpAdapter.setOnNodeDragListener(dragListener);
        ((RecyclerView) findViewById(R.id.rv_actions_op)).setAdapter(actionOpAdapter);

        setupDropdown(findViewById(R.id.sub_file), (RecyclerView) findViewById(R.id.rv_actions_file), actionsFile.isEmpty());
        actionFileAdapter = new NodeAdapter(actionsFile, clickListener);
        actionFileAdapter.setOnNodeDragListener(dragListener);
        ((RecyclerView) findViewById(R.id.rv_actions_file)).setAdapter(actionFileAdapter);

        setupDropdown(findViewById(R.id.sub_http), (RecyclerView) findViewById(R.id.rv_actions_http), actionsHttp.isEmpty());
        actionHttpAdapter = new NodeAdapter(actionsHttp, clickListener);
        actionHttpAdapter.setOnNodeDragListener(dragListener);
        ((RecyclerView) findViewById(R.id.rv_actions_http)).setAdapter(actionHttpAdapter);

        setupDropdown(findViewById(R.id.section_condition), (RecyclerView) findViewById(R.id.rv_conditions), conditions.isEmpty());
        conditionAdapter = new NodeAdapter(conditions, clickListener);
        conditionAdapter.setOnNodeDragListener(dragListener);
        ((RecyclerView) findViewById(R.id.rv_conditions)).setAdapter(conditionAdapter);

        setupDropdown(findViewById(R.id.section_output), (RecyclerView) findViewById(R.id.rv_outputs), outputs.isEmpty());
        outputAdapter = new NodeAdapter(outputs, clickListener);
        outputAdapter.setOnNodeDragListener(dragListener);
        ((RecyclerView) findViewById(R.id.rv_outputs)).setAdapter(outputAdapter);

        TextInputEditText searchInput = findViewById(R.id.search_input);
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                filterPalette(s.toString());
            }
        });

        filterPalette("");
    }

    private String getActionSubcategory(String apiName) {
        if (apiName == null) return "tg";
        if (apiName.equals("ai_chat")) return "ai";
        if (apiName.startsWith("_add") || apiName.startsWith("_subtract")
                || apiName.startsWith("_multiply") || apiName.startsWith("_divide")
                || apiName.startsWith("_modulo") || apiName.startsWith("_random"))
            return "math";
        if (apiName.startsWith("_text_"))
            return "text";
        if (apiName.startsWith("_set_") || apiName.startsWith("_get_")
                || apiName.startsWith("_var_"))
            return "vars";
        if (apiName.startsWith("_delay") || apiName.equals("_return")
                || apiName.startsWith("_repeat") || apiName.startsWith("_wait_")
                || apiName.equals("_loop_break") || apiName.startsWith("_loop_"))
            return "flow";
        if (apiName.startsWith("_list_") || apiName.startsWith("_data_") || apiName.startsWith("_date_time"))
            return "list";
        if (apiName.startsWith("_power") || apiName.startsWith("_sqrt")
                || apiName.startsWith("_abs") || apiName.startsWith("_round")
                || apiName.startsWith("_floor") || apiName.startsWith("_ceil")
                || apiName.startsWith("_min") || apiName.startsWith("_max")
                || apiName.startsWith("_clamp"))
            return "op";
        if (apiName.startsWith("_file_"))
            return "file";
        if (apiName.startsWith("_http_request"))
            return "http";
        if (apiName.equals("_log") || apiName.startsWith("_switch"))
            return "flow";
        if (apiName.startsWith("_phone_"))
            return "phone";
        return "tg";
    }

    private final Set<Integer> expandedSections = new HashSet<>();

    private void setupDropdown(View header, RecyclerView rv, boolean empty) {
        if (empty) {
            header.setVisibility(View.GONE);
            rv.setVisibility(View.GONE);
            return;
        }
        header.setVisibility(View.VISIBLE);
        rv.setVisibility(View.VISIBLE);
        expandedSections.add(header.getId());
        if (header instanceof TextView) {
            ((TextView) header).setText(((TextView) header).getText() + "  ▾");
        }
        header.setClickable(true);
        TypedValue tv = new TypedValue();
        header.getContext().getTheme().resolveAttribute(android.R.attr.selectableItemBackground, tv, true);
        header.setBackgroundResource(tv.resourceId);
        header.setOnClickListener(v -> {
            int id = v.getId();
            boolean expanded = expandedSections.contains(id);
            rv.setVisibility(expanded ? View.GONE : View.VISIBLE);
            if (v instanceof TextView) {
                TextView tv2 = (TextView) v;
                String t = tv2.getText().toString();
                tv2.setText(expanded ? t.replace(" ▾", " ▸") : t.replace(" ▸", " ▾"));
            }
            if (expanded) expandedSections.remove(id);
            else expandedSections.add(id);
        });
    }

    private void filterPalette(String query) {
        filterSection(R.id.section_trigger, R.id.rv_triggers, query);
        filterSection(R.id.sub_tg, R.id.rv_actions_tg, query);
        filterSection(R.id.sub_ai, R.id.rv_actions_ai, query);
        filterSection(R.id.sub_math, R.id.rv_actions_math, query);
        filterSection(R.id.sub_text, R.id.rv_actions_text, query);
        filterSection(R.id.sub_vars, R.id.rv_actions_vars, query);
        filterSection(R.id.sub_flow, R.id.rv_actions_flow, query);
        filterSection(R.id.sub_list, R.id.rv_actions_list, query);
        filterSection(R.id.sub_op, R.id.rv_actions_op, query);
        filterSection(R.id.sub_file, R.id.rv_actions_file, query);
        filterSection(R.id.sub_http, R.id.rv_actions_http, query);
        filterSection(R.id.sub_phone, R.id.rv_actions_phone, query);
        filterSection(R.id.section_condition, R.id.rv_conditions, query);
        filterSection(R.id.section_output, R.id.rv_outputs, query);
    }

    private void filterSection(int sectionId, int rvId, String query) {
        View section = findViewById(sectionId);
        RecyclerView rv = findViewById(rvId);
        if (section == null || rv == null) return;
        NodeAdapter adapter = (NodeAdapter) rv.getAdapter();
        if (adapter == null || adapter.getItemCount() == 0) {
            section.setVisibility(View.GONE);
            rv.setVisibility(View.GONE);
            return;
        }
        boolean visible = query.isEmpty() || anyMatch(adapter, query);
        section.setVisibility(visible ? View.VISIBLE : View.GONE);
        rv.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    private boolean anyMatch(NodeAdapter adapter, String query) {
        String q = query.toLowerCase();
        for (int i = 0; i < adapter.getItemCount(); i++) {
            NodeAdapter.NodeItem item = adapter.getItems().get(i);
            if (item.name.toLowerCase().contains(q)
                    || item.description.toLowerCase().contains(q)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        runMenuItem = menu.findItem(R.id.action_run);
        updateRunIcon();
        setOptionalIconsVisible(menu);
        return true;
    }

    private void setOptionalIconsVisible(Menu menu) {
        try {
            java.lang.reflect.Method m = menu.getClass().getDeclaredMethod(
                    "setOptionalIconsVisible", Boolean.TYPE);
            m.setAccessible(true);
            m.invoke(menu, true);
        } catch (Exception ignored) {}
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_save) {
            saveWorkflow();
            return true;
        } else if (id == R.id.action_run) {
            toggleWorkflow();
            return true;
        } else if (id == R.id.action_view_log) {
            showLogViewer();
            return true;
        } else if (id == R.id.action_settings) {
            showSettingsDialog();
            return true;
        } else if (id == R.id.action_delete_all) {
            confirmDeleteAll();
            return true;
        } else if (id == R.id.action_about) {
            showAboutDialog();
            return true;
        } else if (id == R.id.action_export) {
            exportWorkflow();
            return true;
        } else if (id == R.id.action_import) {
            importWorkflow();
            return true;
        } else if (id == R.id.action_report_bug) {
            showReportBugDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void addNodeToCanvas(FlowNode node) {
        canvas.addNode(node);
        setDirty();
        Snackbar.make(canvas, node.getLabel() + " ditambahkan — seret untuk pindah", Snackbar.LENGTH_SHORT).show();
    }

    private void openNodeEditor(FlowNode node) {
        Intent intent = new Intent(this, NodeEditorActivity.class);
        intent.putExtra("node_id", node.getId());
        intent.putExtra("node_label", node.getLabel());
        intent.putExtra("node_type", node.getType().name());
        intent.putExtra("method_name", node.getProperty("_method"));
        Gson gson = new Gson();
        intent.putExtra("node_properties", gson.toJson(node.getProperties()));
        startActivityForResult(intent, 100);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100 && resultCode == RESULT_OK && data != null) {
            String nodeId = data.getStringExtra("node_id");
            String label = data.getStringExtra("node_label");
            String propertiesJson = data.getStringExtra("node_properties");
            boolean delete = data.getBooleanExtra("delete", false);

            FlowNode node = workflow.findNodeById(nodeId);
            if (node != null) {
                if (delete) {
                    canvas.removeSelectedNode();
                    selectedNode = null;
                    setDirty();
                    Snackbar.make(canvas, "Node dihapus", Snackbar.LENGTH_SHORT).show();
                } else {
                    node.setLabel(label);
                    Gson gson = new Gson();
                    Type type = new TypeToken<java.util.Map<String, String>>(){}.getType();
                    java.util.Map<String, String> props = gson.fromJson(propertiesJson, type);
                    if (props != null) {
                        String savedMethod = node.getProperty("_method");
                        node.setProperties(props);
                        if (savedMethod != null) {
                            node.putProperty("_method", savedMethod);
                        }
                    }
                    setDirty();
                    canvas.invalidate();
                    Snackbar.make(canvas, "Node diperbarui", Snackbar.LENGTH_SHORT).show();
                }
            }
        } else if (requestCode == REQUEST_PICK_FILE && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null) {
                uploadFileWithUri(uri);
            }
        }
    }

    private void toggleWorkflow() {
        if (isRunning && dirty) {
            stopBackgroundPolling();
            dirty = false;
            startBackgroundPolling();
        } else if (isRunning) {
            stopBackgroundPolling();
        } else {
            startBackgroundPolling();
        }
    }

    private void startBackgroundPolling() {
        boolean hasTelegramTrigger = false;
        FlowNode listeningNode = null;
        FlowNode scheduleNode = null;
        FlowNode intervalNode = null;
        FlowNode httpPollNode = null;
        FlowNode webhookNode = null;
        FlowNode manualNode = null;
        for (FlowNode n : workflow.getNodes()) {
            if (n.getType() != NodeType.TRIGGER) continue;
            String mName = n.getProperty("_method");
            if (mName == null) {
                TelegramMethod m = findMethodByLabel(n.getLabel());
                if (m != null) mName = m.apiName;
            }
            if (mName != null && "_on_listening".equals(mName)) {
                listeningNode = n;
            } else if (mName != null && "_on_schedule".equals(mName)) {
                scheduleNode = n;
            } else if (mName != null && "_on_interval".equals(mName)) {
                intervalNode = n;
            } else if (mName != null && "_on_http_poll".equals(mName)) {
                httpPollNode = n;
            } else if (mName != null && "_on_webhook".equals(mName)) {
                webhookNode = n;
            } else if (mName != null && "_on_manual".equals(mName)) {
                manualNode = n;
            } else if (mName != null) {
                hasTelegramTrigger = true;
            }
        }
        if (manualNode != null) {
            isRunning = true;
            updateRunIcon();
            startManualTrigger(manualNode);
            return;
        }
        if (!hasTelegramTrigger && listeningNode != null) {
            String prompt = listeningNode.getProperty("prompt");
            String timeoutRaw = listeningNode.getProperty("timeout_sec");
            int timeoutSec = 10;
            try { timeoutSec = Integer.parseInt(timeoutRaw); } catch (Exception ignored) {}
            if (prompt == null || prompt.isEmpty()) prompt = "Silakan bicara";
            isRunning = true;
            updateRunIcon();
            addLog("On Listening: mendengarkan...");
            startSttForTrigger(prompt, timeoutSec, listeningNode);
            return;
        }
        if (scheduleNode != null) {
            isRunning = true;
            updateRunIcon();
            startScheduleTrigger(scheduleNode);
        }
        if (intervalNode != null) {
            isRunning = true;
            updateRunIcon();
            startIntervalTrigger(intervalNode);
        }
        if (httpPollNode != null) {
            isRunning = true;
            updateRunIcon();
            startHttpPollTrigger(httpPollNode);
        }
        if (webhookNode != null) {
            isRunning = true;
            updateRunIcon();
            startWebhookTrigger(webhookNode);
        }
        if (!hasTelegramTrigger && listeningNode == null && scheduleNode == null && intervalNode == null && httpPollNode == null && webhookNode == null) {
            Snackbar.make(canvas, "Tidak ada trigger aktif", Snackbar.LENGTH_SHORT).show();
            return;
        }
        if (hasTelegramTrigger) {
            String token = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                    .getString("bot_token", "");
            if (token.isEmpty()) {
                Snackbar.make(canvas, "Setel BOT_TOKEN dulu",
                        Snackbar.LENGTH_LONG)
                        .setAction("SETTING", v -> showSettingsDialog())
                        .show();
                return;
            }
            isRunning = true;
            dirty = false;
            updateRunIcon();
            Snackbar.make(canvas, "Workflow started (background)", Snackbar.LENGTH_SHORT).show();
            bgHandler.post(pollRunnable);
        }
    }

    private void stopBackgroundPolling() {
        isRunning = false;
        bgHandler.removeCallbacks(pollRunnable);
        updateRunIcon();
        canvas.clearFlowPath();
        Snackbar.make(canvas, "Workflow stopped", Snackbar.LENGTH_SHORT).show();
    }

    private void updateRunIcon() {
        if (runMenuItem == null) return;
        if (isRunning && dirty) {
            runMenuItem.setIcon(R.drawable.ic_restart);
            runMenuItem.setTitle("Restart");
        } else if (isRunning) {
            runMenuItem.setIcon(R.drawable.ic_stop);
            runMenuItem.setTitle("Stop");
        } else {
            runMenuItem.setIcon(R.drawable.ic_play);
            runMenuItem.setTitle("Start");
        }
    }

    private void setDirty() {
        dirty = true;
        updateRunIcon();
    }

    @Override
    protected void onPause() {
        super.onPause();
        saveWorkflow();
        if (isRunning) {
            bgHandler.removeCallbacks(pollRunnable);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isRunning) {
            bgHandler.post(pollRunnable);
        }
    }

    private void saveWorkflow() {
        String name = getIntent().getStringExtra("workflow_name");
        if (name != null && !name.isEmpty()) {
            saveToWorkflowList(name, new Gson().toJson(workflow));
        }
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .edit()
                .putString(WORKFLOW_KEY, new Gson().toJson(workflow))
                .apply();
        Snackbar.make(canvas, "Workflow disimpan", Snackbar.LENGTH_SHORT).show();
    }

    private void saveToWorkflowList(String name, String data) {
        String json = getSharedPreferences("workflow_list", MODE_PRIVATE)
                .getString("workflow_items", "[]");
        Type type = new TypeToken<List<WorkflowAdapter.WorkflowItem>>(){}.getType();
        List<WorkflowAdapter.WorkflowItem> items = new Gson().fromJson(json, type);
        if (items == null) return;

        for (WorkflowAdapter.WorkflowItem item : items) {
            if (item.name.equals(name)) {
                item.data = data;
                break;
            }
        }
        getSharedPreferences("workflow_list", MODE_PRIVATE)
                .edit()
                .putString("workflow_items", new Gson().toJson(items))
                .apply();
    }

    private void exportWorkflow() {
        String name = getIntent().getStringExtra("workflow_name");
        if (name == null || name.isEmpty()) name = "workflow";
        exportLauncher.launch(name + ".json");
    }

    private void exportWorkflowToUri(Uri uri) {
        try {
            OutputStream os = getContentResolver().openOutputStream(uri);
            if (os == null) return;
            OutputStreamWriter writer = new OutputStreamWriter(os);
            writer.write(new Gson().toJson(workflow));
            writer.flush();
            writer.close();
            Snackbar.make(canvas, "Workflow berhasil diexport", Snackbar.LENGTH_SHORT).show();
        } catch (Exception e) {
            Snackbar.make(canvas, "Gagal export: " + e.getMessage(), Snackbar.LENGTH_LONG).show();
        }
    }

    private void importWorkflow() {
        importLauncher.launch(new String[]{"application/json", "*/*"});
    }

    private void backfillSubcategories(Workflow wf) {
        for (FlowNode node : wf.getNodes()) {
            if (node.getType() == NodeType.ACTION && node.getProperty("_subcat") == null) {
                String methodName = node.getProperty("_method");
                if (methodName != null) node.putProperty("_subcat", getActionSubcategory(methodName));
            }
        }
    }

    private void importWorkflowFromUri(Uri uri) {
        try {
            InputStream is = getContentResolver().openInputStream(uri);
            if (is == null) return;
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
            reader.close();
            Workflow loaded = new Gson().fromJson(sb.toString(), Workflow.class);
            if (loaded == null) {
                Snackbar.make(canvas, "Gagal import: file tidak valid", Snackbar.LENGTH_LONG).show();
                return;
            }
            workflow = loaded;
            backfillSubcategories(workflow);
            workflow.deduplicateConnections();
            canvas.setWorkflow(workflow);
            canvas.invalidate();
            setDirty();
            saveWorkflow();
            Snackbar.make(canvas, "Workflow berhasil diimport", Snackbar.LENGTH_SHORT).show();
        } catch (Exception e) {
            Snackbar.make(canvas, "Gagal import: " + e.getMessage(), Snackbar.LENGTH_LONG).show();
        }
    }

    private void runWorkflow() {
        if (workflow.getNodes().isEmpty()) {
            Snackbar.make(canvas, "Workflow kosong!", Snackbar.LENGTH_SHORT).show();
            return;
        }

        boolean hasListening = false;
        for (FlowNode n : workflow.getNodes()) {
            if (n.getType() != NodeType.TRIGGER) continue;
            String mName = n.getProperty("_method");
            if (mName == null) {
                TelegramMethod m = findMethodByLabel(n.getLabel());
                if (m != null) mName = m.apiName;
            }
            if ("_on_listening".equals(mName)) {
                hasListening = true;
                String prompt = n.getProperty("prompt");
                String timeoutRaw = n.getProperty("timeout_sec");
                int timeoutSec = 10;
                try { timeoutSec = Integer.parseInt(timeoutRaw); } catch (Exception ignored) {}
                if (prompt == null || prompt.isEmpty()) prompt = "Silakan bicara";
                final String sttPrompt = prompt;
                final int sttTimeout = timeoutSec;
                final FlowNode triggerNode = n;
                addLog("On Listening: mendengarkan...");
                startSttForTrigger(sttPrompt, sttTimeout, triggerNode);
            }
        }
        if (hasListening) return;

        Snackbar.make(canvas, "Workflow dijalankan (trigger all triggers)...",
                Snackbar.LENGTH_SHORT).show();

        String sampleText = lastChatId != null && !lastChatId.isEmpty()
                ? "/start test" : "test";
        Map<String, String> testData = new HashMap<>();
        testData.put("text", sampleText);
        testData.put("chat.id", lastChatId != null ? lastChatId : "0");
        testData.put("message_id", "0");
        testData.put("date", String.valueOf(System.currentTimeMillis() / 1000));
        currentMsgData = testData;
        processIncomingMessage(lastChatId != null ? lastChatId : "0", "", sampleText);
    }

    private void pollMessagesOnce(boolean showSnackbar) {
        if (pollingInProgress) return;
        pollingInProgress = true;
        String token = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .getString("bot_token", "");
        if (token.isEmpty()) {
            pollingInProgress = false;
            return;
        }

        int offset = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .getInt("update_offset", 0);

        int finalOffset = offset;
        new Thread(() -> {
            try {
                okhttp3.OkHttpClient client = new okhttp3.OkHttpClient.Builder()
                        .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                        .readTimeout(20, java.util.concurrent.TimeUnit.SECONDS)
                        .writeTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                        .build();
                String url = "https://api.telegram.org/bot" + token
                        + "/getUpdates?offset=" + finalOffset + "&timeout=10";
                okhttp3.Request request = new okhttp3.Request.Builder().url(url).build();
                okhttp3.Response response = client.newCall(request).execute();
                if (response.isSuccessful() && response.body() != null) {
                    String body = response.body().string();
                    response.close();

                    com.google.gson.JsonObject json =
                            new com.google.gson.JsonParser().parse(body).getAsJsonObject();
                    com.google.gson.JsonArray result =
                            json.getAsJsonArray("result");
                    if (result != null && result.size() > 0) {
                        int maxId = finalOffset;
                        for (int i = 0; i < result.size(); i++) {
                            com.google.gson.JsonObject update =
                                    result.get(i).getAsJsonObject();
                            int updateId = update.get("update_id").getAsInt();
                            if (updateId >= maxId) maxId = updateId + 1;
                            processTelegramUpdate(update, maxId);
                        }
                        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                                .edit().putInt("update_offset", maxId).apply();
                    }
                }
            } catch (java.net.SocketTimeoutException ignored) {
            } catch (Exception e) {
                addLog("[Poll] " + e.getClass().getSimpleName() + ": " + e.getMessage());
            }
            pollingInProgress = false;
            if (isRunning) {
                bgHandler.postDelayed(pollRunnable, 3000);
            }
        }).start();
    }

    private void processIncomingMessage(String chatId, String userName, String text) {
        addLog("Pesan dari " + (userName.isEmpty() ? chatId : userName) + ": " + text);
        processTriggerByType("_on_message", chatId, userName, text);
    }

    private void processTelegramUpdate(com.google.gson.JsonObject update, int maxId) {
        String[][] updateTypes = {
            {"message", "_on_message"},
            {"edited_message", "_on_edited_message"},
            {"channel_post", "_on_channel_post"},
            {"callback_query", "_on_callback_query"},
            {"inline_query", "_on_inline_query"},
            {"chosen_inline_result", "_on_chosen_inline_result"},
            {"chat_member", "_on_chat_member"},
            {"my_chat_member", "_on_my_chat_member"},
            {"chat_join_request", "_on_chat_join_request"},
            {"poll", "_on_poll"},
            {"poll_answer", "_on_poll_answer"},
            {"pre_checkout_query", "_on_pre_checkout_query"},
            {"shipping_query", "_on_shipping_query"},
        };

        for (String[] ut : updateTypes) {
            String fieldName = ut[0];
            String triggerName = ut[1];
            if (update.has(fieldName) && !update.get(fieldName).isJsonNull()) {
                com.google.gson.JsonObject data = update.getAsJsonObject(fieldName);
                Map<String, String> msgData = new HashMap<>();
                flattenJson("", data, msgData);
                msgData.put("update_type", fieldName);
                msgData.put("trigger_type", triggerName);
                currentMsgData = msgData;

                String chatId = msgData.containsKey("chat.id") ? msgData.get("chat.id") : "";
                String text = msgData.get("text");
                if (text == null) {
                    String[] textFields = {"data", "query", "inline_query", "chosen_inline_result"};
                    for (String tf : textFields) {
                        if (msgData.containsKey(tf)) { text = msgData.get(tf); break; }
                    }
                }
                if (text == null) text = "";
                String userName = msgData.containsKey("from.username")
                        ? msgData.get("from.username")
                        : msgData.getOrDefault("from.first_name", "");
                if (chatId.isEmpty()) chatId = lastChatId != null ? lastChatId : "0";

                if ("message".equals(fieldName) && chatId.isEmpty()) {
                    String cId = msgData.get("chat.id");
                    if (cId != null) chatId = cId;
                }

                lastChatId = chatId;
                getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                        .edit()
                        .putString("last_chat_id", chatId)
                        .putInt("update_offset", maxId)
                        .apply();

                String finalChatId = chatId;
                String finalText = text;
                String finalUser = userName;
                String finalTrigger = triggerName;
                runOnUiThread(() -> {
                    String logMsg = "Update " + fieldName + " dari " + finalChatId;
                    if (!finalText.isEmpty()) logMsg += ": " + finalText;
                    addLog("[Trigger] " + logMsg);
                    processTriggerByType(finalTrigger, finalChatId, finalUser, finalText);
                });
                return;
            }
        }
    }

    private void processTriggerByType(String triggerMethod, String chatId, String userName, String text) {
        for (FlowNode node : workflow.getNodes()) {
            if (node.getType() == NodeType.TRIGGER) {
                String methodName = node.getProperty("_method");
                if (methodName == null) {
                    TelegramMethod m = findMethodByLabel(node.getLabel());
                    if (m != null) methodName = m.apiName;
                }
                if (methodName == null) continue;

                boolean match = false;
                switch (methodName) {
                    case "_on_message":
                        match = "_on_message".equals(triggerMethod) || "_on_edited_message".equals(triggerMethod);
                        if (match) {
                            String command = resolveTemplate(node.getProperty("command"), text, chatId, userName);
                            String filter = resolveTemplate(node.getProperty("filter"), text, chatId, userName);
                            if (command != null && !command.isEmpty()) match = text.startsWith(command);
                            if (match && filter != null && !filter.isEmpty()) match = text.contains(filter);
                        }
                        break;
                    case "_on_photo":
                        match = "_on_message".equals(triggerMethod) && currentMsgData.containsKey("photo");
                        break;
                    case "_on_video":
                        match = "_on_message".equals(triggerMethod) && currentMsgData.containsKey("video");
                        break;
                    case "_on_document":
                        match = "_on_message".equals(triggerMethod) && currentMsgData.containsKey("document");
                        break;
                    case "_on_audio":
                        match = "_on_message".equals(triggerMethod) && currentMsgData.containsKey("audio");
                        break;
                    case "_on_voice":
                        match = "_on_message".equals(triggerMethod) && currentMsgData.containsKey("voice");
                        break;
                    case "_on_animation":
                        match = "_on_message".equals(triggerMethod) && currentMsgData.containsKey("animation");
                        break;
                    case "_on_sticker":
                        match = "_on_message".equals(triggerMethod) && currentMsgData.containsKey("sticker");
                        break;
                    case "_on_location":
                        match = "_on_message".equals(triggerMethod) && currentMsgData.containsKey("location");
                        break;
                    case "_on_contact":
                        match = "_on_message".equals(triggerMethod) && currentMsgData.containsKey("contact");
                        break;
                    case "_on_poll":
                        match = "_on_message".equals(triggerMethod) && currentMsgData.containsKey("poll");
                        break;
                    case "_on_edited_message":
                        match = "_on_edited_message".equals(triggerMethod);
                        break;
                    case "_on_channel_post":
                        match = "_on_channel_post".equals(triggerMethod);
                        break;
                    case "_on_callback_query":
                        match = "_on_callback_query".equals(triggerMethod);
                        if (match) {
                            String dataFilter = node.getProperty("data");
                            if (dataFilter != null && !dataFilter.isEmpty()) {
                                String callbackData = currentMsgData.get("data");
                                match = callbackData != null && callbackData.contains(dataFilter);
                            }
                        }
                        break;
                    case "_on_inline_query":
                        match = "_on_inline_query".equals(triggerMethod);
                        if (match) {
                            String queryFilter = node.getProperty("query");
                            if (queryFilter != null && !queryFilter.isEmpty()) {
                                String queryText = currentMsgData.get("query");
                                match = queryText != null && queryText.contains(queryFilter);
                            }
                        }
                        break;
                    case "_on_chosen_inline_result":
                        match = "_on_chosen_inline_result".equals(triggerMethod);
                        break;
                    case "_on_chat_member":
                        match = "_on_chat_member".equals(triggerMethod);
                        if (match) {
                            String statusFilter = node.getProperty("status");
                            if (statusFilter != null && !statusFilter.isEmpty()) {
                                String newStatus = currentMsgData.get("new_chat_member.status");
                                match = newStatus != null && newStatus.equals(statusFilter);
                            }
                        }
                        break;
                    case "_on_my_chat_member":
                        match = "_on_my_chat_member".equals(triggerMethod);
                        break;
                    case "_on_chat_join_request":
                        match = "_on_chat_join_request".equals(triggerMethod);
                        break;
                    case "_on_poll_answer":
                        match = "_on_poll_answer".equals(triggerMethod);
                        break;
                    case "_on_pre_checkout_query":
                        match = "_on_pre_checkout_query".equals(triggerMethod);
                        break;
                    case "_on_shipping_query":
                        match = "_on_shipping_query".equals(triggerMethod);
                        break;
                    case "_on_listening":
                        match = "_on_listening".equals(triggerMethod);
                        break;
                    case "_on_schedule":
                    case "_on_interval":
                    case "_on_http_poll":
                    case "_on_webhook":
                    case "_on_manual":
                        match = methodName.equals(triggerMethod);
                        break;
                    default:
                        match = false;
                }

                if (match) {
                    addLog("Trigger '" + node.getLabel() + "' cocok (" + methodName + "), menjalankan flow");
                    processFlowFromNode(node, chatId, userName, text);
                }
            }
        }
    }

    private void startSttForTrigger(String prompt, int timeoutSec, FlowNode triggerNode) {
        if (checkSelfPermission(android.Manifest.permission.RECORD_AUDIO)
                != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            addLog("Izin mikrofon diperlukan untuk On Listening");
            runOnUiThread(() -> Snackbar.make(canvas, "Izin mikrofon diperlukan untuk On Listening",
                    Snackbar.LENGTH_LONG)
                    .setAction("IZIN", v -> requestPermissions(
                            new String[]{android.Manifest.permission.RECORD_AUDIO}, 1002))
                    .show());
            return;
        }
        final java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
        final String[] sttResult = {""};
        final android.speech.SpeechRecognizer[] recognizerRef = new android.speech.SpeechRecognizer[1];
        new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
            android.content.Intent intent = new android.content.Intent(
                    android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(android.speech.RecognizerIntent.EXTRA_PROMPT, prompt);
            intent.putExtra(android.speech.RecognizerIntent.EXTRA_MAX_RESULTS, 1);
            android.speech.SpeechRecognizer r = android.speech.SpeechRecognizer.createSpeechRecognizer(
                    com.tgflowbot.MainActivity.this);
            recognizerRef[0] = r;
            r.setRecognitionListener(new android.speech.RecognitionListener() {
                @Override public void onReadyForSpeech(android.os.Bundle p) {}
                @Override public void onBeginningOfSpeech() {}
                @Override public void onRmsChanged(float v) {}
                @Override public void onBufferReceived(byte[] b) {}
                @Override public void onEndOfSpeech() {}
                @Override public void onError(int err) {
                    addLog("On Listening error: " + err);
                    if (recognizerRef[0] != null) recognizerRef[0].destroy();
                    latch.countDown();
                }
                @Override public void onResults(android.os.Bundle r) {
                    java.util.ArrayList<String> m = r.getStringArrayList(
                            android.speech.SpeechRecognizer.RESULTS_RECOGNITION);
                    if (m != null && !m.isEmpty()) sttResult[0] = m.get(0);
                    if (recognizerRef[0] != null) recognizerRef[0].destroy();
                    latch.countDown();
                }
                @Override public void onPartialResults(android.os.Bundle p) {}
                @Override public void onEvent(int e, android.os.Bundle p) {}
            });
            r.startListening(intent);
        });
        new Thread(() -> {
            try { latch.await(timeoutSec, java.util.concurrent.TimeUnit.SECONDS); }
            catch (Exception ignored) {}
            if (sttResult[0].isEmpty()) {
                addLog("On Listening: tidak ada suara terdeteksi, workflow berhenti");
                runOnUiThread(() -> stopBackgroundPolling());
                return;
            }
            addLog("On Listening: " + sttResult[0]);
            final String fText = sttResult[0];
            Map<String, String> msgData = new HashMap<>();
            msgData.put("text", fText);
            msgData.put("chat.id", lastChatId != null ? lastChatId : "0");
            msgData.put("message_id", "0");
            msgData.put("date", String.valueOf(System.currentTimeMillis() / 1000));
            currentMsgData = msgData;
            runOnUiThread(() -> {
                Snackbar.make(canvas, "STT: " + fText, Snackbar.LENGTH_LONG).show();
                addLog("Trigger '" + triggerNode.getLabel() + "' cocok, menjalankan flow");
                String chatId2 = lastChatId != null && !lastChatId.isEmpty() ? lastChatId : "0";
                processFlowFromNode(triggerNode, chatId2, "", fText);
                if (isRunning) {
                    addLog("On Listening: mendengarkan lagi...");
                    startSttForTrigger(prompt, timeoutSec, triggerNode);
                }
            });
        }).start();
    }

    private void startScheduleTrigger(FlowNode triggerNode) {
        String cronExpr = triggerNode.getProperty("cron");
        String timezone = triggerNode.getProperty("timezone");
        if (cronExpr == null || cronExpr.isEmpty()) cronExpr = "0 * * * * *";
        if (timezone == null || timezone.isEmpty()) timezone = "UTC";
        addLog("Schedule Trigger: " + cronExpr + " (" + timezone + ")");

        final String finalCronExpr = cronExpr;
        final String finalTimezone = timezone;
        new Thread(() -> {
            String[] cronParts = finalCronExpr.split("\\s+");
            String[] parts = cronParts.length < 5 ? new String[]{"*", "*", "*", "*", "*"} : cronParts;
            java.time.ZoneId zoneId = java.time.ZoneId.of(finalTimezone);

            while (isRunning) {
                java.time.ZonedDateTime now = java.time.ZonedDateTime.now(zoneId);
                java.time.ZonedDateTime next = findNextCronMatch(now, parts, zoneId);
                final long finalMillis = java.time.Duration.between(now, next).toMillis();
                if (finalMillis > 0) try { Thread.sleep(finalMillis); } catch (InterruptedException ignored) {}
                if (!isRunning) break;

                final String fText = "scheduled";
                final String fChatId = "0";
                final String fMessageId = "0";
                final String fDate = String.valueOf(System.currentTimeMillis() / 1000);

                runOnUiThread(() -> {
                    addLog("Schedule Trigger: waktu tercapai");
                    Map<String, String> msgData = new HashMap<>();
                    msgData.put("text", fText);
                    msgData.put("chat.id", fChatId);
                    msgData.put("message_id", fMessageId);
                    msgData.put("date", fDate);
                    msgData.put("trigger.type", "schedule");
                    msgData.put("trigger.cron", finalCronExpr);
                    currentMsgData = msgData;
                    processFlowFromNode(triggerNode, fChatId, "system", fText);
                });
            }
        }).start();
    }

    private java.time.ZonedDateTime findNextCronMatch(java.time.ZonedDateTime base, String[] parts, java.time.ZoneId zoneId) {
        // Simple cron matcher: * = any, number = exact, */n = every n
        java.time.ZonedDateTime candidate = base.plusSeconds(1).withNano(0);
        outer: while (true) {
            if (!matchCronField(candidate.getSecond(), parts[0])) { candidate = candidate.plusSeconds(1); continue; }
            if (!matchCronField(candidate.getMinute(), parts.length > 1 ? parts[1] : "*")) { candidate = candidate.plusMinutes(1).withSecond(0); continue; }
            if (!matchCronField(candidate.getHour(), parts.length > 2 ? parts[2] : "*")) { candidate = candidate.plusHours(1).withMinute(0).withSecond(0); continue; }
            if (!matchCronField(candidate.getDayOfMonth(), parts.length > 3 ? parts[3] : "*")) { candidate = candidate.plusDays(1).withHour(0).withMinute(0).withSecond(0); continue; }
            if (!matchCronField(candidate.getMonthValue(), parts.length > 4 ? parts[4] : "*")) { candidate = candidate.plusMonths(1).withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0); continue; }
            if (parts.length > 5 && !matchCronField(candidate.getDayOfWeek().getValue() % 7, parts[5])) { candidate = candidate.plusDays(1).withHour(0).withMinute(0).withSecond(0); continue; }
            return candidate;
        }
    }

    private boolean matchCronField(int value, String expr) {
        if (expr == null || expr.equals("*")) return true;
        if (expr.startsWith("*/")) {
            try { int step = Integer.parseInt(expr.substring(2)); return step > 0 && value % step == 0; } catch (Exception ignored) {}
        }
        if (expr.contains("-")) {
            String[] r = expr.split("-");
            try { int a = Integer.parseInt(r[0]); int b = Integer.parseInt(r[1]); return value >= a && value <= b; } catch (Exception ignored) {}
        }
        if (expr.contains(",")) {
            for (String v : expr.split(",")) { try { if (Integer.parseInt(v) == value) return true; } catch (Exception ignored) {} }
        }
        try { return Integer.parseInt(expr) == value; } catch (Exception ignored) {}
        return false;
    }

    private void startIntervalTrigger(FlowNode triggerNode) {
        String intervalStr = triggerNode.getProperty("interval_sec");
        int intervalSec = 60;
        try { intervalSec = Integer.parseInt(intervalStr); } catch (Exception ignored) {}
        if (intervalSec < 1) intervalSec = 1;
        addLog("Interval Trigger: setiap " + intervalSec + " detik");
        final int finalIntervalSec = intervalSec;
        new Thread(() -> {
            while (isRunning) {
                try { Thread.sleep(finalIntervalSec * 1000L); } catch (Exception ignored) {}
                if (!isRunning) break;
                runOnUiThread(() -> {
                    addLog("Interval Trigger: waktu tercapai");
                    Map<String, String> msgData = new HashMap<>();
                    msgData.put("text", "interval");
                    msgData.put("chat.id", "0");
                    msgData.put("message_id", "0");
                    msgData.put("date", String.valueOf(System.currentTimeMillis() / 1000));
                    msgData.put("trigger.type", "interval");
                    msgData.put("trigger.interval_sec", String.valueOf(finalIntervalSec));
                    currentMsgData = msgData;
                    processFlowFromNode(triggerNode, "0", "system", "interval");
                });
            }
        }).start();
    }

    private void startHttpPollTrigger(FlowNode triggerNode) {
        String url = triggerNode.getProperty("url");
        String method = triggerNode.getProperty("http_method");
        String headersJson = triggerNode.getProperty("headers");
        String body = triggerNode.getProperty("body");
        String intervalStr = triggerNode.getProperty("interval_sec");
        String jsonPath = triggerNode.getProperty("json_path");
        if (url == null || url.isEmpty()) {
            addLog("HTTP Poll: URL kosong");
            return;
        }
        if (method == null || method.isEmpty()) method = "GET";
        int intervalSec = 60;
        try { intervalSec = Integer.parseInt(intervalStr); } catch (Exception ignored) {}
        if (intervalSec < 5) intervalSec = 5;
        addLog("HTTP Poll Trigger: " + method + " " + url + " (interval " + intervalSec + "s)");

        final okhttp3.OkHttpClient client = new okhttp3.OkHttpClient();
        final String finalUrl = url;
        final String finalMethod = method;
        final String finalBody = body;
        final String finalHeadersJson = headersJson;
        final String finalJsonPath = jsonPath;
        final int finalIntervalSec = intervalSec;

        new Thread(() -> {
            while (isRunning) {
                try {
                    okhttp3.Request.Builder reqBuilder = new okhttp3.Request.Builder().url(finalUrl);
                    if ("POST".equalsIgnoreCase(finalMethod) || "PUT".equalsIgnoreCase(finalMethod) || "PATCH".equalsIgnoreCase(finalMethod)) {
                        okhttp3.MediaType mediaType = okhttp3.MediaType.parse("application/json");
                        okhttp3.RequestBody reqBody = okhttp3.RequestBody.create(finalBody != null ? finalBody : "{}", mediaType);
                        reqBuilder.method(finalMethod, reqBody);
                    } else {
                        reqBuilder.method(finalMethod, null);
                    }
                    if (finalHeadersJson != null && !finalHeadersJson.isEmpty()) {
                        com.google.gson.JsonObject headersObj = new com.google.gson.JsonParser().parse(finalHeadersJson).getAsJsonObject();
                        for (java.util.Map.Entry<String, com.google.gson.JsonElement> e : headersObj.entrySet()) {
                            reqBuilder.addHeader(e.getKey(), e.getValue().getAsString());
                        }
                    }
                    okhttp3.Request request = reqBuilder.build();
                    okhttp3.Response response = client.newCall(request).execute();
                    String respBody = response.body() != null ? response.body().string() : "";
                    response.close();
                    String triggerValue = respBody;
                    if (finalJsonPath != null && !finalJsonPath.isEmpty()) {
                        try {
                            com.google.gson.JsonElement elem = new com.google.gson.JsonParser().parse(respBody);
                            final String[] parts = finalJsonPath.split("\\.");
                            for (String part : parts) {
                                if (elem.isJsonObject() && elem.getAsJsonObject().has(part)) {
                                    elem = elem.getAsJsonObject().get(part);
                                } else if (elem.isJsonArray()) {
                                    int idx = Integer.parseInt(part);
                                    elem = elem.getAsJsonArray().get(idx);
                                } else {
                                    elem = null;
                                    break;
                                }
                            }
                            if (elem != null) triggerValue = elem.isJsonPrimitive() ? elem.getAsString() : elem.toString();
                        } catch (Exception ignored) {}
                    }
                    final String fValue = triggerValue;
                    runOnUiThread(() -> {
                        addLog("HTTP Poll: response diterima");
                        Map<String, String> msgData = new HashMap<>();
                        msgData.put("text", fValue);
                        msgData.put("chat.id", "0");
                        msgData.put("message_id", "0");
                        msgData.put("date", String.valueOf(System.currentTimeMillis() / 1000));
                        msgData.put("trigger.type", "http_poll");
                        msgData.put("trigger.url", finalUrl);
                        msgData.put("trigger.response", fValue.length() > 500 ? fValue.substring(0, 500) + "..." : fValue);
                        currentMsgData = msgData;
                        processFlowFromNode(triggerNode, "0", "system", fValue);
                    });
                } catch (Exception e) {
                    addLog("HTTP Poll error: " + e.getMessage());
                }
                try { Thread.sleep(finalIntervalSec * 1000L); } catch (Exception ignored) {}
            }
        }).start();
    }

    private void startWebhookTrigger(FlowNode triggerNode) {
        String path = triggerNode.getProperty("path");
        String secret = triggerNode.getProperty("secret_token");
        if (path == null || path.isEmpty()) path = "/webhook";
        addLog("Webhook Trigger: listening on " + path);
        Snackbar.make(canvas, "Webhook aktif di " + path + " (gunakan port 8080)", Snackbar.LENGTH_LONG).show();
    }

    private void startManualTrigger(FlowNode triggerNode) {
        String inputData = triggerNode.getProperty("input_data");
        if (inputData == null) inputData = "{}";
        addLog("Manual Trigger: dijalankan");
        Map<String, String> msgData = new HashMap<>();
        msgData.put("text", "manual");
        msgData.put("chat.id", "0");
        msgData.put("message_id", "0");
        msgData.put("date", String.valueOf(System.currentTimeMillis() / 1000));
        msgData.put("trigger.type", "manual");
        msgData.put("trigger.input", inputData);
        currentMsgData = msgData;
        processFlowFromNode(triggerNode, "0", "system", inputData);
    }

    private void processFlowFromNode(FlowNode triggerNode, String chatId, String userName, String text) {
        runOnUiThread(() -> {
            canvas.clearFlowPath();
            canvas.triggerPulse(triggerNode.getId());
        });
        for (Connection conn : workflow.getConnections()) {
            if (conn.getSourceNodeId().equals(triggerNode.getId())) {
                FlowNode next = workflow.findNodeById(conn.getTargetNodeId());
                if (next != null) {
                    executeNode(next, chatId, userName, text);
                }
            }
        }
    }

    private void executeNode(FlowNode node, String chatId, String userName, String text) {
        addLog("Eksekusi node: " + node.getLabel());

        String token = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .getString("bot_token", "");
        String methodName = node.getProperty("_method");

        TelegramMethod method = findMethodDef(methodName);
        if (method == null && methodName == null) {
            method = findMethodByLabel(node.getLabel());
        }

        if (method == null) {
            final String unknown = methodName != null ? methodName : node.getLabel();
            runOnUiThread(() -> Snackbar.make(canvas,
                    "Method tidak dikenal: " + unknown, Snackbar.LENGTH_SHORT).show());
            return;
        }

        if (methodName == null) {
            node.putProperty("_method", method.apiName);
        }

        switch (node.getType()) {
            case CONDITION: {
                boolean conditionMet = execCondition(method, node, text);
                currentMsgData.put("result", conditionMet ? "true" : "false");
                runOnUiThread(() ->
                    canvas.triggerConditionPulse(node.getId(), conditionMet));
                for (Connection conn : workflow.getConnections()) {
                    if (conn.getSourceNodeId().equals(node.getId())
                            && conn.getConditionResult() == conditionMet) {
                        FlowNode next = workflow.findNodeById(conn.getTargetNodeId());
                        if (next != null) executeNode(next, chatId, userName, text);
                    }
                }
                break;
            }
            case OUTPUT:
            case ACTION: {
                if ("log".equals(methodName)) {
                    String msg = node.getProperty("message");
                    String raw = msg != null && !msg.trim().isEmpty() ? msg : text;
                    final String logged = resolveTemplate(raw, text, chatId, userName);
                    addLog(logged);
                    currentMsgData.put("result", logged);
                    runOnUiThread(() -> {
                        Snackbar.make(canvas, "Log: " + logged, Snackbar.LENGTH_SHORT).show();
                        continueFlow(node, chatId, userName, logged);
                    });
                } else if ("ai_chat".equals(methodName)) {
                    execAiChat(node, chatId, userName, text);
                } else if ("_return".equals(methodName)) {
                    addLog("Return: flow berhenti");
                    currentMsgData.put("result", "");
                } else if ("reply".equals(methodName)) {
                    String replyText = resolveTemplate(node.getProperty("text"), text, chatId, userName);
                    String parseMode = node.getProperty("parse_mode");
                    Map<String, String> replyParams = new HashMap<>();
                    replyParams.put("chat_id", chatId);
                    replyParams.put("text", replyText != null ? replyText : "");
                    String origMsgId = currentMsgData.get("message_id");
                    if (origMsgId != null && !origMsgId.isEmpty()) {
                        replyParams.put("reply_to_message_id", origMsgId);
                    }
                    if (parseMode != null && !parseMode.trim().isEmpty()) {
                        replyParams.put("parse_mode", parseMode);
                    }
                    TelegramMethod sendMessageMethod = findMethodDef("sendMessage");
                    if (sendMessageMethod == null) {
                        sendMessageMethod = new TelegramMethod("sendMessage", "Send Message", "Kirim pesan teks", NodeType.ACTION);
                    }
                    execApiCall(token, sendMessageMethod, replyParams, node, chatId, userName, text);
                } else if ("_output_delete".equals(methodName)) {
                    String origMsgId = currentMsgData.get("message_id");
                    if (origMsgId != null && !origMsgId.isEmpty()) {
                        Map<String, String> delParams = new HashMap<>();
                        delParams.put("chat_id", chatId);
                        delParams.put("message_id", origMsgId);
                        TelegramMethod delMethod = findMethodDef("deleteMessage");
                        if (delMethod == null) {
                            delMethod = new TelegramMethod("deleteMessage", "Delete Message", "Hapus pesan", NodeType.ACTION);
                        }
                        execApiCall(token, delMethod, delParams, node, chatId, userName, text);
                    } else {
                        runOnUiThread(() -> Snackbar.make(canvas, "Tidak ada pesan untuk dihapus", Snackbar.LENGTH_SHORT).show());
                    }
                } else if ("_output_pin".equals(methodName)) {
                    String origMsgId = currentMsgData.get("message_id");
                    if (origMsgId != null && !origMsgId.isEmpty()) {
                        Map<String, String> pinParams = new HashMap<>();
                        pinParams.put("chat_id", chatId);
                        pinParams.put("message_id", origMsgId);
                        TelegramMethod pinMethod = findMethodDef("pinChatMessage");
                        if (pinMethod == null) {
                            pinMethod = new TelegramMethod("pinChatMessage", "Pin Message", "Sematkan pesan", NodeType.ACTION);
                        }
                        execApiCall(token, pinMethod, pinParams, node, chatId, userName, text);
                    } else {
                        runOnUiThread(() -> Snackbar.make(canvas, "Tidak ada pesan untuk disematkan", Snackbar.LENGTH_SHORT).show());
                    }
                } else if ("_output_kick".equals(methodName)) {
                    String userId = currentMsgData.get("from.id");
                    String untilDate = node.getProperty("until_date");
                    if (userId != null && !userId.isEmpty()) {
                        Map<String, String> banParams = new HashMap<>();
                        banParams.put("chat_id", chatId);
                        banParams.put("user_id", userId);
                        if (untilDate != null && !untilDate.isEmpty()) {
                            banParams.put("until_date", untilDate);
                        }
                        TelegramMethod banMethod = findMethodDef("banChatMember");
                        if (banMethod == null) {
                            banMethod = new TelegramMethod("banChatMember", "Ban User", "Blokir anggota", NodeType.ACTION);
                        }
                        execApiCall(token, banMethod, banParams, node, chatId, userName, text);
                    } else {
                        runOnUiThread(() -> Snackbar.make(canvas, "Tidak ada user untuk ditendang", Snackbar.LENGTH_SHORT).show());
                    }
                } else if ("forward".equals(methodName)) {
                    String targetChatId = resolveTemplate(node.getProperty("target_chat_id"), text, chatId, userName);
                    String origMsgId = currentMsgData.get("message_id");
                    if (targetChatId != null && !targetChatId.trim().isEmpty() && origMsgId != null) {
                        Map<String, String> fwdParams = new HashMap<>();
                        fwdParams.put("chat_id", targetChatId);
                        fwdParams.put("from_chat_id", chatId);
                        fwdParams.put("message_id", origMsgId);
                        TelegramMethod forwardMethodDef = findMethodDef("forwardMessage");
                        if (forwardMethodDef == null) {
                            forwardMethodDef = new TelegramMethod("forwardMessage", "Forward Message", "Forward pesan", NodeType.ACTION);
                        }
                        execApiCall(token, forwardMethodDef, fwdParams, node, chatId, userName, text);
                    } else {
                        runOnUiThread(() -> Snackbar.make(canvas,
                                "Forward gagal: target_chat_id kosong atau tidak ada pesan masuk", Snackbar.LENGTH_SHORT).show());
                    }
                } else if (methodName != null && methodName.startsWith("_phone_")) {
                    execPhoneAction(node, method, chatId, userName, text);
                } else if (methodName != null && methodName.startsWith("_")) {
                    execLocalAction(node, method, chatId, userName, text);
                } else {
                    Map<String, String> params = new HashMap<>();
                    for (ParamDef p : method.params) {
                        String val = node.getProperty(p.name);
                        if (val == null || val.trim().isEmpty()) {
                            if (p.defaultValue != null) val = p.defaultValue;
                        }
                        val = resolveTemplate(val, text, chatId, userName);
                        if (val != null && !val.trim().isEmpty()) {
                            params.put(p.name, val);
                        }
                    }
                    if (!params.containsKey("chat_id") || params.get("chat_id").trim().isEmpty()) {
                        params.put("chat_id", chatId);
                    }
                    execApiCall(token, method, params, node, chatId, userName, text);
                }
                break;
            }
        }
    }

    private void execAiChat(FlowNode node, String chatId, String userName, String text) {
        String promptTemplate = node.getProperty("prompt_template");
        if (promptTemplate == null || promptTemplate.trim().isEmpty()) {
            promptTemplate = "{{text}}";
        }
        final String providerId;
        String rawProviderId = node.getProperty("provider");
        if (rawProviderId == null || rawProviderId.trim().isEmpty()) {
            providerId = "openai";
        } else {
            providerId = rawProviderId;
        }
        String model = node.getProperty("model");
        String systemPrompt = node.getProperty("system_prompt");
        String customEndpoint = node.getProperty("custom_endpoint");
        String tempStr = node.getProperty("temperature");
        final float temperature = tempStr != null ? Float.parseFloat(tempStr) : 0.7f;
        String maxTokensStr = node.getProperty("max_tokens");
        final int maxTokens = maxTokensStr != null ? Integer.parseInt(maxTokensStr) : 1024;

        final String prompt = resolveTemplate(promptTemplate, text, chatId, userName);

        AiProvider[] providers = AiProvider.getBuiltInProviders();
        AiProvider foundProvider = null;
        for (AiProvider p : providers) {
            if (p.id.equals(providerId)) {
                foundProvider = p;
                break;
            }
        }
        if (foundProvider == null) {
            runOnUiThread(() -> Snackbar.make(canvas,
                    "Provider tidak dikenal: " + providerId, Snackbar.LENGTH_SHORT).show());
            return;
        }

        final AiProvider provider = foundProvider;

        String apiKeyRaw = "";
        if (provider.needsApiKey) {
            apiKeyRaw = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                    .getString("ai_key_" + providerId, "");
            if (apiKeyRaw.isEmpty()) {
                runOnUiThread(() -> Snackbar.make(canvas,
                        "API key untuk " + provider.name + " belum dissetting",
                        Snackbar.LENGTH_LONG)
                        .setAction("SETTING", v -> showAiSettings())
                        .show());
                return;
            }
        }
        final String apiKey = apiKeyRaw;

        runOnUiThread(() -> Snackbar.make(canvas,
                "AI Chat: " + provider.name + " ...", Snackbar.LENGTH_SHORT).show());

        final String cId = chatId;
        final String uName = userName;

        String useToolsRaw = node.getProperty("use_phone_tools");
        boolean useTools = "true".equalsIgnoreCase(useToolsRaw);

        if (!useTools) {
            AiChatHelper.chat(provider, apiKey, prompt, model, systemPrompt,
                    temperature, maxTokens, customEndpoint,
                    new AiChatHelper.AiCallback() {
                @Override
                public void onSuccess(String responseText) {
                    addLog("AI: " + responseText);
                    currentMsgData.put("result", responseText);
                    runOnUiThread(() -> {
                        Snackbar.make(canvas, "AI: " + responseText,
                                Snackbar.LENGTH_LONG).show();
                        canvas.triggerPulse(node.getId());
                        continueFlow(node, cId, uName, responseText);
                    });
                }

                @Override
                public void onError(String error) {
                    addLog("AI Error: " + error);
                    currentMsgData.put("error", error);
                    runOnUiThread(() -> {
                        Snackbar.make(canvas,
                                "AI Error: " + error, Snackbar.LENGTH_LONG).show();
                        canvas.triggerPulse(node.getId());
                        continueFlow(node, cId, uName, "");
                    });
                }
            });
            return;
        }

        List<AiChatHelper.ToolDefinition> toolDefs = buildAiToolDefs();
        List<String> history = new ArrayList<>();

        AiChatHelper.chatWithTools(provider, apiKey, prompt, model, systemPrompt,
                temperature, maxTokens, customEndpoint, toolDefs, history,
                new AiChatHelper.AiToolCallback() {
            @Override
            public void onToolCalls(List<AiChatHelper.ToolCall> calls, Runnable retry) {
                for (AiChatHelper.ToolCall call : calls) {
                    JsonObject asstMsg = new JsonObject();
                    asstMsg.addProperty("role", "assistant");
                    asstMsg.addProperty("content", (String) null);
                    JsonArray tcs = new JsonArray();
                    JsonObject tc = new JsonObject();
                    tc.addProperty("id", call.id);
                    tc.addProperty("type", "function");
                    JsonObject func = new JsonObject();
                    func.addProperty("name", call.name);
                    func.addProperty("arguments", call.arguments);
                    tc.add("function", func);
                    tcs.add(tc);
                    asstMsg.add("tool_calls", tcs);
                    history.add(asstMsg.toString());

                    String result = executeTool(call.name, call.arguments, cId, uName, text);

                    JsonObject toolMsg = new JsonObject();
                    toolMsg.addProperty("role", "tool");
                    toolMsg.addProperty("tool_call_id", call.id);
                    toolMsg.addProperty("content", result != null ? result : "ok");
                    history.add(toolMsg.toString());

                    break;
                }

                AiChatHelper.continueWithToolResults(provider, apiKey, model,
                        systemPrompt, temperature, maxTokens, customEndpoint,
                        toolDefs, history, this);
            }

            @Override
            public void onSuccess(String responseText) {
                addLog("AI: " + responseText);
                currentMsgData.put("result", responseText);
                runOnUiThread(() -> {
                    Snackbar.make(canvas, "AI: " + responseText,
                            Snackbar.LENGTH_LONG).show();
                    canvas.triggerPulse(node.getId());
                    continueFlow(node, cId, uName, responseText);
                });
            }

            @Override
            public void onError(String error) {
                addLog("AI Error: " + error);
                currentMsgData.put("error", error);
                runOnUiThread(() -> {
                    Snackbar.make(canvas,
                            "AI Error: " + error, Snackbar.LENGTH_LONG).show();
                    canvas.triggerPulse(node.getId());
                    continueFlow(node, cId, uName, "");
                });
            }
        });
    }

    private List<AiChatHelper.ToolDefinition> buildAiToolDefs() {
        List<AiChatHelper.ToolDefinition> defs = new ArrayList<>();
        for (TelegramMethod m : MethodRegistry.getAllMethods()) {
            if (m.apiName != null && m.apiName.startsWith("_phone_")) {
                JsonObject params = new JsonObject();
                params.addProperty("type", "object");
                JsonObject props = new JsonObject();
                JsonArray required = new JsonArray();
                for (ParamDef p : m.params) {
                    JsonObject prop = new JsonObject();
                    String jsonType = "string";
                    if (p.type == ParamDef.ParamType.INTEGER) jsonType = "integer";
                    else if (p.type == ParamDef.ParamType.FLOAT) jsonType = "number";
                    else if (p.type == ParamDef.ParamType.BOOLEAN) jsonType = "boolean";
                    prop.addProperty("type", jsonType);
                    if (p.hint != null) prop.addProperty("description", p.hint);
                    props.add(p.name, prop);
                    if (p.required) required.add(p.name);
                }
                params.add("properties", props);
                params.add("required", required);
                defs.add(new AiChatHelper.ToolDefinition(m.apiName, m.description, params));
            }
        }
        return defs;
    }

    private String executeTool(String toolName, String argsJson, String chatId, String userName, String text) {
        try {
            JsonObject args = JsonParser.parseString(argsJson).getAsJsonObject();

            switch (toolName) {
                case "_phone_flashlight": {
                    String state = args.has("state") ? args.get("state").getAsString() : "on";
                    boolean on = "on".equalsIgnoreCase(state);
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                        if (checkSelfPermission(android.Manifest.permission.CAMERA)
                                != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                            return "Izin kamera belum diberikan";
                        }
                        android.hardware.camera2.CameraManager cm =
                                (android.hardware.camera2.CameraManager) getSystemService(CAMERA_SERVICE);
                        String cameraId = null;
                        for (String id : cm.getCameraIdList()) {
                            android.hardware.camera2.CameraCharacteristics chars =
                                    cm.getCameraCharacteristics(id);
                            Boolean flashAvail = chars.get(
                                    android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE);
                            if (flashAvail != null && flashAvail) { cameraId = id; break; }
                        }
                        if (cameraId != null) {
                            cm.setTorchMode(cameraId, on);
                            return on ? "Senter menyala" : "Senter mati";
                        } else {
                            return "Tidak ada flash";
                        }
                    }
                    return "API terlalu rendah";
                }
                case "_phone_vibrate": {
                    long dur = args.has("duration") ? args.get("duration").getAsLong() : 500;
                    android.os.Vibrator vibrator = (android.os.Vibrator) getSystemService(VIBRATOR_SERVICE);
                    if (vibrator != null && vibrator.hasVibrator()) {
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                            vibrator.vibrate(android.os.VibrationEffect.createOneShot(dur,
                                    android.os.VibrationEffect.DEFAULT_AMPLITUDE));
                        } else {
                            vibrator.vibrate(dur);
                        }
                        return "Bergetar " + dur + "ms";
                    }
                    return "Tidak ada vibrator";
                }
                case "_phone_toast": {
                    String msg = args.has("message") ? args.get("message").getAsString() : "";
                    android.widget.Toast.makeText(this, msg, android.widget.Toast.LENGTH_SHORT).show();
                    return "Toast: " + msg;
                }
                case "_phone_battery": {
                    android.content.IntentFilter ifilter = new android.content.IntentFilter(
                            android.content.Intent.ACTION_BATTERY_CHANGED);
                    android.content.Intent batteryStatus = registerReceiver(null, ifilter);
                    if (batteryStatus != null) {
                        int level = batteryStatus.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1);
                        int scale = batteryStatus.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, -1);
                        int pct = (int) (level * 100.0 / scale);
                        int status = batteryStatus.getIntExtra(android.os.BatteryManager.EXTRA_STATUS, -1);
                        String s = status == android.os.BatteryManager.BATTERY_STATUS_CHARGING ? "charging" : "not charging";
                        String plugged = batteryStatus.getIntExtra(android.os.BatteryManager.EXTRA_PLUGGED, 0) > 0 ? "plugged in" : "unplugged";
                        return "Battery: " + pct + "%, " + s + ", " + plugged;
                    }
                    return "Battery info unavailable";
                }
                case "_phone_device_info": {
                    return "Model: " + android.os.Build.MODEL
                            + ", Android: " + android.os.Build.VERSION.RELEASE
                            + ", API: " + android.os.Build.VERSION.SDK_INT;
                }
                case "_phone_open_url": {
                    String url = args.has("url") ? args.get("url").getAsString() : "";
                    if (!url.startsWith("http://") && !url.startsWith("https://")) {
                        url = "https://" + url;
                    }
                    android.content.Intent intent = new android.content.Intent(
                            android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url));
                    intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    return "Membuka URL: " + url;
                }
                case "_phone_clipboard_set": {
                    String clipText = args.has("text") ? args.get("text").getAsString() : "";
                    android.content.ClipboardManager clipboard =
                            (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                    clipboard.setPrimaryClip(android.content.ClipData.newPlainText(
                            "TgFlowBot", clipText));
                    return "Clipboard: " + clipText;
                }
                case "_phone_volume": {
                    int level = args.has("level") ? args.get("level").getAsInt() : 50;
                    android.media.AudioManager audio =
                            (android.media.AudioManager) getSystemService(AUDIO_SERVICE);
                    int maxVol = audio.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC);
                    audio.setStreamVolume(android.media.AudioManager.STREAM_MUSIC,
                            level * maxVol / 100, 0);
                    return "Volume: " + level + "%";
                }
                case "_phone_brightness": {
                    int brightness = args.has("level") ? args.get("level").getAsInt() : 128;
                    if (brightness < 0) brightness = 0;
                    if (brightness > 255) brightness = 255;
                    if (android.provider.Settings.System.canWrite(this)) {
                        android.provider.Settings.System.putInt(getContentResolver(),
                                android.provider.Settings.System.SCREEN_BRIGHTNESS, brightness);
                        return "Brightness: " + brightness;
                    }
                    return "Izin write_settings belum diberikan";
                }
                default:
                    return "Unknown tool: " + toolName;
            }
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    private void continueFlow(FlowNode fromNode, String chatId, String userName, String text) {
        for (Connection conn : workflow.getConnections()) {
            if (conn.getSourceNodeId().equals(fromNode.getId())) {
                FlowNode next = workflow.findNodeById(conn.getTargetNodeId());
                if (next != null) {
                    executeNode(next, chatId, userName, text);
                }
            }
        }
    }

    private boolean execCondition(TelegramMethod method, FlowNode node, String text) {
        String methodName = method.apiName;
        String value = node.getProperty("value");
        String pattern = node.getProperty("pattern");
        String type = node.getProperty("type");
        String mediaType = node.getProperty("media_type");
        String cs = node.getProperty("case_sensitive");
        boolean caseSensitive = "true".equals(cs);

        switch (methodName) {
            case "contains": {
                if (value == null || value.isEmpty()) return true;
                return caseSensitive ? text.contains(value) :
                        text.toLowerCase().contains(value.toLowerCase());
            }
            case "equals": {
                if (value == null) return false;
                return caseSensitive ? text.equals(value) :
                        text.equalsIgnoreCase(value);
            }
            case "startsWith": {
                if (value == null || value.isEmpty()) return true;
                return caseSensitive ? text.startsWith(value) :
                        text.toLowerCase().startsWith(value.toLowerCase());
            }
            case "matches": {
                if (pattern == null || pattern.isEmpty()) return true;
                try {
                    int flags = caseSensitive ? 0 : java.util.regex.Pattern.CASE_INSENSITIVE;
                    return java.util.regex.Pattern.compile(pattern, flags).matcher(text).find();
                } catch (Exception e) {
                    return false;
                }
            }
            case "chatType": {
                return type != null && type.equals(node.getProperty("_chat_type"));
            }
            case "hasMedia": {
                if (mediaType == null || mediaType.isEmpty()) {
                    return currentMsgData.containsKey("photo") || currentMsgData.containsKey("video")
                            || currentMsgData.containsKey("document") || currentMsgData.containsKey("audio")
                            || currentMsgData.containsKey("voice") || currentMsgData.containsKey("animation")
                            || currentMsgData.containsKey("sticker") || currentMsgData.containsKey("location")
                            || currentMsgData.containsKey("contact") || currentMsgData.containsKey("poll");
                }
                return currentMsgData.containsKey(mediaType);
            }
            case "hasPhoto": return currentMsgData.containsKey("photo");
            case "hasVideo": return currentMsgData.containsKey("video");
            case "hasDocument": return currentMsgData.containsKey("document");
            case "hasAudio": return currentMsgData.containsKey("audio");
            case "hasVoice": return currentMsgData.containsKey("voice");
            case "hasAnimation": return currentMsgData.containsKey("animation");
            case "hasSticker": return currentMsgData.containsKey("sticker");
            case "hasLocation": return currentMsgData.containsKey("location");
            case "hasContact": return currentMsgData.containsKey("contact");
            case "hasPoll": return currentMsgData.containsKey("poll");
            case "hasDice": return currentMsgData.containsKey("dice");
            case "isForwarded": return currentMsgData.containsKey("forward_from") || currentMsgData.containsKey("forward_origin");
            case "isReply": return currentMsgData.containsKey("reply_to_message");
            case "isBot": {
                String isBot = currentMsgData.get("from.is_bot");
                return "true".equals(isBot);
            }
            case "isCommand": return text != null && text.startsWith("/");
            case "isAdmin": {
                return currentMsgData.containsKey("from.is_admin") && "true".equals(currentMsgData.get("from.is_admin"));
            }
            case "alwaysTrue": {
                return true;
            }
            case "_compare": {
                String aRaw = resolveTemplate(node.getProperty("a"), text, null, null);
                String bRaw = resolveTemplate(node.getProperty("b"), text, null, null);
                String op = node.getProperty("operator");
                if (op == null) op = "==";
                double a = 0, b = 0;
                try { a = Double.parseDouble(aRaw != null ? aRaw : "0"); } catch (Exception ignored) {}
                try { b = Double.parseDouble(bRaw != null ? bRaw : "0"); } catch (Exception ignored) {}
                switch (op) {
                    case "!=": return a != b;
                    case ">":  return a > b;
                    case "<":  return a < b;
                    case ">=": return a >= b;
                    case "<=": return a <= b;
                    default:   return a == b;
                }
            }
            case "endsWith": {
                if (value == null || value.isEmpty()) return true;
                return caseSensitive ? text.endsWith(value) :
                        text.toLowerCase().endsWith(value.toLowerCase());
            }
            case "isEmpty": {
                return text == null || text.trim().isEmpty();
            }
            case "isNumeric": {
                if (text == null || text.isEmpty()) return false;
                try { Double.parseDouble(text.trim()); return true; }
                catch (NumberFormatException e) { return false; }
            }
            case "length": {
                String op = node.getProperty("operator");
                if (op == null) op = "==";
                String lenStr = node.getProperty("value");
                int targetLen = 0;
                try { targetLen = Integer.parseInt(lenStr); } catch (Exception ignored) {}
                int actualLen = text != null ? text.length() : 0;
                switch (op) {
                    case "!=": return actualLen != targetLen;
                    case ">":  return actualLen > targetLen;
                    case "<":  return actualLen < targetLen;
                    case ">=": return actualLen >= targetLen;
                    case "<=": return actualLen <= targetLen;
                    default:   return actualLen == targetLen;
                }
            }
            case "isBetween": {
                String valRaw = resolveTemplate(node.getProperty("value"), text, null, null);
                String minRaw = resolveTemplate(node.getProperty("min"), text, null, null);
                String maxRaw = resolveTemplate(node.getProperty("max"), text, null, null);
                double val = 0, min = 0, max = 0;
                try { val = Double.parseDouble(valRaw != null ? valRaw : "0"); } catch (Exception ignored) {}
                try { min = Double.parseDouble(minRaw != null ? minRaw : "0"); } catch (Exception ignored) {}
                try { max = Double.parseDouble(maxRaw != null ? maxRaw : "0"); } catch (Exception ignored) {}
                return val >= min && val <= max;
            }
            case "_in": {
                String listStr = node.getProperty("list");
                if (listStr == null || listStr.isEmpty()) return false;
                String[] items = listStr.split(",");
                for (String item : items) {
                    String trimmed = item.trim();
                    if (caseSensitive ? value != null && value.equals(trimmed) :
                            value != null && value.equalsIgnoreCase(trimmed)) {
                        return true;
                    }
                }
                return false;
            }
            case "notEmpty": {
                String prop = resolveTemplate(node.getProperty("value"), text, null, null);
                return prop != null && !prop.trim().isEmpty();
            }
            case "hasKey": {
                String key = node.getProperty("key");
                return key != null && currentMsgData.containsKey(key);
            }
            case "alwaysFalse": {
                return false;
            }
            default:
                return true;
        }
    }

    private void execApiCall(String token, TelegramMethod method, Map<String, String> params,
                             FlowNode node, String chatId, String userName, String text) {
        String inputType = params.get("input_type");
        boolean isUpload = "upload".equals(inputType);
        String[] mediaMethods = {"sendPhoto", "sendVideo", "sendDocument", "sendAudio", "sendVoice", "sendVideoNote", "sendAnimation", "sendSticker", "setChatPhoto", "setStickerSetThumbnail", "uploadStickerFile"};
        boolean isMedia = java.util.Arrays.asList(mediaMethods).contains(method.apiName);
        
        if (isMedia) {
            String mediaField = getMediaFieldName(method.apiName);
            String mediaValue = params.get(mediaField);
            boolean isContentUri = mediaValue != null && (mediaValue.startsWith("content://") || mediaValue.startsWith("file://"));
            
            if ("upload".equals(inputType) || isContentUri) {
                if (mediaValue != null && !mediaValue.trim().isEmpty()) {
                    new Thread(() -> uploadFileWithUriString(mediaValue, token, method, params, node, chatId, userName, text)).start();
                } else {
                    pickFileAndUpload(token, method, params, node, chatId, userName, text);
                }
                return;
            }
        }
        
        new Thread(() -> {
            try {
                okhttp3.OkHttpClient client = new okhttp3.OkHttpClient();
                okhttp3.FormBody.Builder formBuilder = new okhttp3.FormBody.Builder();
                
                // Ensure media field (photo/video/etc.) is included if present in node properties
                if (isMedia) {
                    String mediaField = getMediaFieldName(method.apiName);
                    String mediaFromNode = node.getProperty(mediaField);
                    if (mediaFromNode != null && !mediaFromNode.trim().isEmpty()) {
                        String resolved = resolveTemplate(mediaFromNode, text, chatId, userName);
                        if (resolved != null && !resolved.trim().isEmpty()) {
                            formBuilder.add(mediaField, resolved);
                        }
                    }
                }
                
                for (Map.Entry<String, String> e : params.entrySet()) {
                    if (!e.getKey().equals("input_type") && !e.getKey().equals("photo") 
                            && !e.getKey().equals("video") && !e.getKey().equals("document")
                            && !e.getKey().equals("audio") && !e.getKey().equals("voice")
                            && !e.getKey().equals("video_note") && !e.getKey().equals("animation")
                            && !e.getKey().equals("sticker") && !e.getKey().equals("thumbnail")) {
                        formBuilder.add(e.getKey(), e.getValue());
                    }
                }
                String url = "https://api.telegram.org/bot" + token + "/" + method.apiName;
                okhttp3.Request request = new okhttp3.Request.Builder()
                        .url(url).post(formBuilder.build()).build();
                okhttp3.Response response = client.newCall(request).execute();
                String body = response.body() != null ? response.body().string() : "";
                response.close();
                handleApiResponse(method, body, node, chatId, userName, text);
            } catch (Exception e) {
                String errMsg = e.getMessage();
                runOnUiThread(() -> {
                    Snackbar.make(canvas, "Error: " + errMsg, Snackbar.LENGTH_SHORT).show();
                    addLog("[API Error] " + method.apiName + ": " + errMsg);
                });
            }
        }).start();
    }
    
    private void handleApiResponse(TelegramMethod method, String body,
                                   FlowNode node, String chatId, String userName, String text) {
        try {
            com.google.gson.JsonObject json =
                    new com.google.gson.JsonParser().parse(body).getAsJsonObject();
            if (json.get("ok").getAsBoolean()) {
                String resultStr = json.has("result") ? json.get("result").toString() : "{}";
                addLog("[API] " + method.apiName + ": " + resultStr);
                final String flowText;
                if ("sendChatAction".equals(method.apiName)) {
                    flowText = text;
                } else {
                    flowText = resultStr;
                    currentMsgData.put("result", resultStr);
                    com.google.gson.JsonElement resultEl = json.get("result");
                    if (resultEl != null) {
                        flattenJsonTo("result", resultEl, currentMsgData);
                    }
                }
                runOnUiThread(() -> {
                    canvas.triggerPulse(node.getId());
                    Snackbar.make(canvas, method.displayName + " berhasil", Snackbar.LENGTH_SHORT).show();
                    continueFlow(node, chatId, userName, flowText);
                });
            } else {
                String desc = json.has("description") ?
                        json.get("description").getAsString() : "Unknown error";
                currentMsgData.put("error", desc);
                runOnUiThread(() -> {
                    Snackbar.make(canvas, "Gagal: " + desc, Snackbar.LENGTH_SHORT).show();
                    addLog("[API Error] " + method.apiName + ": " + desc);
                });
            }
        } catch (Exception e) {
            runOnUiThread(() -> {
                Snackbar.make(canvas, "Error: " + e.getMessage(), Snackbar.LENGTH_SHORT).show();
                addLog("[API Error] " + method.apiName + ": " + e.getMessage());
            });
        }
    }

    private FlowNode fileUploadNode;
    private String fileUploadChatId;
    private String fileUploadUserName;
    private String fileUploadText;

    private void pickFileAndUpload(String token, TelegramMethod method, Map<String, String> params,
                                   FlowNode node, String chatId, String userName, String text) {
        String acceptType;
        switch (method.apiName) {
            case "sendPhoto": acceptType = "image/*"; break;
            case "sendVideo": acceptType = "video/*"; break;
            case "sendAudio": acceptType = "audio/*"; break;
            case "sendVoice": acceptType = "audio/*"; break;
            case "sendVideoNote": acceptType = "video/*"; break;
            case "sendAnimation": acceptType = "image/gif"; break;
            case "sendSticker": acceptType = "image/webp"; break;
            case "setChatPhoto": acceptType = "image/*"; break;
            case "setStickerSetThumbnail": acceptType = "image/*"; break;
            case "uploadStickerFile": acceptType = "image/*"; break;
            case "sendDocument": acceptType = "*/*"; break;
            default: acceptType = "*/*";
        }
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType(acceptType);
        runOnUiThread(() -> {
            fileUploadParams = params;
            fileUploadToken = token;
            fileUploadMethod = method;
            fileUploadNode = node;
            fileUploadChatId = chatId;
            fileUploadUserName = userName;
            fileUploadText = text;
            startActivityForResult(intent, REQUEST_PICK_FILE);
        });
    }

    private static final int REQUEST_PICK_FILE = 2001;
    private Map<String, String> fileUploadParams;
    private String fileUploadToken;
    private TelegramMethod fileUploadMethod;

    private void uploadFileWithUri(Uri uri) {
        final FlowNode node = fileUploadNode;
        final String chatId = fileUploadChatId;
        final String userName = fileUploadUserName;
        final String text = fileUploadText;
        final TelegramMethod method = fileUploadMethod;

        new Thread(() -> {
            try {
                okhttp3.OkHttpClient client = new okhttp3.OkHttpClient();
                java.io.InputStream is = getContentResolver().openInputStream(uri);
                if (is == null) {
                    runOnUiThread(() -> Snackbar.make(canvas, "Gagal buka file", Snackbar.LENGTH_SHORT).show());
                    return;
                }
                byte[] fileBytes = is.readAllBytes();
                is.close();
                String fileName = getFileName(uri);
                String mimeType = getContentResolver().getType(uri);
                if (mimeType == null) mimeType = "application/octet-stream";

                String fieldName = getMediaFieldName(method.apiName);
                okhttp3.RequestBody fileBody = okhttp3.RequestBody.create(fileBytes, okhttp3.MediaType.parse(mimeType));
                okhttp3.MultipartBody.Builder mpBuilder = new okhttp3.MultipartBody.Builder()
                        .setType(okhttp3.MultipartBody.FORM)
                        .addFormDataPart(fieldName, fileName, fileBody);

                for (Map.Entry<String, String> e : fileUploadParams.entrySet()) {
                    String key = e.getKey();
                    if (isMediaParam(key)) continue;
                    mpBuilder.addFormDataPart(key, e.getValue());
                }

                String url = "https://api.telegram.org/bot" + fileUploadToken + "/" + method.apiName;
                okhttp3.Request request = new okhttp3.Request.Builder()
                        .url(url).post(mpBuilder.build()).build();
                okhttp3.Response response = client.newCall(request).execute();
                String body = response.body() != null ? response.body().string() : "";
                response.close();
                handleApiResponse(method, body, node, chatId, userName, text);
            } catch (Exception e) {
                runOnUiThread(() -> {
                    Snackbar.make(canvas, "Upload error: " + e.getMessage(), Snackbar.LENGTH_SHORT).show();
                    addLog("[Upload Error] " + method.apiName + ": " + e.getMessage());
                });
            }
        }).start();
    }

    private void uploadFileWithUriString(String uriString, String token, TelegramMethod method,
                                         Map<String, String> params, FlowNode node,
                                         String chatId, String userName, String text) {
        try {
            android.net.Uri uri = android.net.Uri.parse(uriString);
            okhttp3.OkHttpClient client = new okhttp3.OkHttpClient();
            java.io.InputStream is = getContentResolver().openInputStream(uri);
            if (is == null) {
                runOnUiThread(() -> Snackbar.make(canvas, "Gagal baca file: " + uriString, Snackbar.LENGTH_SHORT).show());
                return;
            }
            byte[] fileBytes = is.readAllBytes();
            is.close();
            String fileName = getFileName(uri);
            String mimeType = getContentResolver().getType(uri);
            if (mimeType == null) mimeType = "application/octet-stream";

            String fieldName = getMediaFieldName(method.apiName);
            okhttp3.RequestBody fileBody = okhttp3.RequestBody.create(fileBytes, okhttp3.MediaType.parse(mimeType));
            okhttp3.MultipartBody.Builder mpBuilder = new okhttp3.MultipartBody.Builder()
                    .setType(okhttp3.MultipartBody.FORM)
                    .addFormDataPart(fieldName, fileName, fileBody);

            for (Map.Entry<String, String> e : params.entrySet()) {
                if (isMediaParam(e.getKey())) continue;
                mpBuilder.addFormDataPart(e.getKey(), e.getValue());
            }

            String url = "https://api.telegram.org/bot" + token + "/" + method.apiName;
            okhttp3.Request request = new okhttp3.Request.Builder().url(url).post(mpBuilder.build()).build();
            okhttp3.Response response = client.newCall(request).execute();
            String body = response.body() != null ? response.body().string() : "";
            response.close();
            handleApiResponse(method, body, node, chatId, userName, text);
        } catch (Exception e) {
            runOnUiThread(() -> {
                Snackbar.make(canvas, "Upload error: " + e.getMessage(), Snackbar.LENGTH_SHORT).show();
                addLog("[Upload Error] " + method.apiName + ": " + e.getMessage());
            });
        }
    }

    private boolean isMediaParam(String key) {
        return key.equals("input_type") || key.equals("caption") || key.equals("parse_mode")
                || key.equals("disable_notification") || key.equals("protect_content")
                || key.equals("message_thread_id") || key.equals("reply_to_message_id")
                || key.equals("allow_paid_broadcast") || key.equals("duration")
                || key.equals("width") || key.equals("height") || key.equals("has_spoiler")
                || key.equals("supports_streaming") || key.equals("thumbnail");
    }

    private String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            try (android.database.Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                    if (nameIndex >= 0) result = cursor.getString(nameIndex);
                }
            }
        }
        if (result == null) result = uri.getLastPathSegment();
        return result != null ? result : "file";
    }

    private String getMediaFieldName(String apiName) {
        switch (apiName) {
            case "sendPhoto": return "photo";
            case "sendVideo": return "video";
            case "sendDocument": return "document";
            case "sendAudio": return "audio";
            case "sendVoice": return "voice";
            case "sendVideoNote": return "video_note";
            case "sendAnimation": return "animation";
            case "sendSticker": return "sticker";
            case "setChatPhoto": return "photo";
            case "setStickerSetThumbnail": return "thumbnail";
            case "uploadStickerFile": return "sticker";
            default: return "document";
        }
    }

    private void flattenJson(String prefix, com.google.gson.JsonObject obj, Map<String, String> out) {
        flattenJsonTo(prefix, obj, out);
    }

    private void flattenJsonTo(String prefix, com.google.gson.JsonElement el, Map<String, String> out) {
        if (el.isJsonObject()) {
            com.google.gson.JsonObject obj = el.getAsJsonObject();
            for (java.util.Map.Entry<String, com.google.gson.JsonElement> e : obj.entrySet()) {
                String key = prefix != null && !prefix.isEmpty() ? prefix + "." + e.getKey() : e.getKey();
                flattenJsonTo(key, e.getValue(), out);
            }
        } else if (el.isJsonArray()) {
            com.google.gson.JsonArray arr = el.getAsJsonArray();
            out.put(prefix, arr.toString());
            for (int i = 0; i < arr.size(); i++) {
                String key = prefix + "." + i;
                flattenJsonTo(key, arr.get(i), out);
            }
        } else if (el.isJsonPrimitive()) {
            out.put(prefix, el.getAsString());
        } else if (el.isJsonNull()) {
            out.put(prefix, "");
        }
    }

    private String resolveTemplate(String val, String text, String chatId, String userName) {
        if (val == null) return null;
        if (!val.contains("{{")) return val;
        val = val.replace("{{text}}", text != null ? text : "");
        val = val.replace("{{message}}", text != null ? text : "");
        val = val.replace("{{chatId}}", chatId != null ? chatId : "");
        val = val.replace("{{username}}", userName != null ? userName : "");

        java.util.List<java.util.Map.Entry<String, String>> sortedData =
                new java.util.ArrayList<>(currentMsgData.entrySet());
        sortedData.sort((a, b) -> Integer.compare(b.getKey().length(), a.getKey().length()));
        for (java.util.Map.Entry<String, String> e : sortedData) {
            String placeholder = "{{" + e.getKey() + "}}";
            if (val.contains(placeholder)) {
                val = val.replace(placeholder, e.getValue() != null ? e.getValue() : "");
            }
        }

        java.util.List<java.util.Map.Entry<String, String>> sortedVars =
                new java.util.ArrayList<>(variables.entrySet());
        sortedVars.sort((a, b) -> Integer.compare(b.getKey().length(), a.getKey().length()));
        for (java.util.Map.Entry<String, String> e : sortedVars) {
            String placeholder = "{{$" + e.getKey() + "}}";
            if (val.contains(placeholder)) {
                val = val.replace(placeholder, e.getValue() != null ? e.getValue() : "");
            }
        }

        val = val.replaceAll("\\{\\{\\s*\\$?[\\w.]+\\s*\\}\\}", "");
        return val;
    }

    private void execLocalAction(FlowNode node, TelegramMethod method, String chatId, String userName, String text) {
        runOnUiThread(() -> Snackbar.make(canvas, method.displayName + " ...", Snackbar.LENGTH_SHORT).show());

        execLocalActionAsync(node, method, chatId, userName, text);
    }

    private void execLocalActionAsync(FlowNode node, TelegramMethod method, String chatId, String userName, String text) {
        new Thread(() -> {
            String result = text;

            switch (method.apiName) {
                case "_add":
                case "_subtract":
                case "_multiply":
                case "_divide":
                case "_modulo": {
                    String aRaw = resolveTemplate(node.getProperty("a"), text, chatId, userName);
                    String bRaw = resolveTemplate(node.getProperty("b"), text, chatId, userName);
                    double a = 0, b = 0;
                    try { a = Double.parseDouble(aRaw); } catch (Exception ignored) {}
                    try { b = Double.parseDouble(bRaw); } catch (Exception ignored) {}
                    double val;
                    switch (method.apiName) {
                        case "_add": val = a + b; break;
                        case "_subtract": val = a - b; break;
                        case "_multiply": val = a * b; break;
                        case "_divide": val = b != 0 ? a / b : 0; break;
                        case "_modulo": val = b != 0 ? a % b : 0; break;
                        default: val = 0;
                    }
                    result = String.valueOf(val);
                    addLog(method.displayName + ": " + result);
                    break;
                }
                case "_random": {
                    String minRaw = resolveTemplate(node.getProperty("min"), text, chatId, userName);
                    String maxRaw = resolveTemplate(node.getProperty("max"), text, chatId, userName);
                    double min = 0, max = 100;
                    try { min = Double.parseDouble(minRaw); } catch (Exception ignored) {}
                    try { max = Double.parseDouble(maxRaw); } catch (Exception ignored) {}
                    double val = min + (max - min) * java.lang.Math.random();
                    result = String.valueOf(val);
                    addLog(method.displayName + ": " + result);
                    break;
                }
                case "_text_append": {
                    String a = resolveTemplate(node.getProperty("a"), text, chatId, userName);
                    String b = resolveTemplate(node.getProperty("b"), text, chatId, userName);
                    result = (a != null ? a : "") + (b != null ? b : "");
                    addLog(method.displayName + ": " + result);
                    break;
                }
                case "_text_replace": {
                    String inputText = resolveTemplate(node.getProperty("text"), text, chatId, userName);
                    String search = resolveTemplate(node.getProperty("search"), text, chatId, userName);
                    String replace = resolveTemplate(node.getProperty("replace"), text, chatId, userName);
                    if (inputText != null && search != null) {
                        result = inputText.replace(search, replace != null ? replace : "");
                    }
                    addLog(method.displayName + ": " + result);
                    break;
                }
                case "_set_variable": {
                    String key = resolveTemplate(node.getProperty("key"), text, chatId, userName);
                    String val = resolveTemplate(node.getProperty("value"), text, chatId, userName);
                    if (key != null && !key.isEmpty()) {
                        variables.put(key, val != null ? val : "");
                        addLog("Set $" + key + " = " + (val != null ? val : ""));
                    }
                    break;
                }
                case "_get_variable": {
                    String key = resolveTemplate(node.getProperty("key"), text, chatId, userName);
                    String defVal = resolveTemplate(node.getProperty("default"), text, chatId, userName);
                    if (key != null && variables.containsKey(key)) {
                        result = variables.get(key);
                    } else {
                        result = defVal != null ? defVal : "";
                    }
                    addLog("Get $" + key + " = " + result);
                    break;
                }
                case "_delay": {
                    String msRaw = resolveTemplate(node.getProperty("ms"), text, chatId, userName);
                    long ms = 1000;
                    try { ms = Long.parseLong(msRaw); } catch (Exception ignored) {}
                    try { Thread.sleep(ms); } catch (Exception ignored) {}
                    addLog("Delay " + ms + "ms");
                    break;
                }
                case "_var_add":
                case "_var_subtract":
                case "_var_multiply":
                case "_var_divide": {
                    String key = resolveTemplate(node.getProperty("key"), text, chatId, userName);
                    String valRaw = resolveTemplate(node.getProperty("value"), text, chatId, userName);
                    if (key == null || key.isEmpty()) break;
                    double current = 0;
                    if (variables.containsKey(key)) {
                        try { current = Double.parseDouble(variables.get(key)); } catch (Exception ignored) {}
                    }
                    double delta = 0;
                    try { delta = Double.parseDouble(valRaw); } catch (Exception ignored) {}
                    switch (method.apiName) {
                        case "_var_add": current += delta; break;
                        case "_var_subtract": current -= delta; break;
                        case "_var_multiply": current *= delta; break;
                        case "_var_divide": if (delta != 0) current /= delta; break;
                    }
                    variables.put(key, String.valueOf(current));
                    result = String.valueOf(current);
                    addLog(method.displayName + " $" + key + " = " + result);
                    break;
                }
                case "_var_append": {
                    String key = resolveTemplate(node.getProperty("key"), text, chatId, userName);
                    String val = resolveTemplate(node.getProperty("value"), text, chatId, userName);
                    if (key == null || key.isEmpty()) break;
                    String existing = variables.containsKey(key) ? variables.get(key) : "";
                    String appended = existing + (val != null ? val : "");
                    variables.put(key, appended);
                    result = appended;
                    addLog(method.displayName + " $" + key + " = " + result);
                    break;
                }
                case "_var_clear": {
                    String key = resolveTemplate(node.getProperty("key"), text, chatId, userName);
                    if (key != null && variables.containsKey(key)) {
                        variables.put(key, "");
                        addLog("Var Clear $" + key);
                    }
                    break;
                }
                case "_var_delete": {
                    String key = resolveTemplate(node.getProperty("key"), text, chatId, userName);
                    if (key != null) {
                        variables.remove(key);
                        addLog("Var Delete $" + key);
                    }
                    break;
                }
                case "_list_create": {
                    String key = resolveTemplate(node.getProperty("key"), text, chatId, userName);
                    String items = resolveTemplate(node.getProperty("items"), text, chatId, userName);
                    if (key == null || key.isEmpty()) break;
                    if (items == null) items = "";
                    String[] parts = items.split(",", -1);
                    ArrayList<String> list = new ArrayList<>();
                    for (String p : parts) list.add(p.trim());
                    variables.put(key, joinList(list));
                    addLog("List Create: " + list.size() + " items");
                    break;
                }
                case "_list_add": {
                    String key = resolveTemplate(node.getProperty("key"), text, chatId, userName);
                    String item = resolveTemplate(node.getProperty("item"), text, chatId, userName);
                    if (key == null || key.isEmpty()) break;
                    ArrayList<String> list = getListVar(key);
                    list.add(item != null ? item : "");
                    variables.put(key, joinList(list));
                    addLog("List Add: " + item);
                    break;
                }
                case "_list_remove": {
                    String key = resolveTemplate(node.getProperty("key"), text, chatId, userName);
                    String idxRaw = resolveTemplate(node.getProperty("index"), text, chatId, userName);
                    if (key == null || key.isEmpty()) break;
                    ArrayList<String> list = getListVar(key);
                    int idx = 0;
                    try { idx = Integer.parseInt(idxRaw); } catch (Exception ignored) {}
                    if (idx >= 0 && idx < list.size()) {
                        String removed = list.remove(idx);
                        variables.put(key, joinList(list));
                        addLog("List Remove index " + idx + ": " + removed);
                    }
                    break;
                }
                case "_list_get": {
                    String key = resolveTemplate(node.getProperty("key"), text, chatId, userName);
                    String idxRaw = resolveTemplate(node.getProperty("index"), text, chatId, userName);
                    if (key == null || key.isEmpty()) break;
                    ArrayList<String> list = getListVar(key);
                    int idx = 0;
                    try { idx = Integer.parseInt(idxRaw); } catch (Exception ignored) {}
                    if (idx >= 0 && idx < list.size()) {
                        result = list.get(idx);
                    }
                    addLog("List Get [" + idx + "] = " + result);
                    break;
                }
                case "_list_size": {
                    String key = resolveTemplate(node.getProperty("key"), text, chatId, userName);
                    if (key == null || key.isEmpty()) break;
                    ArrayList<String> list = getListVar(key);
                    result = String.valueOf(list.size());
                    addLog("List Size: " + result);
                    break;
                }
                case "_list_clear": {
                    String key = resolveTemplate(node.getProperty("key"), text, chatId, userName);
                    if (key != null) {
                        variables.put(key, "");
                        addLog("List Clear");
                    }
                    break;
                }
                case "_list_join": {
                    String key = resolveTemplate(node.getProperty("key"), text, chatId, userName);
                    String sep = resolveTemplate(node.getProperty("separator"), text, chatId, userName);
                    if (key == null || key.isEmpty()) break;
                    if (sep == null) sep = ", ";
                    ArrayList<String> list = getListVar(key);
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < list.size(); i++) {
                        if (i > 0) sb.append(sep);
                        sb.append(list.get(i));
                    }
                    result = sb.toString();
                    addLog("List Join: " + result);
                    break;
                }
                case "_list_shuffle": {
                    String key = resolveTemplate(node.getProperty("key"), text, chatId, userName);
                    if (key == null || key.isEmpty()) break;
                    ArrayList<String> list = getListVar(key);
                    java.util.Collections.shuffle(list);
                    variables.put(key, joinList(list));
                    addLog("List Shuffle");
                    break;
                }
                case "_repeat": {
                    String cntRaw = resolveTemplate(node.getProperty("count"), text, chatId, userName);
                    int count = 3;
                    try { count = Integer.parseInt(cntRaw); } catch (Exception ignored) {}
                    String counterKey = "_repeat_counter_" + chatId + "_" + node.getId();
                    String remainingRaw = variables.get(counterKey);
                    int remaining;
                    if (remainingRaw == null) {
                        remaining = count;
                    } else {
                        try { remaining = Integer.parseInt(remainingRaw); } catch (Exception ignored) { remaining = 0; }
                    }
                    if (remaining > 0) {
                        remaining--;
                        variables.put(counterKey, String.valueOf(remaining));
                        variables.put("_loop_index", String.valueOf(count - remaining - 1));
                        variables.put("_loop_total", String.valueOf(count));
                        addLog("Repeat: iterasi " + (count - remaining) + "/" + count);
                    } else {
                        addLog("Repeat: selesai");
                    }
                    result = text;
                    break;
                }
                case "_wait_until": {
                    String varName = resolveTemplate(node.getProperty("condition_var"), text, chatId, userName);
                    String expected = resolveTemplate(node.getProperty("expected"), text, chatId, userName);
                    String timeoutRaw = resolveTemplate(node.getProperty("timeout_ms"), text, chatId, userName);
                    String intervalRaw = resolveTemplate(node.getProperty("interval_ms"), text, chatId, userName);
                    long timeoutMs = 5000, intervalMs = 200;
                    try { timeoutMs = Long.parseLong(timeoutRaw); } catch (Exception ignored) {}
                    try { intervalMs = Long.parseLong(intervalRaw); } catch (Exception ignored) {}
                    long start = System.currentTimeMillis();
                    boolean found = false;
                    while (System.currentTimeMillis() - start < timeoutMs) {
                        String val = variables.get(varName);
                        if ((val != null && val.equals(expected)) || (val == null && (expected == null || expected.isEmpty()))) {
                            found = true;
                            break;
                        }
                        try { Thread.sleep(intervalMs); } catch (Exception ignored) {}
                    }
                    addLog(found ? "Wait Until: kondisi terpenuhi" : "Wait Until: timeout");
                    result = found ? "true" : "false";
                    break;
                }
                case "_loop_break": {
                    addLog("Loop Break");
                    runOnUiThread(() -> {
                        for (Map.Entry<String, String> e : variables.entrySet()) {
                            if (e.getKey().startsWith("_repeat_counter_") || e.getKey().startsWith("_loop_idx_") || e.getKey().startsWith("_split_idx_")) {
                                variables.put(e.getKey(), "0");
                            }
                        }
                    });
                    break;
                }
                case "_power": {
                    String aRaw = resolveTemplate(node.getProperty("a"), text, chatId, userName);
                    String bRaw = resolveTemplate(node.getProperty("b"), text, chatId, userName);
                    double a = 0, b = 0;
                    try { a = Double.parseDouble(aRaw); } catch (Exception ignored) {}
                    try { b = Double.parseDouble(bRaw); } catch (Exception ignored) {}
                    result = String.valueOf(Math.pow(a, b));
                    addLog(method.displayName + ": " + result);
                    break;
                }
                case "_sqrt": {
                    String valRaw = resolveTemplate(node.getProperty("value"), text, chatId, userName);
                    double v = 0;
                    try { v = Double.parseDouble(valRaw); } catch (Exception ignored) {}
                    result = String.valueOf(Math.sqrt(v));
                    addLog(method.displayName + ": " + result);
                    break;
                }
                case "_abs": {
                    String valRaw = resolveTemplate(node.getProperty("value"), text, chatId, userName);
                    double v = 0;
                    try { v = Double.parseDouble(valRaw); } catch (Exception ignored) {}
                    result = String.valueOf(Math.abs(v));
                    addLog(method.displayName + ": " + result);
                    break;
                }
                case "_round": {
                    String valRaw = resolveTemplate(node.getProperty("value"), text, chatId, userName);
                    double v = 0;
                    try { v = Double.parseDouble(valRaw); } catch (Exception ignored) {}
                    result = String.valueOf(Math.round(v));
                    addLog(method.displayName + ": " + result);
                    break;
                }
                case "_floor": {
                    String valRaw = resolveTemplate(node.getProperty("value"), text, chatId, userName);
                    double v = 0;
                    try { v = Double.parseDouble(valRaw); } catch (Exception ignored) {}
                    result = String.valueOf(Math.floor(v));
                    addLog(method.displayName + ": " + result);
                    break;
                }
                case "_ceil": {
                    String valRaw = resolveTemplate(node.getProperty("value"), text, chatId, userName);
                    double v = 0;
                    try { v = Double.parseDouble(valRaw); } catch (Exception ignored) {}
                    result = String.valueOf(Math.ceil(v));
                    addLog(method.displayName + ": " + result);
                    break;
                }
                case "_min": {
                    String aRaw = resolveTemplate(node.getProperty("a"), text, chatId, userName);
                    String bRaw = resolveTemplate(node.getProperty("b"), text, chatId, userName);
                    double a = 0, b = 0;
                    try { a = Double.parseDouble(aRaw); } catch (Exception ignored) {}
                    try { b = Double.parseDouble(bRaw); } catch (Exception ignored) {}
                    result = String.valueOf(Math.min(a, b));
                    addLog(method.displayName + ": " + result);
                    break;
                }
                case "_max": {
                    String aRaw = resolveTemplate(node.getProperty("a"), text, chatId, userName);
                    String bRaw = resolveTemplate(node.getProperty("b"), text, chatId, userName);
                    double a = 0, b = 0;
                    try { a = Double.parseDouble(aRaw); } catch (Exception ignored) {}
                    try { b = Double.parseDouble(bRaw); } catch (Exception ignored) {}
                    result = String.valueOf(Math.max(a, b));
                    addLog(method.displayName + ": " + result);
                    break;
                }
                case "_clamp": {
                    String valRaw = resolveTemplate(node.getProperty("value"), text, chatId, userName);
                    String minRaw = resolveTemplate(node.getProperty("min"), text, chatId, userName);
                    String maxRaw = resolveTemplate(node.getProperty("max"), text, chatId, userName);
                    double v = 0, min = 0, max = 100;
                    try { v = Double.parseDouble(valRaw); } catch (Exception ignored) {}
                    try { min = Double.parseDouble(minRaw); } catch (Exception ignored) {}
                    try { max = Double.parseDouble(maxRaw); } catch (Exception ignored) {}
                    result = String.valueOf(Math.max(min, Math.min(max, v)));
                    addLog(method.displayName + ": " + result);
                    break;
                }
                case "_file_read": {
                    String path = resolveTemplate(node.getProperty("path"), text, chatId, userName);
                    if (path != null) {
                        try {
                            java.io.File file = new java.io.File(path);
                            if (file.exists()) {
                                result = new String(java.nio.file.Files.readAllBytes(file.toPath()));
                            } else {
                                result = "";
                                addLog("File not found: " + path);
                            }
                        } catch (Exception e) {
                            result = "";
                            addLog("File Read error: " + e.getMessage());
                        }
                        addLog("File Read: " + path);
                    }
                    break;
                }
                case "_file_write": {
                    String path = resolveTemplate(node.getProperty("path"), text, chatId, userName);
                    String content = resolveTemplate(node.getProperty("content"), text, chatId, userName);
                    if (path != null && content != null) {
                        try {
                            java.io.File file = new java.io.File(path);
                            file.getParentFile().mkdirs();
                            java.nio.file.Files.write(file.toPath(), content.getBytes());
                            addLog("File Write: " + path);
                        } catch (Exception e) {
                            addLog("File Write error: " + e.getMessage());
                        }
                    }
                    break;
                }
                case "_file_append": {
                    String path = resolveTemplate(node.getProperty("path"), text, chatId, userName);
                    String content = resolveTemplate(node.getProperty("content"), text, chatId, userName);
                    if (path != null && content != null) {
                        try {
                            java.io.File file = new java.io.File(path);
                            file.getParentFile().mkdirs();
                            java.nio.file.Files.write(file.toPath(), content.getBytes(),
                                    java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
                            addLog("File Append: " + path);
                        } catch (Exception e) {
                            addLog("File Append error: " + e.getMessage());
                        }
                    }
                    break;
                }
                case "_file_delete": {
                    String path = resolveTemplate(node.getProperty("path"), text, chatId, userName);
                    if (path != null) {
                        java.io.File file = new java.io.File(path);
                        if (file.delete()) {
                            addLog("File Delete: " + path);
                        } else {
                            addLog("File Delete gagal: " + path);
                        }
                    }
                    break;
                }
                case "_file_exists": {
                    String path = resolveTemplate(node.getProperty("path"), text, chatId, userName);
                    if (path != null) {
                        result = String.valueOf(new java.io.File(path).exists());
                        addLog("File Exists: " + path + " = " + result);
                    }
                    break;
                }
                case "_file_list": {
                    String dir = resolveTemplate(node.getProperty("dir"), text, chatId, userName);
                    if (dir != null) {
                        java.io.File folder = new java.io.File(dir);
                        java.io.File[] files = folder.listFiles();
                        if (files != null) {
                            StringBuilder sb = new StringBuilder();
                            for (int i = 0; i < files.length; i++) {
                                if (i > 0) sb.append("\n");
                                sb.append(files[i].getName()).append(files[i].isDirectory() ? "/" : "");
                            }
                            result = sb.toString();
                            addLog("File List: " + files.length + " entries");
                        } else {
                            result = "";
                            addLog("File List: direktori tidak ditemukan");
                        }
                    }
                    break;
                }
                case "_http_request": {
                    String httpMethod = resolveTemplate(node.getProperty("method"), text, chatId, userName);
                    String url = resolveTemplate(node.getProperty("url"), text, chatId, userName);
                    String headersJson = resolveTemplate(node.getProperty("headers"), text, chatId, userName);
                    String reqBody = resolveTemplate(node.getProperty("body"), text, chatId, userName);
                    if (httpMethod == null || httpMethod.isEmpty()) httpMethod = "GET";
                    if (url == null || url.isEmpty()) break;
                    try {
                        okhttp3.OkHttpClient client = new okhttp3.OkHttpClient();
                        okhttp3.Request.Builder builder = new okhttp3.Request.Builder().url(url);
                        if (headersJson != null && !headersJson.isEmpty()) {
                            try {
                                com.google.gson.JsonObject hdrs = com.google.gson.JsonParser.parseString(headersJson).getAsJsonObject();
                                for (String k : hdrs.keySet()) {
                                    builder.addHeader(k, hdrs.get(k).getAsString());
                                }
                            } catch (Exception ignored) {}
                        }
                        if (httpMethod.equalsIgnoreCase("GET")) {
                            builder.get();
                        } else {
                            okhttp3.MediaType mediaType = okhttp3.MediaType.parse("application/json; charset=utf-8");
                            okhttp3.RequestBody body = okhttp3.RequestBody.create(
                                    reqBody != null ? reqBody : "", mediaType);
                            switch (httpMethod.toUpperCase()) {
                                case "POST": builder.post(body); break;
                                case "PUT": builder.put(body); break;
                                case "DELETE": builder.delete(body); break;
                                case "PATCH": builder.patch(body); break;
                                case "HEAD": builder.head(); break;
                                default: builder.get();
                            }
                        }
                        okhttp3.Response response = client.newCall(builder.build()).execute();
                        result = response.body() != null ? response.body().string() : "";
                        response.close();
                        addLog("HTTP " + httpMethod + " " + url + " -> " + result.length() + " chars");
                    } catch (Exception e) {
                        result = "";
                        addLog("HTTP Error: " + e.getMessage());
                    }
                    break;
                }
                case "_switch": {
                    String switchValue = resolveTemplate(node.getProperty("value"), text, chatId, userName);
                    result = switchValue != null ? switchValue : "";
                    addLog("Switch: " + result);
                    break;
                }
                case "_loop_items": {
                    String srcRaw = resolveTemplate(node.getProperty("source"), text, chatId, userName);
                    if (srcRaw == null) srcRaw = result;
                    String loopIdxKey = "_loop_idx_" + node.getId();
                    String loopRaw = variables.get(loopIdxKey);
                    int idx = 0;
                    if (loopRaw != null) {
                        try { idx = Integer.parseInt(loopRaw); } catch (Exception ignored) {}
                    }
                    try {
                        com.google.gson.JsonArray arr = com.google.gson.JsonParser.parseString(srcRaw).getAsJsonArray();
                        if (idx < arr.size()) {
                            com.google.gson.JsonElement item = arr.get(idx);
                            idx++;
                            variables.put(loopIdxKey, String.valueOf(idx));
                            String itemStr = item.isJsonPrimitive() ? item.getAsString() : item.toString();
                            variables.put("loop_item", itemStr);
                            variables.put("loop_index", String.valueOf(idx - 1));
                            variables.put("loop_total", String.valueOf(arr.size()));
                            com.google.gson.JsonObject obj = new com.google.gson.JsonObject();
                            obj.addProperty("value", itemStr);
                            obj.addProperty("index", idx - 1);
                            obj.addProperty("total", arr.size());
                            result = obj.toString();
                            addLog("Loop: item " + idx + "/" + arr.size() + " = " + itemStr);
                        } else {
                            variables.remove(loopIdxKey);
                            result = "{\"done\":true}";
                            addLog("Loop: selesai (" + arr.size() + " items)");
                        }
                    } catch (Exception e) {
                        variables.remove(loopIdxKey);
                        result = "{\"error\":\"Invalid array: " + e.getMessage() + "\"}";
                        addLog("Loop Error: " + e.getMessage());
                    }
                    break;
                }
                case "_data_set": {
                    String mode = node.getProperty("mode");
                    if (mode == null) mode = "set";
                    String field = resolveTemplate(node.getProperty("field"), text, chatId, userName);
                    String value = resolveTemplate(node.getProperty("value"), text, chatId, userName);
                    String newName = resolveTemplate(node.getProperty("new_name"), text, chatId, userName);
                    switch (mode) {
                        case "remove":
                            if (field != null) currentMsgData.remove(field);
                            addLog("Edit Fields: remove " + field);
                            break;
                        case "rename":
                            if (field != null && newName != null && currentMsgData.containsKey(field)) {
                                currentMsgData.put(newName, currentMsgData.remove(field));
                            }
                            addLog("Edit Fields: rename " + field + " -> " + newName);
                            break;
                        default:
                            if (field != null && value != null) {
                                currentMsgData.put(field, value);
                            }
                            addLog("Edit Fields: set " + field + " = " + value);
                            break;
                    }
                    result = text;
                    break;
                }
                case "_data_filter": {
                    String srcRaw = resolveTemplate(node.getProperty("source"), text, chatId, userName);
                    if (srcRaw == null) srcRaw = result;
                    String filterField = resolveTemplate(node.getProperty("field"), text, chatId, userName);
                    String condition = node.getProperty("condition");
                    if (condition == null) condition = "equals";
                    String filterValue = resolveTemplate(node.getProperty("value"), text, chatId, userName);
                    try {
                        com.google.gson.JsonArray arr = com.google.gson.JsonParser.parseString(srcRaw).getAsJsonArray();
                        com.google.gson.JsonArray out = new com.google.gson.JsonArray();
                        for (int i = 0; i < arr.size(); i++) {
                            boolean match = false;
                            com.google.gson.JsonElement el = arr.get(i);
                            String val = el.isJsonObject() && filterField != null
                                    ? getJsonString(el.getAsJsonObject().get(filterField))
                                    : el.isJsonPrimitive() ? el.getAsString() : el.toString();
                            if (val == null) val = "";
                            switch (condition) {
                                case "equals": match = val.equals(filterValue); break;
                                case "contains": match = filterValue != null && val.contains(filterValue); break;
                                case "greater": match = filterValue != null && Double.parseDouble(val) > Double.parseDouble(filterValue); break;
                                case "less": match = filterValue != null && Double.parseDouble(val) < Double.parseDouble(filterValue); break;
                                case "is_empty": match = val.isEmpty(); break;
                                case "is_true": match = "true".equals(val); break;
                                default: match = true;
                            }
                            if (match) out.add(el);
                        }
                        result = out.toString();
                        addLog("Filter: " + arr.size() + " -> " + out.size() + " items");
                    } catch (Exception e) {
                        result = "[]";
                        addLog("Filter Error: " + e.getMessage());
                    }
                    break;
                }
                case "_data_splitout": {
                    String srcRaw = resolveTemplate(node.getProperty("source"), text, chatId, userName);
                    if (srcRaw == null) srcRaw = result;
                    String key = node.getProperty("key");
                    try {
                        com.google.gson.JsonArray arr = com.google.gson.JsonParser.parseString(srcRaw).getAsJsonArray();
                        String idxKey = "_split_idx_" + node.getId();
                        String rawIdx = variables.get(idxKey);
                        int idx = 0;
                        if (rawIdx != null) { try { idx = Integer.parseInt(rawIdx); } catch (Exception ignored) {} }
                        if (arr.size() > 0 && idx < arr.size()) {
                            com.google.gson.JsonElement item = arr.get(idx);
                            result = item.isJsonPrimitive() ? item.getAsString() : item.toString();
                            if (key != null && !key.isEmpty() && item.isJsonObject()) {
                                com.google.gson.JsonElement keyVal = item.getAsJsonObject().get(key);
                                if (keyVal != null) result = keyVal.isJsonPrimitive() ? keyVal.getAsString() : keyVal.toString();
                            }
                            idx++;
                            variables.put(idxKey, String.valueOf(idx));
                            variables.put("split_item_" + node.getId(), result);
                            if (key != null && !key.isEmpty()) {
                                variables.put("split_key", result);
                            }
                            variables.put("split_index", String.valueOf(idx - 1));
                            variables.put("split_total", String.valueOf(arr.size()));
                            addLog("Split Out: item " + idx + "/" + arr.size() + " = " + result);
                        } else {
                            variables.remove(idxKey);
                            result = "{\"done\":true}";
                            addLog("Split Out: selesai (" + arr.size() + " items)");
                        }
                    } catch (Exception e) {
                        result = "{\"error\":\"" + e.getMessage() + "\"}";
                        addLog("Split Out Error: " + e.getMessage());
                    }
                    break;
                }
                case "_data_aggregate": {
                    String srcRaw = resolveTemplate(node.getProperty("source"), text, chatId, userName);
                    if (srcRaw == null) srcRaw = result;
                    String groupKey = resolveTemplate(node.getProperty("group_key"), text, chatId, userName);
                    try {
                        com.google.gson.JsonArray arr = com.google.gson.JsonParser.parseString(srcRaw).getAsJsonArray();
                        if (groupKey == null || groupKey.isEmpty()) {
                            com.google.gson.JsonArray out = new com.google.gson.JsonArray();
                            for (int i = 0; i < arr.size(); i++) {
                                com.google.gson.JsonElement item = arr.get(i);
                                if (item.isJsonObject()) {
                                    out.add(item);
                                } else {
                                    com.google.gson.JsonObject obj = new com.google.gson.JsonObject();
                                    obj.addProperty("value", item.isJsonPrimitive() ? item.getAsString() : item.toString());
                                    out.add(obj);
                                }
                            }
                            result = out.toString();
                        } else {
                            java.util.LinkedHashMap<String, com.google.gson.JsonArray> groups = new java.util.LinkedHashMap<>();
                            for (int i = 0; i < arr.size(); i++) {
                                com.google.gson.JsonElement item = arr.get(i);
                                String gKey = item.isJsonObject() ? getJsonString(item.getAsJsonObject().get(groupKey)) : "";
                                if (gKey == null) gKey = "";
                                if (!groups.containsKey(gKey)) groups.put(gKey, new com.google.gson.JsonArray());
                                groups.get(gKey).add(item);
                            }
                            com.google.gson.JsonArray out = new com.google.gson.JsonArray();
                            for (java.util.Map.Entry<String, com.google.gson.JsonArray> g : groups.entrySet()) {
                                com.google.gson.JsonObject grp = new com.google.gson.JsonObject();
                                grp.addProperty("key", g.getKey());
                                grp.add("items", g.getValue());
                                grp.addProperty("count", g.getValue().size());
                                out.add(grp);
                            }
                            result = out.toString();
                        }
                        addLog("Aggregate: " + result);
                    } catch (Exception e) {
                        result = "[]";
                        addLog("Aggregate Error: " + e.getMessage());
                    }
                    break;
                }
                case "_data_summarize": {
                    String srcRaw = resolveTemplate(node.getProperty("source"), text, chatId, userName);
                    if (srcRaw == null) srcRaw = result;
                    String groupField = resolveTemplate(node.getProperty("group_field"), text, chatId, userName);
                    String aggregation = node.getProperty("aggregation");
                    if (aggregation == null) aggregation = "count";
                    String valField = resolveTemplate(node.getProperty("value_field"), text, chatId, userName);
                    try {
                        com.google.gson.JsonArray arr = com.google.gson.JsonParser.parseString(srcRaw).getAsJsonArray();
                        java.util.LinkedHashMap<String, com.google.gson.JsonArray> groups = new java.util.LinkedHashMap<>();
                        if (groupField != null && !groupField.isEmpty()) {
                            for (int i = 0; i < arr.size(); i++) {
                                com.google.gson.JsonElement item = arr.get(i);
                                String gKey = item.isJsonObject() ? getJsonString(item.getAsJsonObject().get(groupField)) : "";
                                if (gKey == null) gKey = "";
                                if (!groups.containsKey(gKey)) groups.put(gKey, new com.google.gson.JsonArray());
                                groups.get(gKey).add(item);
                            }
                        } else {
                            groups.put("all", arr);
                        }
                        com.google.gson.JsonArray out = new com.google.gson.JsonArray();
                        for (java.util.Map.Entry<String, com.google.gson.JsonArray> g : groups.entrySet()) {
                            com.google.gson.JsonObject row = new com.google.gson.JsonObject();
                            row.addProperty("group", g.getKey());
                            com.google.gson.JsonArray items = g.getValue();
                            row.addProperty("count", items.size());
                            if ("sum".equals(aggregation) || "avg".equals(aggregation) || "min".equals(aggregation) || "max".equals(aggregation)) {
                                double total = 0;
                                double minVal = Double.MAX_VALUE;
                                double maxVal = -Double.MAX_VALUE;
                                int numericCount = 0;
                                for (int i = 0; i < items.size(); i++) {
                                    String raw = null;
                                    if (valField != null && !valField.isEmpty() && items.get(i).isJsonObject()) {
                                        raw = getJsonString(items.get(i).getAsJsonObject().get(valField));
                                    } else {
                                        raw = items.get(i).isJsonPrimitive() ? items.get(i).getAsString() : null;
                                    }
                                    if (raw != null) {
                                        try {
                                            double d = Double.parseDouble(raw);
                                            total += d;
                                            if (d < minVal) minVal = d;
                                            if (d > maxVal) maxVal = d;
                                            numericCount++;
                                        } catch (Exception ignored) {}
                                    }
                                }
                                switch (aggregation) {
                                    case "sum": row.addProperty("result", total); break;
                                    case "avg": row.addProperty("result", numericCount > 0 ? total / numericCount : 0); break;
                                    case "min": row.addProperty("result", numericCount > 0 ? minVal : 0); break;
                                    case "max": row.addProperty("result", numericCount > 0 ? maxVal : 0); break;
                                }
                            }
                            out.add(row);
                        }
                        result = out.toString();
                        addLog("Summarize: " + result);
                    } catch (Exception e) {
                        result = "[]";
                        addLog("Summarize Error: " + e.getMessage());
                    }
                    break;
                }
                case "_data_merge": {
                    String mode = node.getProperty("mode");
                    if (mode == null) mode = "combine";
                    String src2Raw = resolveTemplate(node.getProperty("source2"), text, chatId, userName);
                    if (src2Raw == null) src2Raw = "[]";
                    String mergeField = resolveTemplate(node.getProperty("merge_field"), text, chatId, userName);
                    try {
                        com.google.gson.JsonArray arr1 = com.google.gson.JsonParser.parseString(result).getAsJsonArray();
                        com.google.gson.JsonArray arr2 = com.google.gson.JsonParser.parseString(src2Raw).getAsJsonArray();
                        com.google.gson.JsonArray out = new com.google.gson.JsonArray();
                        if ("combine".equals(mode)) {
                            for (com.google.gson.JsonElement e : arr1) out.add(e);
                            for (com.google.gson.JsonElement e : arr2) out.add(e);
                        } else if ("merge_by_field".equals(mode) && mergeField != null) {
                            java.util.HashMap<String, com.google.gson.JsonObject> map = new java.util.HashMap<>();
                            for (com.google.gson.JsonElement e : arr1) {
                                if (e.isJsonObject()) {
                                    String key = getJsonString(e.getAsJsonObject().get(mergeField));
                                    if (key != null) map.put(key, e.getAsJsonObject());
                                }
                            }
                            for (com.google.gson.JsonElement e : arr2) {
                                if (e.isJsonObject()) {
                                    String key = getJsonString(e.getAsJsonObject().get(mergeField));
                                    if (key != null && map.containsKey(key)) {
                                        com.google.gson.JsonObject merged = new com.google.gson.JsonObject();
                                        com.google.gson.JsonObject obj2 = e.getAsJsonObject();
                                        for (String k : map.get(key).keySet()) merged.add(k, map.get(key).get(k));
                                        for (String k : obj2.keySet()) merged.add(k, obj2.get(k));
                                        out.add(merged);
                                        map.remove(key);
                                    }
                                }
                            }
                            for (com.google.gson.JsonObject remaining : map.values()) out.add(remaining);
                        }
                        result = out.toString();
                        addLog("Merge: " + arr1.size() + " + " + arr2.size() + " = " + out.size() + " items");
                    } catch (Exception e) {
                        result = "[]";
                        addLog("Merge Error: " + e.getMessage());
                    }
                    break;
                }
                case "_date_time": {
                    String op = node.getProperty("operation");
                    if (op == null) op = "now";
                    String fmt = node.getProperty("format");
                    if (fmt == null) fmt = "dd/MM/yyyy HH:mm:ss";
                    String valStr = resolveTemplate(node.getProperty("value"), text, chatId, userName);
                    String srcDate = resolveTemplate(node.getProperty("source_date"), text, chatId, userName);
                    try {
                        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat(fmt, java.util.Locale.getDefault());
                        java.util.Date now = new java.util.Date();
                        java.util.Date source = now;
                        if (op.equals("format_iso")) {
                            java.text.SimpleDateFormat isoFmt = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.getDefault());
                            isoFmt.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
                            result = isoFmt.format(now);
                        } else if (op.equals("format")) {
                            if (srcDate != null && !srcDate.isEmpty()) {
                                try {
                                    source = sdf.parse(srcDate);
                                } catch (Exception e) {
                                    try {
                                        java.text.SimpleDateFormat iso = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.getDefault());
                                        iso.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
                                        source = iso.parse(srcDate);
                                    } catch (Exception e2) {
                                        source = now;
                                    }
                                }
                            }
                            result = sdf.format(source);
                        } else if (op.equals("now")) {
                            result = sdf.format(now);
                        } else if (op.startsWith("add_")) {
                            java.util.Calendar cal = java.util.Calendar.getInstance();
                            if (srcDate != null && !srcDate.isEmpty()) {
                                try { cal.setTime(sdf.parse(srcDate)); } catch (Exception ignored) {}
                            }
                            int amount = 0;
                            try { amount = Integer.parseInt(valStr != null ? valStr : "0"); } catch (Exception ignored) {}
                            switch (op) {
                                case "add_seconds": cal.add(java.util.Calendar.SECOND, amount); break;
                                case "add_minutes": cal.add(java.util.Calendar.MINUTE, amount); break;
                                case "add_hours": cal.add(java.util.Calendar.HOUR_OF_DAY, amount); break;
                                case "add_days": cal.add(java.util.Calendar.DAY_OF_MONTH, amount); break;
                                case "add_months": cal.add(java.util.Calendar.MONTH, amount); break;
                                case "add_years": cal.add(java.util.Calendar.YEAR, amount); break;
                            }
                            result = sdf.format(cal.getTime());
                        } else if (op.equals("diff")) {
                            if (srcDate != null && !srcDate.isEmpty() && valStr != null && !valStr.isEmpty()) {
                                try {
                                    java.util.Date d1 = sdf.parse(srcDate);
                                    java.util.Date d2 = sdf.parse(valStr);
                                    long diffMs = Math.abs(d2.getTime() - d1.getTime());
                                    result = String.valueOf(diffMs);
                                } catch (Exception e) {
                                    result = "0";
                                }
                            } else {
                                result = "0";
                            }
                        } else {
                            result = sdf.format(now);
                        }
                        addLog("Date/Time: " + result);
                    } catch (Exception e) {
                        result = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss", java.util.Locale.getDefault()).format(new java.util.Date());
                        addLog("Date/Time Error: " + e.getMessage());
                    }
                    break;
                }
                case "_log": {
                    String msg = resolveTemplate(node.getProperty("message"), text, chatId, userName);
                    if (msg == null || msg.isEmpty()) msg = text;
                    addLog("LOG: " + msg);
                    result = msg;
                    break;
                }
            }

            final String finalResult = result;
            currentMsgData.put("result", finalResult);
            try {
                com.google.gson.JsonElement el = com.google.gson.JsonParser.parseString(finalResult);
                if (el.isJsonObject() || el.isJsonArray()) {
                    flattenJsonTo("result", el, currentMsgData);
                }
            } catch (Exception ignored) {}
            runOnUiThread(() -> {
                canvas.triggerPulse(node.getId());
                continueFlow(node, chatId, userName, finalResult);
            });
        }).start();
    }

    private String joinList(ArrayList<String> list) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append("||");
            sb.append(list.get(i));
        }
        return sb.toString();
    }

    private ArrayList<String> getListVar(String key) {
        String raw = variables.get(key);
        if (raw == null || raw.isEmpty()) return new ArrayList<>();
        String[] parts = raw.split("\\|\\|", -1);
        ArrayList<String> list = new ArrayList<>();
        for (String p : parts) list.add(p);
        return list;
    }

    private void execPhoneAction(FlowNode node, TelegramMethod method, String chatId, String userName, String text) {
        runOnUiThread(() -> {
            String methodName = method.apiName;
            addLog("Phone: " + method.displayName);

            switch (methodName) {
                case "_phone_flashlight": {
                    String state = resolveTemplate(node.getProperty("state"), text, chatId, userName);
                    boolean on = "on".equalsIgnoreCase(state);
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                        if (checkSelfPermission(android.Manifest.permission.CAMERA) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                            Snackbar.make(canvas, "Izin kamera diperlukan untuk senter", Snackbar.LENGTH_LONG)
                                    .setAction("IZIN", v -> requestPermissions(
                                            new String[]{android.Manifest.permission.CAMERA}, 1001))
                                    .show();
                            return;
                        }
                        try {
                            android.hardware.camera2.CameraManager cm = (android.hardware.camera2.CameraManager) getSystemService(CAMERA_SERVICE);
                            String cameraId = null;
                            for (String id : cm.getCameraIdList()) {
                                android.hardware.camera2.CameraCharacteristics chars = cm.getCameraCharacteristics(id);
                                Boolean flashAvail = chars.get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE);
                                if (flashAvail != null && flashAvail) { cameraId = id; break; }
                            }
                            if (cameraId != null) {
                                cm.setTorchMode(cameraId, on);
                                addLog(on ? "Senter menyala" : "Senter mati");
                            } else {
                                addLog("Tidak ada flash tersedia");
                            }
                        } catch (Exception e) {
                            addLog("Gagal senter: " + e.getMessage());
                        }
                    }
                    break;
                }
                case "_phone_vibrate": {
                    String durStr = resolveTemplate(node.getProperty("duration"), text, chatId, userName);
                    long dur = 500;
                    try { dur = Long.parseLong(durStr); } catch (Exception ignored) {}
                    android.os.Vibrator vibrator = (android.os.Vibrator) getSystemService(VIBRATOR_SERVICE);
                    if (vibrator != null && vibrator.hasVibrator()) {
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                            vibrator.vibrate(android.os.VibrationEffect.createOneShot(dur,
                                    android.os.VibrationEffect.DEFAULT_AMPLITUDE));
                        } else {
                            vibrator.vibrate(dur);
                        }
                        addLog("Getar " + dur + "ms");
                    } else {
                        addLog("Tidak ada vibrator");
                    }
                    break;
                }
                case "_phone_toast": {
                    String msg = resolveTemplate(node.getProperty("message"), text, chatId, userName);
                    android.widget.Toast.makeText(this, msg != null ? msg : "", android.widget.Toast.LENGTH_SHORT).show();
                    addLog("Toast: " + msg);
                    break;
                }
                case "_phone_battery": {
                    android.content.IntentFilter ifilter = new android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED);
                    android.content.Intent batteryStatus = registerReceiver(null, ifilter);
                    if (batteryStatus != null) {
                        int level = batteryStatus.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1);
                        int scale = batteryStatus.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, -1);
                        int pct = (int) (level * 100.0 / scale);
                        int status = batteryStatus.getIntExtra(android.os.BatteryManager.EXTRA_STATUS, -1);
                        String statusStr = status == android.os.BatteryManager.BATTERY_STATUS_CHARGING ? "Mengisi" : "Tidak";
                        String plugged = batteryStatus.getIntExtra(android.os.BatteryManager.EXTRA_PLUGGED, 0) > 0 ? "Ya" : "Tidak";
                        addLog("Baterai: " + pct + "% (" + statusStr + ", charger: " + plugged + ")");
                    }
                    break;
                }
                case "_phone_device_info": {
                    String info = "Model: " + android.os.Build.MODEL
                            + ", Android: " + android.os.Build.VERSION.RELEASE
                            + ", API: " + android.os.Build.VERSION.SDK_INT
                            + ", Brand: " + android.os.Build.BRAND
                            + ", Device: " + android.os.Build.DEVICE;
                    addLog(info);
                    break;
                }
                case "_phone_open_url": {
                    String url = resolveTemplate(node.getProperty("url"), text, chatId, userName);
                    if (url != null && !url.isEmpty()) {
                        if (!url.startsWith("http://") && !url.startsWith("https://")) {
                            url = "https://" + url;
                        }
                        android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url));
                        intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        addLog("Buka URL: " + url);
                    }
                    break;
                }
                case "_phone_clipboard_set": {
                    String clipText = resolveTemplate(node.getProperty("text"), text, chatId, userName);
                    android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                    android.content.ClipData clip = android.content.ClipData.newPlainText("TgFlowBot", clipText != null ? clipText : "");
                    clipboard.setPrimaryClip(clip);
                    addLog("Clipboard: " + clipText);
                    break;
                }
                case "_phone_volume": {
                    String lvlStr = resolveTemplate(node.getProperty("level"), text, chatId, userName);
                    int level = 50;
                    try { level = Integer.parseInt(lvlStr); } catch (Exception ignored) {}
                    android.media.AudioManager audio = (android.media.AudioManager) getSystemService(AUDIO_SERVICE);
                    int maxVol = audio.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC);
                    int vol = level * maxVol / 100;
                    audio.setStreamVolume(android.media.AudioManager.STREAM_MUSIC, vol, 0);
                    addLog("Volume: " + level + "%");
                    break;
                }
                case "_phone_brightness": {
                    String lvlStr = resolveTemplate(node.getProperty("level"), text, chatId, userName);
                    int brightness = 128;
                    try { brightness = Integer.parseInt(lvlStr); } catch (Exception ignored) {}
                    if (brightness < 0) brightness = 0;
                    if (brightness > 255) brightness = 255;
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                        if (android.provider.Settings.System.canWrite(this)) {
                            android.provider.Settings.System.putInt(getContentResolver(),
                                    android.provider.Settings.System.SCREEN_BRIGHTNESS, brightness);
                            addLog("Kecerahan: " + brightness);
                        } else {
                            Snackbar.make(canvas, "Izin tulis diperlukan untuk kecerahan", Snackbar.LENGTH_LONG)
                                    .setAction("IZIN", v -> {
                                        android.content.Intent intent = new android.content.Intent(
                                                android.provider.Settings.ACTION_MANAGE_WRITE_SETTINGS);
                                        intent.setData(android.net.Uri.parse("package:" + getPackageName()));
                                        intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
                                        startActivity(intent);
                                    })
                                    .show();
                        }
                    }
                    break;
                }
                case "_phone_tts": {
                    String ttsText = resolveTemplate(node.getProperty("text"), text, chatId, userName);
                    String langRaw = resolveTemplate(node.getProperty("language"), text, chatId, userName);
                    if (langRaw == null || langRaw.isEmpty()) langRaw = "id-ID";
                    if (ttsText == null || ttsText.isEmpty()) ttsText = text;
                    final String ttsFinal = ttsText;
                    final String lang = langRaw;
                    final android.speech.tts.TextToSpeech[] ttsRef = new android.speech.tts.TextToSpeech[1];
                    ttsRef[0] = new android.speech.tts.TextToSpeech(this, status -> {
                        if (status == android.speech.tts.TextToSpeech.SUCCESS) {
                            java.util.Locale locale = java.util.Locale.forLanguageTag(lang);
                            ttsRef[0].setLanguage(locale);
                            ttsRef[0].speak(ttsFinal, android.speech.tts.TextToSpeech.QUEUE_FLUSH, null, "tts_" + node.getId());
                            addLog("TTS: " + ttsFinal);
                        } else {
                            addLog("TTS gagal inisialisasi");
                        }
                    });
                    break;
                }
                case "_phone_stt": {
                    String sttPrompt = resolveTemplate(node.getProperty("prompt"), text, chatId, userName);
                    String timeoutRaw = resolveTemplate(node.getProperty("timeout_sec"), text, chatId, userName);
                    int timeoutSecTmp = 5;
                    try { timeoutSecTmp = Integer.parseInt(timeoutRaw); } catch (Exception ignored) {}
                    final int timeoutSec = timeoutSecTmp;
                    if (checkSelfPermission(android.Manifest.permission.RECORD_AUDIO)
                            != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                        Snackbar.make(canvas, "Izin mikrofon diperlukan untuk STT", Snackbar.LENGTH_LONG)
                                .setAction("IZIN", v -> requestPermissions(
                                        new String[]{android.Manifest.permission.RECORD_AUDIO}, 1002))
                                .show();
                        break;
                    }
                    addLog("STT: dengarkan...");
                    final String sttText = text;
                    final String sttChatId = chatId;
                    final String sttUser = userName;
                    final FlowNode sttNode = node;
                    final java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
                    final String[] sttResult = {""};
                    final android.speech.SpeechRecognizer[] recognizerRef = new android.speech.SpeechRecognizer[1];
                    final android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
                    mainHandler.post(() -> {
                        android.content.Intent sttIntent = new android.content.Intent(
                                android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                        sttIntent.putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                                android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                        sttIntent.putExtra(android.speech.RecognizerIntent.EXTRA_PROMPT,
                                sttPrompt != null ? sttPrompt : "Silakan bicara");
                        sttIntent.putExtra(android.speech.RecognizerIntent.EXTRA_MAX_RESULTS, 1);
                        android.speech.SpeechRecognizer recognizer =
                                android.speech.SpeechRecognizer.createSpeechRecognizer(
                                        com.tgflowbot.MainActivity.this);
                        recognizerRef[0] = recognizer;
                        recognizer.setRecognitionListener(new android.speech.RecognitionListener() {
                            @Override public void onReadyForSpeech(android.os.Bundle params) {}
                            @Override public void onBeginningOfSpeech() {}
                            @Override public void onRmsChanged(float rmsDb) {}
                            @Override public void onBufferReceived(byte[] buffer) {}
                            @Override public void onEndOfSpeech() {}
                            @Override public void onError(int err) {
                                addLog("STT error: " + err);
                                if (recognizerRef[0] != null) recognizerRef[0].destroy();
                                latch.countDown();
                            }
                            @Override
                            public void onResults(android.os.Bundle r) {
                                java.util.ArrayList<String> m = r.getStringArrayList(
                                        android.speech.SpeechRecognizer.RESULTS_RECOGNITION);
                                if (m != null && !m.isEmpty()) sttResult[0] = m.get(0);
                                if (recognizerRef[0] != null) recognizerRef[0].destroy();
                                latch.countDown();
                            }
                            @Override public void onPartialResults(android.os.Bundle p) {}
                            @Override public void onEvent(int et, android.os.Bundle p) {}
                        });
                        recognizer.startListening(sttIntent);
                    });
                    new Thread(() -> {
                        try { latch.await(timeoutSec, java.util.concurrent.TimeUnit.SECONDS); }
                        catch (Exception ignored) {}
                        final String recognized = sttResult[0].isEmpty() ? sttText : sttResult[0];
                        if (!sttResult[0].isEmpty()) addLog("STT: " + sttResult[0]);
                        runOnUiThread(() -> {
                            canvas.triggerPulse(sttNode.getId());
                            continueFlow(sttNode, sttChatId, sttUser, recognized);
                        });
                    }).start();
                    return;
                }
            }
            canvas.triggerPulse(node.getId());
            continueFlow(node, chatId, userName, text);
        });
    }
    private TelegramMethod findMethodDef(String name) {
        if (name == null) return null;
        for (TelegramMethod m : MethodRegistry.getAllMethods()) {
            if (m.apiName.equals(name)) return m;
        }
        return null;
    }

    private TelegramMethod findMethodByLabel(String label) {
        if (label == null) return null;
        for (TelegramMethod m : MethodRegistry.getAllMethods()) {
            if (m.displayName.equals(label)) return m;
        }
        return null;
    }

    private static String timestamp() {
        return new java.text.SimpleDateFormat(
                "HH:mm:ss", java.util.Locale.getDefault()).format(new java.util.Date());
    }

    private void addLog(String entry) {
        String ts = timestamp();
        synchronized (logLock) {
            logEntries.add("[" + ts + "] " + entry);
        }
        runOnUiThread(() -> {
            if (logAdapter != null) {
                logAdapter.notifyDataSetChanged();
            }
        });
    }

    private void showLogViewer() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(
                (int) (16 * getResources().getDisplayMetrics().density),
                (int) (16 * getResources().getDisplayMetrics().density),
                (int) (16 * getResources().getDisplayMetrics().density),
                (int) (16 * getResources().getDisplayMetrics().density)
        );

        TextView title = new TextView(this);
        title.setText("Log Viewer");
        title.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_TitleMedium);
        layout.addView(title);

        RecyclerView rvLog = new RecyclerView(this);
        rvLog.setLayoutManager(new LinearLayoutManager(this));
        logAdapter = new LogAdapter(logEntries);
        rvLog.setAdapter(logAdapter);
        rvLog.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (int) (300 * getResources().getDisplayMetrics().density)
        ));
        layout.addView(rvLog);

        if (!logEntries.isEmpty()) {
            rvLog.post(() -> rvLog.scrollToPosition(0));
        }

        MaterialButton copyBtn = new MaterialButton(this);
        copyBtn.setText("Salin Log");
        copyBtn.setOnClickListener(v -> {
            StringBuilder sb = new StringBuilder();
            for (int i = logEntries.size() - 1; i >= 0; i--) {
                sb.append(logEntries.get(i)).append('\n');
            }
            android.content.ClipboardManager cm =
                    (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            cm.setText(sb.toString().trim());
            Snackbar.make(canvas, "Log disalin ke clipboard", Snackbar.LENGTH_SHORT).show();
        });
        layout.addView(copyBtn);

        MaterialButton clearBtn = new MaterialButton(this);
        clearBtn.setText("Hapus Log");
        clearBtn.setOnClickListener(v -> {
            logEntries.clear();
            if (logAdapter != null) logAdapter.notifyDataSetChanged();
            Snackbar.make(canvas, "Log dihapus", Snackbar.LENGTH_SHORT).show();
        });
        layout.addView(clearBtn);

        dialog.setOnDismissListener(d -> logAdapter = null);
        dialog.setContentView(layout);
        dialog.show();
    }

    private class LogAdapter extends RecyclerView.Adapter<LogAdapter.ViewHolder> {
        private final ArrayList<String> entries;

        LogAdapter(ArrayList<String> entries) {
            this.entries = entries;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            TextView tv = new TextView(parent.getContext());
            tv.setPadding(0, (int) (4 * getResources().getDisplayMetrics().density), 0, 0);
            tv.setTypeface(android.graphics.Typeface.MONOSPACE);
            tv.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodySmall);
            tv.setLayoutParams(new RecyclerView.LayoutParams(
                    RecyclerView.LayoutParams.MATCH_PARENT,
                    RecyclerView.LayoutParams.WRAP_CONTENT));
            return new ViewHolder(tv);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            holder.textView.setText(entries.get(entries.size() - 1 - position));
        }

        @Override
        public int getItemCount() {
            return entries.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            final TextView textView;
            ViewHolder(TextView tv) {
                super(tv);
                textView = tv;
            }
        }
    }

    private void showSettingsDialog() {
        View view = getLayoutInflater().inflate(R.layout.dialog_settings, null);
        com.google.android.material.textfield.TextInputEditText etToken =
                view.findViewById(R.id.et_token);
        com.google.android.material.textfield.TextInputEditText etChatId =
                view.findViewById(R.id.et_chat_id);

        String savedToken = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .getString("bot_token", "");
        String savedChatId = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .getString("chat_id", "");
        etToken.setText(savedToken);
        etChatId.setText(savedChatId);

        new MaterialAlertDialogBuilder(this)
                .setTitle("Telegram Settings")
                .setView(view)
                .setPositiveButton("Simpan", (d, w) -> {
                    getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                            .edit()
                            .putString("bot_token", etToken.getText().toString())
                            .putString("chat_id", etChatId.getText().toString())
                            .apply();
                    Snackbar.make(canvas, "Settings disimpan", Snackbar.LENGTH_SHORT).show();
                })
                .setNegativeButton("Batal", null)
                .show();
    }

    private void showChatIdInput() {
        View view = getLayoutInflater().inflate(R.layout.dialog_chat_id, null);
        com.google.android.material.textfield.TextInputEditText et =
                view.findViewById(R.id.et_chat_id);
        et.setText(lastChatId);

        new MaterialAlertDialogBuilder(this)
                .setTitle("Set Last Chat ID")
                .setMessage("Chat ID untuk mode publik (kosongkan jika ingin diisi otomatis dari polling)")
                .setView(view)
                .setPositiveButton("Simpan", (d, w) -> {
                    lastChatId = et.getText().toString().trim();
                    getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                            .edit().putString("last_chat_id", lastChatId).apply();
                    Snackbar.make(canvas, "Chat ID disimpan", Snackbar.LENGTH_SHORT).show();
                })
                .setNegativeButton("Batal", null)
                .show();
    }

    private void showAiSettings() {
        AiProvider[] providers = AiProvider.getBuiltInProviders();
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 24, 48, 24);

        for (AiProvider p : providers) {
            if (!p.needsApiKey) continue;
            com.google.android.material.textfield.TextInputLayout til =
                    new com.google.android.material.textfield.TextInputLayout(this);
            til.setBoxBackgroundMode(TextInputLayout.BOX_BACKGROUND_OUTLINE);
            til.setHint(p.name + " API Key");
            til.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT));
            int m = (int) (12 * getResources().getDisplayMetrics().density);
            ((LinearLayout.LayoutParams) til.getLayoutParams()).setMargins(0, m, 0, 0);

            com.google.android.material.textfield.TextInputEditText et =
                    new com.google.android.material.textfield.TextInputEditText(this);
            String saved = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                    .getString("ai_key_" + p.id, "");
            et.setText(saved);
            et.setTag("key_" + p.id);
            til.addView(et);
            layout.addView(til);
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle("AI API Keys")
                .setView(layout)
                .setPositiveButton("Simpan", (d, w) -> {
                    for (int i = 0; i < layout.getChildCount(); i++) {
                        android.view.View child = layout.getChildAt(i);
                        if (child instanceof com.google.android.material.textfield.TextInputLayout) {
                            com.google.android.material.textfield.TextInputLayout til =
                                    (com.google.android.material.textfield.TextInputLayout) child;
                            com.google.android.material.textfield.TextInputEditText et =
                                    (com.google.android.material.textfield.TextInputEditText) til.getEditText();
                            if (et != null && et.getTag() instanceof String) {
                                String tag = (String) et.getTag();
                                if (tag.startsWith("key_")) {
                                    String providerId = tag.substring(4);
                                    String key = et.getText() != null ? et.getText().toString() : "";
                                    getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                                            .edit()
                                            .putString("ai_key_" + providerId, key)
                                            .apply();
                                }
                            }
                        }
                    }
                    Snackbar.make(canvas, "API Keys disimpan", Snackbar.LENGTH_SHORT).show();
                })
                .setNegativeButton("Batal", null)
                .show();
    }

    private void showNodeActions(FlowNode node) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(node.getLabel())
                .setItems(new CharSequence[]{"Edit Node", "Duplikat Node", "Hapus Node"}, (d, w) -> {
                    if (w == 0) {
                        openNodeEditor(node);
                    } else if (w == 1) {
                        FlowNode copy = node.duplicate(60f, 60f);
                        canvas.addNode(copy);
                        setDirty();
                        Snackbar.make(canvas, "Node diduplikasi", Snackbar.LENGTH_SHORT).show();
                    } else {
                        selectedNode = node;
                        canvas.removeSelectedNode();
                        selectedNode = null;
                        setDirty();
                        Snackbar.make(canvas, "Node dihapus", Snackbar.LENGTH_SHORT).show();
                    }
                })
                .show();
    }

    private void confirmDeleteAll() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Hapus Semua")
                .setMessage("Hapus semua node dan koneksi?")
                .setPositiveButton("Hapus", (d, w) -> {
                    canvas.clearAll();
                    setDirty();
                    Snackbar.make(canvas, "Semua dihapus", Snackbar.LENGTH_SHORT).show();
                })
                .setNegativeButton("Batal", null)
                .show();
    }

    private void showAboutDialog() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 24, 48, 24);

        TextView tvIcon = new TextView(this);
        tvIcon.setText("\uD83E\uDD16");
        tvIcon.setTextSize(48);
        tvIcon.setGravity(android.view.Gravity.CENTER);
        layout.addView(tvIcon);

        TextView tvTitle = new TextView(this);
        tvTitle.setText("TgFlowBot");
        tvTitle.setTextSize(22);
        tvTitle.setGravity(android.view.Gravity.CENTER);
        tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        layout.addView(tvTitle);

        String[] lines = {
                "Versi: 1.0.0",
                "",
                "Visual Telegram Bot Builder",
                "untuk Android",
                "",
                "Bangun bot Telegram secara visual",
                "dengan drag-and-drop node editor.",
                "Seperti n8n, tapi untuk Telegram bot,",
                "berjalan native di Android.",
                "",
                "122+ metode Telegram API built-in",
                "29 trigger, 150+ action, 25+ kondisi",
                "Dukungan AI: OpenAI, Claude, Gemini,",
                "Groq, dan llama.cpp",
                "",
                "Dibangun dengan Java & Material Design 3",
                "Target: Android 7.0+ (API 24)",
        };
        for (String line : lines) {
            TextView tv = new TextView(this);
            tv.setText(line);
            tv.setGravity(android.view.Gravity.CENTER);
            tv.setTextSize(14);
            layout.addView(tv);
        }

        new MaterialAlertDialogBuilder(this)
                .setView(layout)
                .setPositiveButton("Tutup", null)
                .show();
    }

    private void showReportBugDialog() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 24, 48, 24);

        TextView tvDesc = new TextView(this);
        tvDesc.setText("Laporkan bug atau minta fitur baru");
        tvDesc.setTextSize(16);
        tvDesc.setGravity(android.view.Gravity.CENTER);
        layout.addView(tvDesc);

        com.google.android.material.textfield.TextInputLayout tilTitle =
                new com.google.android.material.textfield.TextInputLayout(this);
        tilTitle.setBoxBackgroundMode(TextInputLayout.BOX_BACKGROUND_OUTLINE);
        tilTitle.setHint("Judul");
        com.google.android.material.textfield.TextInputEditText etTitle =
                new com.google.android.material.textfield.TextInputEditText(this);
        tilTitle.addView(etTitle);
        layout.addView(tilTitle);

        com.google.android.material.textfield.TextInputLayout tilDesc =
                new com.google.android.material.textfield.TextInputLayout(this);
        tilDesc.setBoxBackgroundMode(TextInputLayout.BOX_BACKGROUND_OUTLINE);
        tilDesc.setHint("Deskripsi detail");
        com.google.android.material.textfield.TextInputEditText etDesc =
                new com.google.android.material.textfield.TextInputEditText(this);
        etDesc.setMinLines(4);
        etDesc.setGravity(android.view.Gravity.TOP);
        tilDesc.addView(etDesc);
        layout.addView(tilDesc);

        new MaterialAlertDialogBuilder(this)
                .setTitle("Report Bug")
                .setView(layout)
                .setPositiveButton("Buka GitHub", (d, w) -> {
                    String title = etTitle.getText() != null ? etTitle.getText().toString().trim() : "";
                    String body = etDesc.getText() != null ? etDesc.getText().toString().trim() : "";
                    String url = "https://github.com/padz24/TgFlowBot/issues/new?title="
                            + android.net.Uri.encode(title)
                            + "&body=" + android.net.Uri.encode(body);
                    android.content.Intent intent = new android.content.Intent(
                            android.content.Intent.ACTION_VIEW,
                            android.net.Uri.parse(url));
                    startActivity(intent);
                })
                .setNegativeButton("Batal", null)
                .show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1001 && grantResults.length > 0
                && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            Snackbar.make(canvas, "Izin kamera diberikan, jalankan ulang node", Snackbar.LENGTH_SHORT).show();
        }
        if (requestCode == 1002 && grantResults.length > 0
                && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            Snackbar.make(canvas, "Izin mikrofon diberikan, jalankan ulang workflow", Snackbar.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onBackPressed() {
        saveWorkflow();
        super.onBackPressed();
    }

    private String getJsonString(com.google.gson.JsonElement el) {
        if (el == null) return null;
        if (el.isJsonNull()) return null;
        if (el.isJsonPrimitive()) return el.getAsString();
        return el.toString();
    }
}

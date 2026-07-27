package com.tgflowbot;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.tgflowbot.model.Workflow;
import com.tgflowbot.view.WorkflowAdapter;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class WorkflowListActivity extends AppCompatActivity {

    private RecyclerView rv;
    private TextView tvEmpty;
    private WorkflowAdapter adapter;
    private List<WorkflowAdapter.WorkflowItem> workflowItems;
    private static final String PREFS = "workflow_list";
    private static final String KEY_LIST = "workflow_items";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.AppTheme);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_workflow_list);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        }

        rv = findViewById(R.id.rv_workflows);
        tvEmpty = findViewById(R.id.tv_empty);
        MaterialButton btnNew = findViewById(R.id.btn_new_workflow);
        MaterialButton btnExt = findViewById(R.id.btn_extensions);

        workflowItems = loadWorkflowItems();
        refreshView();

        adapter = new WorkflowAdapter(workflowItems, new WorkflowAdapter.OnWorkflowListener() {
            @Override
            public void onWorkflowClick(WorkflowAdapter.WorkflowItem item) {
                openWorkflow(item);
            }

            @Override
            public void onWorkflowDelete(WorkflowAdapter.WorkflowItem item) {
                confirmDelete(item);
            }
        });

        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(adapter);

        btnNew.setOnClickListener(v -> showNewWorkflowDialog());
        btnExt.setOnClickListener(v -> {
            startActivity(new Intent(this, ExtensionManagerActivity.class));
        });
    }

    private void openWorkflow(WorkflowAdapter.WorkflowItem item) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("workflow_name", item.name);
        intent.putExtra("workflow_data", item.data);
        startActivity(intent);
    }

    private void showNewWorkflowDialog() {
        View view = getLayoutInflater().inflate(R.layout.dialog_workflow, null);
        com.google.android.material.textfield.TextInputEditText etName =
                view.findViewById(R.id.et_name);
        com.google.android.material.textfield.TextInputEditText etDesc =
                view.findViewById(R.id.et_desc);

        new MaterialAlertDialogBuilder(this)
                .setTitle("New Workflow")
                .setView(view)
                .setPositiveButton("Buat", (d, w) -> {
                    String name = etName.getText() != null ?
                            etName.getText().toString().trim() : "Untitled";
                    String desc = etDesc.getText() != null ?
                            etDesc.getText().toString().trim() : "";
                    if (name.isEmpty()) name = "Untitled";

                    WorkflowAdapter.WorkflowItem item =
                            new WorkflowAdapter.WorkflowItem(name, desc);
                    workflowItems.add(0, item);
                    saveWorkflowItems();
                    adapter.notifyItemInserted(0);
                    refreshView();
                    openWorkflow(item);
                })
                .setNegativeButton("Batal", null)
                .show();
    }

    private void confirmDelete(WorkflowAdapter.WorkflowItem item) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Hapus Workflow")
                .setMessage("Hapus \"" + item.name + "\"?")
                .setPositiveButton("Hapus", (d, w) -> {
                    int idx = workflowItems.indexOf(item);
                    if (idx >= 0) {
                        workflowItems.remove(idx);
                        adapter.notifyItemRemoved(idx);
                        saveWorkflowItems();
                        refreshView();
                    }
                })
                .setNegativeButton("Batal", null)
                .show();
    }

    private void refreshView() {
        boolean empty = workflowItems.isEmpty();
        rv.setVisibility(empty ? View.GONE : View.VISIBLE);
        tvEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
    }

    private List<WorkflowAdapter.WorkflowItem> loadWorkflowItems() {
        String json = getSharedPreferences(PREFS, MODE_PRIVATE)
                .getString(KEY_LIST, "[]");
        Type type = new TypeToken<List<WorkflowAdapter.WorkflowItem>>(){}.getType();
        List<WorkflowAdapter.WorkflowItem> list = new Gson().fromJson(json, type);
        return list != null ? list : new ArrayList<>();
    }

    private void saveWorkflowItems() {
        getSharedPreferences(PREFS, MODE_PRIVATE)
                .edit()
                .putString(KEY_LIST, new Gson().toJson(workflowItems))
                .apply();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}

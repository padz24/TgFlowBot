package com.tgflowbot;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.gson.Gson;
import com.tgflowbot.telegram.MethodRegistry;
import com.tgflowbot.telegram.ParamDef;
import com.tgflowbot.telegram.TelegramMethod;

import java.util.HashMap;
import java.util.Map;

public class NodeEditorActivity extends AppCompatActivity {

    private String nodeId;
    private String nodeLabel;
    private TelegramMethod method;
    private TextInputEditText etLabel;
    private LinearLayout propertiesContainer;
    private TextInputEditText lastFocusedEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_node_editor);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        nodeId = getIntent().getStringExtra("node_id");
        nodeLabel = getIntent().getStringExtra("node_label");
        String methodName = getIntent().getStringExtra("method_name");
        String propertiesJson = getIntent().getStringExtra("node_properties");

        etLabel = findViewById(R.id.et_label);
        etLabel.setText(nodeLabel);
        etLabel.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) lastFocusedEditText = (TextInputEditText) v;
        });

        propertiesContainer = findViewById(R.id.properties_container);

        method = findMethod(methodName);

        Map<String, String> properties = new HashMap<>();
        if (propertiesJson != null) {
            Gson gson = new Gson();
            java.lang.reflect.Type type =
                    new com.google.gson.reflect.TypeToken<Map<String, String>>(){}.getType();
            Map<String, String> parsed = gson.fromJson(propertiesJson, type);
            if (parsed != null) properties.putAll(parsed);
        }

        renderParams(properties);
        updateTitle();
        buildVariableChips();

        MaterialButton btnSave = findViewById(R.id.btn_save);
        btnSave.setOnClickListener(v -> saveAndExit(false));

        MaterialButton btnDelete = findViewById(R.id.btn_delete);
        btnDelete.setOnClickListener(v -> saveAndExit(true));
    }

    private void buildVariableChips() {
        LinearLayout chipsContainer = findViewById(R.id.variable_chips_container);
        if (chipsContainer == null) return;

        String[][] variables = {
            {"{{text}}", "Message text"},
            {"{{chatId}}", "Chat ID"},
            {"{{username}}", "Sender username"},
            {"{{message_id}}", "Message ID"},
            {"{{result}}", "Last node output (JSON)"},
            {"{{result.message_id}}", "Sent message ID"},
            {"{{result.chat.id}}", "Result chat ID"},
            {"{{result.from.id}}", "Sender user ID"},
            {"{{error}}", "Last error message"},
            {"{{$name}}", "Your variable (replace name)"},
        };

        for (String[] var : variables) {
            com.google.android.material.chip.Chip chip = new com.google.android.material.chip.Chip(this);
            chip.setText(var[0]);
            chip.setClickable(true);
            chip.setCheckable(false);
            chip.setTag(var[0]);

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            int m = (int) (4 * getResources().getDisplayMetrics().density);
            lp.setMargins(0, 0, m, 0);
            chip.setLayoutParams(lp);

            chip.setOnClickListener(v -> insertPlaceholder((String) v.getTag()));

            chip.setOnLongClickListener(v -> {
                android.content.ClipData data = android.content.ClipData.newPlainText("variable", (String) v.getTag());
                android.view.View.DragShadowBuilder shadow = new android.view.View.DragShadowBuilder(v);
                v.startDragAndDrop(data, shadow, (String) v.getTag(), 0);
                return true;
            });

            chipsContainer.addView(chip);
        }
    }

    private void insertPlaceholder(String placeholder) {
        TextInputEditText target = lastFocusedEditText;
        if (target == null) {
            if (propertiesContainer.getChildCount() > 0) {
                for (int i = 0; i < propertiesContainer.getChildCount(); i++) {
                    android.view.View child = propertiesContainer.getChildAt(i);
                    if (child instanceof TextInputLayout) {
                        TextInputEditText et = (TextInputEditText) ((TextInputLayout) child).getEditText();
                        if (et != null) { target = et; break; }
                    }
                }
            }
            if (target == null) return;
        }
        int pos = target.getSelectionStart();
        if (pos < 0) pos = target.getText() != null ? target.getText().length() : 0;
        String current = target.getText() != null ? target.getText().toString() : "";
        String before = current.substring(0, pos);
        String after = current.substring(pos);
        target.setText(before + placeholder + after);
        target.setSelection(pos + placeholder.length());
        target.requestFocus();
    }

    private void setupDragAndDropForEditText(TextInputEditText et) {
        et.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) lastFocusedEditText = (TextInputEditText) v;
        });
        et.setOnDragListener((v, event) -> {
            switch (event.getAction()) {
                case android.view.DragEvent.ACTION_DRAG_STARTED:
                    return true;
                case android.view.DragEvent.ACTION_DRAG_ENTERED:
                    v.setAlpha(0.6f);
                    return true;
                case android.view.DragEvent.ACTION_DRAG_EXITED:
                    v.setAlpha(1.0f);
                    return true;
                case android.view.DragEvent.ACTION_DROP: {
                    android.content.ClipData clipData = event.getClipData();
                    if (clipData != null && clipData.getItemCount() > 0) {
                        String placeholder = clipData.getItemAt(0).getText().toString();
                        TextInputEditText editText = (TextInputEditText) v;
                        int pos = editText.getSelectionStart();
                        if (pos < 0) pos = editText.getText() != null ? editText.getText().length() : 0;
                        String current = editText.getText() != null ? editText.getText().toString() : "";
                        String before = current.substring(0, pos);
                        String after = current.substring(pos);
                        editText.setText(before + placeholder + after);
                        editText.setSelection(pos + placeholder.length());
                    }
                    v.setAlpha(1.0f);
                    return true;
                }
                case android.view.DragEvent.ACTION_DRAG_ENDED:
                    v.setAlpha(1.0f);
                    return true;
            }
            return false;
        });
    }

    private TelegramMethod findMethod(String name) {
        if (name == null) return null;
        for (TelegramMethod m : MethodRegistry.getAllMethods()) {
            if (m.apiName.equals(name)) return m;
        }
        return null;
    }

    private void updateTitle() {
        if (getSupportActionBar() != null && method != null) {
            getSupportActionBar().setTitle(method.displayName);
        }
    }

    private void renderParams(Map<String, String> saved) {
        propertiesContainer.removeAllViews();

        if (method == null) return;

        boolean hasAny = false;
        for (ParamDef param : method.params) {
            if (param.name.equals("chat_id")) continue;
            hasAny = true;
            String key = param.name;
            String value = saved.containsKey(key) ? saved.get(key) : (param.defaultValue != null ? param.defaultValue : "");

            if (param.type == ParamDef.ParamType.BOOLEAN) {
                MaterialCheckBox cb = new MaterialCheckBox(this);
                cb.setText(key.replace("_", " ") + (param.required ? " *" : ""));
                cb.setChecked("true".equals(value));
                cb.setTag("key_" + key);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
                int m = (int) (8 * getResources().getDisplayMetrics().density);
                lp.setMargins(0, m, 0, 0);
                cb.setLayoutParams(lp);
                propertiesContainer.addView(cb);
            } else {
                TextInputLayout til = new TextInputLayout(this);
                til.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT));
                til.setBoxBackgroundMode(TextInputLayout.BOX_BACKGROUND_OUTLINE);
                til.setHint(param.hint + (param.required ? " *" : " (opsional)"));
                til.setTag("key_" + key);
                til.setHelperText(param.type.name().toLowerCase() + " — drag or tap variable chips below");

                TextInputEditText et = new TextInputEditText(this);
                et.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT));
                et.setText(value);
                et.setTag("prop_" + key);
                if (param.type == ParamDef.ParamType.INTEGER) {
                    et.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
                } else if (param.type == ParamDef.ParamType.FLOAT) {
                    et.setInputType(android.text.InputType.TYPE_CLASS_NUMBER |
                            android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
                }

                setupDragAndDropForEditText(et);
                til.addView(et);

                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
                int m = (int) (8 * getResources().getDisplayMetrics().density);
                lp.setMargins(0, m, 0, 0);
                til.setLayoutParams(lp);

                propertiesContainer.addView(til);
            }
        }

        if (!hasAny) {
            android.widget.TextView tv = new android.widget.TextView(this);
            tv.setText("Tidak ada parameter");
            tv.setPadding(0, (int) (16 * getResources().getDisplayMetrics().density), 0, 0);
            propertiesContainer.addView(tv);
        }
    }

    private void saveAndExit(boolean delete) {
        Map<String, String> properties = new HashMap<>();
        for (int i = 0; i < propertiesContainer.getChildCount(); i++) {
            android.view.View child = propertiesContainer.getChildAt(i);
            String key = null;
            String value = null;

            if (child instanceof TextInputLayout) {
                TextInputLayout til = (TextInputLayout) child;
                String tag = (String) til.getTag();
                if (tag != null && tag.startsWith("key_")) {
                    key = tag.substring(4);
                    TextInputEditText et = (TextInputEditText) til.getEditText();
                    if (et != null && et.getText() != null) {
                        value = et.getText().toString();
                    }
                }
            } else if (child instanceof MaterialCheckBox) {
                MaterialCheckBox cb = (MaterialCheckBox) child;
                String tag = (String) cb.getTag();
                if (tag != null && tag.startsWith("key_")) {
                    key = tag.substring(4);
                    value = cb.isChecked() ? "true" : "false";
                }
            }

            if (key != null && value != null) {
                properties.put(key, value);
            }
        }

        Intent result = new Intent();
        result.putExtra("node_id", nodeId);
        result.putExtra("node_label", etLabel.getText() != null ?
                etLabel.getText().toString() : "");
        result.putExtra("node_properties", new Gson().toJson(properties));
        result.putExtra("delete", delete);
        setResult(RESULT_OK, result);
        finish();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            saveAndExit(false);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}

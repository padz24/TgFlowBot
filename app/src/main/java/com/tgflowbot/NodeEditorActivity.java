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

        MaterialButton btnSave = findViewById(R.id.btn_save);
        btnSave.setOnClickListener(v -> saveAndExit(false));

        MaterialButton btnDelete = findViewById(R.id.btn_delete);
        btnDelete.setOnClickListener(v -> saveAndExit(true));
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
                til.setHelperText(param.type.name().toLowerCase());

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

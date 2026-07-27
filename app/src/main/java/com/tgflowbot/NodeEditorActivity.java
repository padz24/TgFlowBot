package com.tgflowbot;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.LinearLayout;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
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
    private String pendingPickKey;
    private TextInputEditText hiddenUriField;
    private android.widget.TextView selectedFileNameView;

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

    private final ActivityResultLauncher<Intent> filePickerLauncher =
        registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                android.net.Uri uri = result.getData().getData();
                if (uri != null && pendingPickKey != null) {
                    try {
                        getContentResolver().takePersistableUriPermission(uri,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    } catch (Exception ignored) {}
                    if (hiddenUriField != null) {
                        hiddenUriField.setText(uri.toString());
                    }
                    if (selectedFileNameView != null) {
                        selectedFileNameView.setText(getDisplayName(uri));
                    }
                    pendingPickKey = null;
                }
            }
        });

    private String getDisplayName(android.net.Uri uri) {
        String name = null;
        if ("content".equals(uri.getScheme())) {
            try (android.database.Cursor c = getContentResolver().query(uri, null, null, null, null)) {
                if (c != null && c.moveToFirst()) {
                    int idx = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                    if (idx >= 0) name = c.getString(idx);
                }
            } catch (Exception ignored) {}
        }
        if (name == null) name = uri.getLastPathSegment();
        return name != null ? name : "file";
    }

    private String getMimeTypeForMethod(String apiName) {
        switch (apiName) {
            case "sendPhoto": return "image/*";
            case "sendVideo": return "video/*";
            case "sendAudio": return "audio/*";
            case "sendVoice": return "audio/*";
            case "sendVideoNote": return "video/*";
            case "sendAnimation": return "image/gif";
            case "sendSticker": return "image/webp";
            case "setChatPhoto": return "image/*";
            case "setStickerSetThumbnail": return "image/*";
            case "uploadStickerFile": return "image/*";
            default: return "*/*";
        }
    }

    private void renderParams(Map<String, String> saved) {
        propertiesContainer.removeAllViews();

        if (method == null) return;

        String inputTypeDefault = null;
        String mediaFieldKey = null;
        boolean hasInputType = false;
        java.util.List<ParamDef> pList = method.params;
        for (int i = 0; i < pList.size(); i++) {
            if (pList.get(i).name.equals("input_type")) {
                hasInputType = true;
                inputTypeDefault = pList.get(i).defaultValue;
                if (i + 1 < pList.size()) mediaFieldKey = pList.get(i + 1).name;
                break;
            }
        }

        boolean hasAny = false;
        int margin = (int) (8 * getResources().getDisplayMetrics().density);

        for (ParamDef param : pList) {
            if (param.name.equals("chat_id")) continue;
            hasAny = true;
            String key = param.name;
            String value = saved.containsKey(key) ? saved.get(key)
                    : (param.defaultValue != null ? param.defaultValue : "");

            if (param.type == ParamDef.ParamType.BOOLEAN) {
                MaterialCheckBox cb = new MaterialCheckBox(this);
                cb.setText(key.replace("_", " ") + (param.required ? " *" : ""));
                cb.setChecked("true".equals(value));
                cb.setTag("key_" + key);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
                lp.setMargins(0, margin, 0, 0);
                cb.setLayoutParams(lp);
                propertiesContainer.addView(cb);
            } else if (hasInputType && key.equals("input_type")) {
                TextInputLayout til = new TextInputLayout(this);
                til.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT));
                til.setBoxBackgroundMode(TextInputLayout.BOX_BACKGROUND_OUTLINE);
                til.setHint(param.hint + (param.required ? " *" : ""));
                til.setTag("key_" + key);
                til.setEndIconMode(TextInputLayout.END_ICON_DROPDOWN_MENU);
                til.setHelperText("Pilih sumber media");

                com.google.android.material.textfield.MaterialAutoCompleteTextView actv =
                        new com.google.android.material.textfield.MaterialAutoCompleteTextView(this);
                actv.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT));
                actv.setInputType(android.text.InputType.TYPE_NULL);
                actv.setText(value, false);
                actv.setTag("prop_" + key);
                String[] options = {"url", "upload"};
                actv.setAdapter(new android.widget.ArrayAdapter<>(this,
                        android.R.layout.simple_dropdown_item_1line, options));
                til.addView(actv);

                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
                lp.setMargins(0, margin, 0, 0);
                til.setLayoutParams(lp);
                propertiesContainer.addView(til);

                if (mediaFieldKey != null) {
                    actv.setOnItemClickListener((parent, view, pos, id) -> {
                        String sel = (String) parent.getItemAtPosition(pos);
                        setMediaFieldVisibility(mediaFieldKey, "upload".equals(sel));
                    });
                }
            } else if (hasInputType && mediaFieldKey != null && key.equals(mediaFieldKey)) {
                TextInputLayout urlTil = new TextInputLayout(this);
                urlTil.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT));
                urlTil.setBoxBackgroundMode(TextInputLayout.BOX_BACKGROUND_OUTLINE);
                urlTil.setHint(param.hint + (param.required ? " *" : " (opsional)"));
                urlTil.setTag("media_field_" + key);
                urlTil.setHelperText(param.type.name().toLowerCase() + " — drag or tap variable chips below");

                TextInputEditText urlEt = new TextInputEditText(this);
                urlEt.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT));
                urlEt.setText(value);
                urlEt.setTag("prop_" + key);
                setupDragAndDropForEditText(urlEt);
                urlTil.addView(urlEt);
                LinearLayout.LayoutParams urlLp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
                urlLp.setMargins(0, margin, 0, 0);
                urlTil.setLayoutParams(urlLp);
                propertiesContainer.addView(urlTil);

                LinearLayout pickerLayout = new LinearLayout(this);
                pickerLayout.setOrientation(LinearLayout.HORIZONTAL);
                pickerLayout.setTag("picker_" + key);
                pickerLayout.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT));
                ((LinearLayout.LayoutParams) pickerLayout.getLayoutParams()).setMargins(0, margin, 0, 0);
                pickerLayout.setGravity(android.view.Gravity.CENTER_VERTICAL);

                TextInputEditText hiddenEt = new TextInputEditText(this);
                hiddenEt.setLayoutParams(new LinearLayout.LayoutParams(0, 0));
                hiddenEt.setText(value);
                hiddenEt.setTag("prop_" + key);
                pickerLayout.addView(hiddenEt);

                selectedFileNameView = new android.widget.TextView(this);
                selectedFileNameView.setLayoutParams(new LinearLayout.LayoutParams(0,
                        LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
                selectedFileNameView.setPadding((int) (12 * getResources().getDisplayMetrics().density), 0, 0, 0);
                selectedFileNameView.setEllipsize(android.text.TextUtils.TruncateAt.MIDDLE);
                selectedFileNameView.setSingleLine(true);
                selectedFileNameView.setText(value != null && !value.isEmpty() ? getDisplayName(android.net.Uri.parse(value)) : "");
                pickerLayout.addView(selectedFileNameView);

                MaterialButton pickBtn = new MaterialButton(this);
                pickBtn.setText("Pilih File");
                pickBtn.setOnClickListener(v -> {
                    pendingPickKey = key;
                    hiddenUriField = hiddenEt;
                    String mime = getMimeTypeForMethod(method.apiName);
                    Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    intent.setType(mime);
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                            | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
                    filePickerLauncher.launch(intent);
                });
                pickerLayout.addView(pickBtn);

                propertiesContainer.addView(pickerLayout);
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
                lp.setMargins(0, margin, 0, 0);
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

        if (mediaFieldKey != null) {
            String inputVal = saved.containsKey("input_type") ? saved.get("input_type")
                    : (inputTypeDefault != null ? inputTypeDefault : "url");
            setMediaFieldVisibility(mediaFieldKey, "upload".equals(inputVal));
        }
    }

    private void setMediaFieldVisibility(String mediaFieldKey, boolean isUpload) {
        for (int i = 0; i < propertiesContainer.getChildCount(); i++) {
            android.view.View child = propertiesContainer.getChildAt(i);
            Object tag = child.getTag();
            if (tag instanceof String) {
                String t = (String) tag;
                if (t.equals("media_field_" + mediaFieldKey)) {
                    child.setVisibility(isUpload ? android.view.View.GONE : android.view.View.VISIBLE);
                } else if (t.equals("picker_" + mediaFieldKey)) {
                    child.setVisibility(isUpload ? android.view.View.VISIBLE : android.view.View.GONE);
                }
            }
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
                if (tag != null && (tag.startsWith("key_") || tag.startsWith("media_field_"))) {
                    key = tag.substring(tag.startsWith("media_field_") ? 12 : 4);
                    android.widget.EditText et = til.getEditText();
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
            } else if (child instanceof LinearLayout) {
                String tag = (String) child.getTag();
                if (tag != null && tag.startsWith("picker_")) {
                    key = tag.substring(7);
                    TextInputEditText hiddenEt = null;
                    for (int j = 0; j < ((LinearLayout) child).getChildCount(); j++) {
                        android.view.View inner = ((LinearLayout) child).getChildAt(j);
                        if (inner instanceof TextInputEditText) {
                            hiddenEt = (TextInputEditText) inner;
                            break;
                        }
                    }
                    if (hiddenEt != null && hiddenEt.getText() != null) {
                        value = hiddenEt.getText().toString();
                    }
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

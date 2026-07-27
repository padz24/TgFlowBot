package com.tgflowbot.model;

import com.tgflowbot.R;

public enum NodeType {
    TRIGGER(R.string.node_trigger, R.drawable.ic_trigger, R.drawable.node_bg_trigger),
    ACTION(R.string.node_action, R.drawable.ic_action, R.drawable.node_bg_action),
    CONDITION(R.string.node_condition, R.drawable.ic_condition, R.drawable.node_bg_condition),
    OUTPUT(R.string.node_output, R.drawable.ic_output, R.drawable.node_bg_output);

    private final int labelResId;
    private final int iconResId;
    private final int backgroundResId;

    NodeType(int labelResId, int iconResId, int backgroundResId) {
        this.labelResId = labelResId;
        this.iconResId = iconResId;
        this.backgroundResId = backgroundResId;
    }

    public int getLabelResId() { return labelResId; }
    public int getIconResId() { return iconResId; }
    public int getBackgroundResId() { return backgroundResId; }
}

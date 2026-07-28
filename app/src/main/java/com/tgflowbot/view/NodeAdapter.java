package com.tgflowbot.view;

import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.tgflowbot.R;
import com.tgflowbot.model.NodeType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NodeAdapter extends RecyclerView.Adapter<NodeAdapter.ViewHolder> {

    public interface OnNodeClickListener {
        void onNodeClick(NodeItem item);
    }

    public interface OnNodeDragListener {
        void onNodeDragStart(NodeItem item, View view);
    }

    public static class NodeItem {
        public final String name;
        public final String description;
        public final NodeType type;
        public final String methodName;
        public final String subcategory;

        public NodeItem(String name, String description, NodeType type) {
            this(name, description, type, null, null);
        }

        public NodeItem(String name, String description, NodeType type, String methodName) {
            this(name, description, type, methodName, null);
        }

        public NodeItem(String name, String description, NodeType type, String methodName, String subcategory) {
            this.name = name;
            this.description = description;
            this.type = type;
            this.methodName = methodName;
            this.subcategory = subcategory;
        }
    }

    private static final Map<String, int[]> SUBCAT_STYLES = new HashMap<>();
    static {
        SUBCAT_STYLES.put("tg",    new int[]{0xFF64B5F6, R.drawable.ic_action});
        SUBCAT_STYLES.put("ai",    new int[]{0xFFCE93D8, R.drawable.ic_ai});
        SUBCAT_STYLES.put("math",  new int[]{0xFF4DB6AC, R.drawable.ic_math});
        SUBCAT_STYLES.put("text",  new int[]{0xFF4DD0E1, R.drawable.ic_text});
        SUBCAT_STYLES.put("vars",  new int[]{0xFFFFD54F, R.drawable.ic_var});
        SUBCAT_STYLES.put("flow",  new int[]{0xFFFF8A65, R.drawable.ic_flow});
        SUBCAT_STYLES.put("phone", new int[]{0xFF81C784, R.drawable.ic_phone});
        SUBCAT_STYLES.put("list",  new int[]{0xFF7986CB, R.drawable.ic_list});
        SUBCAT_STYLES.put("op",    new int[]{0xFF90A4AE, R.drawable.ic_ops});
        SUBCAT_STYLES.put("file",  new int[]{0xFFA1887F, R.drawable.ic_file});
        SUBCAT_STYLES.put("http",  new int[]{0xFFE57373, R.drawable.ic_http});
        SUBCAT_STYLES.put("data",  new int[]{0xFF64B5F6, R.drawable.ic_data});
    }

    private static final Map<NodeType, int[]> TYPE_STYLES = new HashMap<>();
    static {
        TYPE_STYLES.put(NodeType.TRIGGER,  new int[]{0xFFFFB74D, R.drawable.ic_trigger});
        TYPE_STYLES.put(NodeType.ACTION,   new int[]{0xFF64B5F6, R.drawable.ic_action});
        TYPE_STYLES.put(NodeType.CONDITION,new int[]{0xFF81C784, R.drawable.ic_condition});
        TYPE_STYLES.put(NodeType.OUTPUT,   new int[]{0xFFCE93D8, R.drawable.ic_output});
    }

    private final List<NodeItem> items;
    private final OnNodeClickListener clickListener;
    private OnNodeDragListener dragListener;

    public NodeAdapter(List<NodeItem> items, OnNodeClickListener listener) {
        this.items = items;
        this.clickListener = listener;
    }

    public void setOnNodeDragListener(OnNodeDragListener listener) {
        this.dragListener = listener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.node_item, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        NodeItem item = items.get(position);
        holder.name.setText(item.name);
        holder.description.setText(item.description);

        int[] style = null;
        if (item.type == NodeType.ACTION && item.subcategory != null) {
            style = SUBCAT_STYLES.get(item.subcategory);
        }
        if (style == null) {
            style = TYPE_STYLES.get(item.type);
            if (style == null) style = new int[]{0xFF1976D2, R.drawable.ic_action};
        }

        int accentColor = style[0];
        int iconRes = style[1];

        holder.accentBar.setBackgroundColor(accentColor);
        holder.icon.setImageResource(iconRes);
        holder.icon.setColorFilter(accentColor);
        Drawable bg = holder.icon.getBackground();
        if (bg instanceof GradientDrawable) {
            ((GradientDrawable) bg).setColor(accentColor & 0x20FFFFFF);
            ((GradientDrawable) bg).setStroke(1, accentColor & 0x40FFFFFF);
        }

        holder.itemView.setOnClickListener(v -> clickListener.onNodeClick(item));
        holder.itemView.setOnLongClickListener(v -> {
            if (dragListener != null) {
                dragListener.onNodeDragStart(item, holder.itemView);
            }
            return true;
        });
    }

    @Override
    public int getItemCount() { return items.size(); }

    public List<NodeItem> getItems() { return items; }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView name;
        final TextView description;
        final ImageView icon;
        final View accentBar;

        ViewHolder(View v) {
            super(v);
            name = v.findViewById(R.id.tv_name);
            description = v.findViewById(R.id.tv_description);
            icon = v.findViewById(R.id.iv_icon);
            accentBar = v.findViewById(R.id.accent_bar);
        }
    }
}

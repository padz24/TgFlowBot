package com.tgflowbot.view;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.tgflowbot.R;
import com.tgflowbot.model.NodeType;

import java.util.List;

public class NodeAdapter extends RecyclerView.Adapter<NodeAdapter.ViewHolder> {

    public static class NodeItem {
        public final String name;
        public final String description;
        public final NodeType type;
        public final String methodName;

        public NodeItem(String name, String description, NodeType type) {
            this(name, description, type, null);
        }

        public NodeItem(String name, String description, NodeType type, String methodName) {
            this.name = name;
            this.description = description;
            this.type = type;
            this.methodName = methodName;
        }
    }

    public interface OnNodeClickListener {
        void onNodeClick(NodeItem item);
    }

    public interface OnNodeDragListener {
        void onNodeDragStart(NodeItem item, View view);
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
        holder.icon.setImageResource(item.type.getIconResId());

        int accentColor;
        switch (item.type) {
            case TRIGGER: accentColor = 0xFFFF9800; break;
            case ACTION: accentColor = 0xFF2196F3; break;
            case CONDITION: accentColor = 0xFF4CAF50; break;
            case OUTPUT: accentColor = 0xFF9C27B0; break;
            default: accentColor = 0xFF1976D2;
        }
        holder.accentBar.setBackgroundColor(accentColor);

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

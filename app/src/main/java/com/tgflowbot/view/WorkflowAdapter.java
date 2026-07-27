package com.tgflowbot.view;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.tgflowbot.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class WorkflowAdapter extends RecyclerView.Adapter<WorkflowAdapter.ViewHolder> {

    public static class WorkflowItem {
        public String name;
        public String description;
        public String data;
        public long createdAt;

        public WorkflowItem() {}

        public WorkflowItem(String name, String description) {
            this.name = name;
            this.description = description;
            this.data = "{}";
            this.createdAt = System.currentTimeMillis();
        }
    }

    public interface OnWorkflowListener {
        void onWorkflowClick(WorkflowItem item);
        void onWorkflowDelete(WorkflowItem item);
    }

    private final List<WorkflowItem> items;
    private final OnWorkflowListener listener;

    public WorkflowAdapter(List<WorkflowItem> items, OnWorkflowListener listener) {
        this.items = items;
        this.listener = listener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_workflow, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        WorkflowItem item = items.get(position);
        holder.tvName.setText(item.name);
        holder.tvDesc.setText(item.description != null && !item.description.isEmpty()
                ? item.description : "Tidak ada deskripsi");
        holder.tvInfo.setText(formatDate(item.createdAt));
        holder.itemView.setOnClickListener(v -> listener.onWorkflowClick(item));
        holder.ivDelete.setOnClickListener(v -> listener.onWorkflowDelete(item));
    }

    @Override
    public int getItemCount() { return items.size(); }

    private String formatDate(long millis) {
        return new SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault())
                .format(new Date(millis));
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView tvName, tvDesc, tvInfo;
        final ImageView ivDelete;

        ViewHolder(View v) {
            super(v);
            tvName = v.findViewById(R.id.tv_name);
            tvDesc = v.findViewById(R.id.tv_desc);
            tvInfo = v.findViewById(R.id.tv_info);
            ivDelete = v.findViewById(R.id.iv_delete);
        }
    }
}

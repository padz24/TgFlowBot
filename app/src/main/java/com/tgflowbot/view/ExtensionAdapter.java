package com.tgflowbot.view;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.tgflowbot.R;
import com.tgflowbot.telegram.ExtensionModule;

import java.util.List;
import java.util.Set;

public class ExtensionAdapter extends RecyclerView.Adapter<ExtensionAdapter.ViewHolder> {

    public interface OnExtensionActionListener {
        void onInstall(ExtensionModule item);
        void onRemove(ExtensionModule item);
    }

    private final List<ExtensionModule> items;
    private final OnExtensionActionListener listener;
    private final Set<String> installedPackageIds;

    public ExtensionAdapter(List<ExtensionModule> items,
                            Set<String> installedPackageIds,
                            OnExtensionActionListener listener) {
        this.items = items;
        this.installedPackageIds = installedPackageIds;
        this.listener = listener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_extension, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        ExtensionModule ext = items.get(position);
        holder.tvName.setText(ext.name);
        holder.tvCategory.setText(ext.category);
        holder.tvDescription.setText(ext.description);
        holder.tvMethods.setText(ext.methods.size() + " method(s)");

        boolean installed = installedPackageIds.contains(ext.packageId);
        if (installed) {
            holder.btnInstall.setVisibility(View.GONE);
            holder.btnRemove.setVisibility(View.VISIBLE);
        } else {
            holder.btnInstall.setVisibility(View.VISIBLE);
            holder.btnRemove.setVisibility(View.GONE);
        }

        holder.btnInstall.setOnClickListener(v -> listener.onInstall(ext));
        holder.btnRemove.setOnClickListener(v -> listener.onRemove(ext));
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView tvName, tvCategory, tvDescription, tvMethods;
        final MaterialButton btnInstall, btnRemove;

        ViewHolder(View v) {
            super(v);
            tvName = v.findViewById(R.id.tv_ext_name);
            tvCategory = v.findViewById(R.id.tv_ext_category);
            tvDescription = v.findViewById(R.id.tv_ext_description);
            tvMethods = v.findViewById(R.id.tv_ext_methods);
            btnInstall = v.findViewById(R.id.btn_install);
            btnRemove = v.findViewById(R.id.btn_remove);
        }
    }
}

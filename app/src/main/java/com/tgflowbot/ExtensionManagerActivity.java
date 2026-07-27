package com.tgflowbot;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.snackbar.Snackbar;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.tgflowbot.telegram.ExtensionModule;
import com.tgflowbot.telegram.MethodRegistry;
import com.tgflowbot.view.ExtensionAdapter;

import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ExtensionManagerActivity extends AppCompatActivity {

    private RecyclerView rv;
    private TextView tvEmpty;
    private ExtensionAdapter adapter;
    private List<ExtensionModule> marketplaceItems;
    private Set<String> installedPackageIds;
    private static final String PREFS = "extensions";
    private static final String KEY_EXTS = "installed_packages";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.AppTheme);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_extension_manager);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Extension Marketplace");
        }

        rv = findViewById(R.id.rv_extensions);
        tvEmpty = findViewById(R.id.tv_empty);

        marketplaceItems = ExtensionModule.getMarketplaceExtensions();
        installedPackageIds = loadInstalledPackageIds();

        for (ExtensionModule ext : marketplaceItems) {
            if (installedPackageIds.contains(ext.packageId)) {
                MethodRegistry.registerExtension(ext);
            }
        }

        refreshEmptyState();

        adapter = new ExtensionAdapter(marketplaceItems, installedPackageIds,
                new ExtensionAdapter.OnExtensionActionListener() {
            @Override
            public void onInstall(ExtensionModule item) {
                installExtension(item);
            }

            @Override
            public void onRemove(ExtensionModule item) {
                removeExtension(item);
            }
        });

        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(adapter);
    }

    private void installExtension(ExtensionModule ext) {
        installedPackageIds.add(ext.packageId);
        saveInstalledPackageIds();
        MethodRegistry.clearExtensions();
        for (ExtensionModule e : marketplaceItems) {
            if (installedPackageIds.contains(e.packageId)) {
                MethodRegistry.registerExtension(e);
            }
        }
        adapter.notifyDataSetChanged();
        refreshEmptyState();
        Snackbar.make(rv, ext.name + " berhasil diinstall", Snackbar.LENGTH_SHORT).show();
    }

    private void removeExtension(ExtensionModule ext) {
        installedPackageIds.remove(ext.packageId);
        saveInstalledPackageIds();
        MethodRegistry.clearExtensions();
        for (ExtensionModule e : marketplaceItems) {
            if (installedPackageIds.contains(e.packageId)) {
                MethodRegistry.registerExtension(e);
            }
        }
        adapter.notifyDataSetChanged();
        refreshEmptyState();
        Snackbar.make(rv, ext.name + " dihapus", Snackbar.LENGTH_SHORT).show();
    }

    private Set<String> loadInstalledPackageIds() {
        String json = getSharedPreferences(PREFS, MODE_PRIVATE)
                .getString(KEY_EXTS, "[]");
        Type type = new TypeToken<Set<String>>(){}.getType();
        Set<String> set = new Gson().fromJson(json, type);
        if (set == null) set = new HashSet<>();
        return set;
    }

    private void saveInstalledPackageIds() {
        getSharedPreferences(PREFS, MODE_PRIVATE)
                .edit()
                .putString(KEY_EXTS, new Gson().toJson(installedPackageIds))
                .apply();
    }

    private void refreshEmptyState() {
        boolean allInstalled = installedPackageIds.size() >= marketplaceItems.size();
        tvEmpty.setText(allInstalled
                ? "Semua extension sudah terinstall"
                : "Pilih extension di atas untuk diinstall");
        tvEmpty.setVisibility(View.GONE);
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

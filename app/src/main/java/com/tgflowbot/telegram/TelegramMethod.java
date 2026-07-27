package com.tgflowbot.telegram;

import com.tgflowbot.model.NodeType;

import java.util.ArrayList;
import java.util.List;

public class TelegramMethod {
    public final String apiName;
    public final String displayName;
    public final String description;
    public final NodeType nodeType;
    public final List<ParamDef> params;

    public TelegramMethod(String apiName, String displayName, String description, NodeType nodeType) {
        this.apiName = apiName;
        this.displayName = displayName;
        this.description = description;
        this.nodeType = nodeType;
        this.params = new ArrayList<>();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TelegramMethod that = (TelegramMethod) o;
        return apiName != null ? apiName.equals(that.apiName) : that.apiName == null;
    }

    @Override
    public int hashCode() {
        return apiName != null ? apiName.hashCode() : 0;
    }

    public TelegramMethod addParam(ParamDef p) {
        params.add(p);
        return this;
    }

    public TelegramMethod addParam(String name, ParamDef.ParamType type, boolean required, String hint) {
        params.add(new ParamDef(name, type, required, hint));
        return this;
    }

    public TelegramMethod addParam(String name, ParamDef.ParamType type, boolean required, String hint, String def) {
        params.add(new ParamDef(name, type, required, hint, def));
        return this;
    }
}

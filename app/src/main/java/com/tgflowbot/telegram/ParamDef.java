package com.tgflowbot.telegram;

public class ParamDef {
    public enum ParamType { STRING, INTEGER, BOOLEAN, FLOAT }

    public final String name;
    public final ParamType type;
    public final boolean required;
    public final String hint;
    public final String defaultValue;

    public ParamDef(String name, ParamType type, boolean required, String hint) {
        this(name, type, required, hint, null);
    }

    public ParamDef(String name, ParamType type, boolean required, String hint, String defaultValue) {
        this.name = name;
        this.type = type;
        this.required = required;
        this.hint = hint;
        this.defaultValue = defaultValue;
    }

    public boolean isOptional() { return !required; }
}

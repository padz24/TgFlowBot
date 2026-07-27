package com.tgflowbot.telegram;

import java.util.ArrayList;
import java.util.List;

public class MethodRegistry {

    private static final List<TelegramMethod> extensionMethods = new ArrayList<>();

    public static void registerExtension(ExtensionModule ext) {
        for (TelegramMethod m : ext.methods) {
            if (!extensionMethods.contains(m)) {
                extensionMethods.add(m);
            }
        }
    }

    public static void clearExtensions() {
        extensionMethods.clear();
    }

    public static List<TelegramMethod> getAllMethods() {
        return new ArrayList<>(extensionMethods);
    }
}

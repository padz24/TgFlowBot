package com.tgflowbot.telegram.ai;

public class AiProvider {
    public final String id;
    public final String name;
    public final String endpoint;
    public final String defaultModel;
    public final boolean needsApiKey;
    public final boolean keyInHeader;
    public final String keyHeader;
    public final boolean keyInUrl;
    public final String urlKeyParam;
    public final String requestFormat; // "openai", "anthropic", "gemini"

    public AiProvider(String id, String name, String endpoint, String defaultModel,
                      boolean needsApiKey, String requestFormat) {
        this(id, name, endpoint, defaultModel, needsApiKey, requestFormat, true, "Authorization", false, null);
    }

    public AiProvider(String id, String name, String endpoint, String defaultModel,
                      boolean needsApiKey, String requestFormat,
                      boolean keyInHeader, String keyHeader,
                      boolean keyInUrl, String urlKeyParam) {
        this.id = id;
        this.name = name;
        this.endpoint = endpoint;
        this.defaultModel = defaultModel;
        this.needsApiKey = needsApiKey;
        this.keyInHeader = keyInHeader;
        this.keyHeader = keyHeader;
        this.keyInUrl = keyInUrl;
        this.urlKeyParam = urlKeyParam;
        this.requestFormat = requestFormat;
    }

    public static AiProvider[] getBuiltInProviders() {
        return new AiProvider[]{
            new AiProvider("openai", "OpenAI",
                    "https://api.openai.com/v1/chat/completions", "gpt-4o", true, "openai"),
            new AiProvider("claude", "Claude (Anthropic)",
                    "https://api.anthropic.com/v1/messages", "claude-sonnet-4-20250514", true, "anthropic"),
            new AiProvider("groq", "Groq",
                    "https://api.groq.com/openai/v1/chat/completions", "llama-3.3-70b-versatile", true, "openai"),
            new AiProvider("gemini", "Google Gemini",
                    "https://generativelanguage.googleapis.com/v1beta/models/", "gemini-2.0-flash", true, "gemini"),
            new AiProvider("llamacpp", "Llama.cpp",
                    "http://10.0.2.2:8080/v1/chat/completions", "", false, "openai")
        };
    }
}

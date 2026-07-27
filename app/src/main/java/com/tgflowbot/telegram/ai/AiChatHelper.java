package com.tgflowbot.telegram.ai;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class AiChatHelper {

    public interface AiCallback {
        void onSuccess(String responseText);
        void onError(String error);
    }

    public interface AiToolCallback {
        void onToolCalls(List<ToolCall> calls, Runnable retry);
        void onSuccess(String responseText);
        void onError(String error);
    }

    public static class ToolDefinition {
        public final String name;
        public final String description;
        public final JsonObject parameters;

        public ToolDefinition(String name, String description, JsonObject parameters) {
            this.name = name;
            this.description = description;
            this.parameters = parameters;
        }
    }

    public static class ToolCall {
        public final String id;
        public final String name;
        public final String arguments;

        public ToolCall(String id, String name, String arguments) {
            this.id = id;
            this.name = name;
            this.arguments = arguments;
        }
    }

    private static final OkHttpClient client = new OkHttpClient();
    private static final MediaType JSON = MediaType.parse("application/json");

    public static void chatWithTools(AiProvider provider, String apiKey, 
            String message, String model, String systemPrompt,
            float temperature, int maxTokens, String customEndpoint,
            List<ToolDefinition> tools, List<String> history, AiToolCallback cb) {
        String endpoint = provider.endpoint;
        if (customEndpoint != null && !customEndpoint.trim().isEmpty()) {
            endpoint = customEndpoint;
        }

        String resolvedModel = (model != null && !model.trim().isEmpty()) ? model : provider.defaultModel;

        String bodyJson = buildOpenaiToolBody(resolvedModel, message, systemPrompt, 
                temperature, maxTokens, tools, history);

        final String url;
        if ("gemini".equals(provider.requestFormat)) {
            url = endpoint + resolvedModel + ":generateContent?key=" + apiKey;
        } else {
            url = endpoint;
        }

        new Thread(() -> {
            try {
                Request.Builder reqBuilder = new Request.Builder().url(url);

                if (provider.requestFormat.equals("anthropic")) {
                    if (apiKey != null && !apiKey.isEmpty()) {
                        reqBuilder.addHeader("x-api-key", apiKey);
                    }
                    reqBuilder.addHeader("anthropic-version", "2023-06-01");
                    reqBuilder.addHeader("Content-Type", "application/json");
                } else if (provider.keyInHeader && apiKey != null && !apiKey.isEmpty()
                        && !provider.requestFormat.equals("gemini")) {
                    String headerVal = provider.keyHeader.equals("Authorization")
                            ? "Bearer " + apiKey : apiKey;
                    reqBuilder.addHeader(provider.keyHeader, headerVal);
                }

                Request request = reqBuilder
                        .post(RequestBody.create(bodyJson, JSON))
                        .build();

                Response response = client.newCall(request).execute();
                String respBody = response.body() != null ? response.body().string() : "";
                response.close();

                if (!response.isSuccessful()) {
                    cb.onError("HTTP " + response.code() + ": " + respBody);
                    return;
                }

                String toolCallsJson = extractToolCalls(respBody);
                if (toolCallsJson != null) {
                    List<ToolCall> calls = parseToolCalls(toolCallsJson);
                    if (!calls.isEmpty()) {
                        cb.onToolCalls(calls, () -> {});
                        return;
                    }
                }

                String text = parseResponse(provider, respBody);
                if (text != null) {
                    cb.onSuccess(text);
                } else {
                    cb.onError("Gagal parse response");
                }
            } catch (Exception e) {
                cb.onError(e.getMessage());
            }
        }).start();
    }

    public static void continueWithToolResults(AiProvider provider, String apiKey,
            String model, String systemPrompt,
            float temperature, int maxTokens, String customEndpoint,
            List<ToolDefinition> tools, List<String> history, AiToolCallback cb) {
        String endpoint = provider.endpoint;
        if (customEndpoint != null && !customEndpoint.trim().isEmpty()) {
            endpoint = customEndpoint;
        }

        String resolvedModel = (model != null && !model.trim().isEmpty()) ? model : provider.defaultModel;

        JsonObject body = new JsonObject();
        body.addProperty("model", resolvedModel);
        body.addProperty("max_tokens", maxTokens);
        body.addProperty("temperature", temperature);
        JsonArray messages = new JsonArray();
        if (systemPrompt != null && !systemPrompt.trim().isEmpty()) {
            JsonObject sysMsg = new JsonObject();
            sysMsg.addProperty("role", "system");
            sysMsg.addProperty("content", systemPrompt);
            messages.add(sysMsg);
        }
        for (String h : history) {
            messages.add(JsonParser.parseString(h).getAsJsonObject());
        }
        body.add("messages", messages);
        if (tools != null && !tools.isEmpty()) {
            body.add("tools", buildToolsArray(tools));
        }
        String bodyJson = body.toString();

        final String url = endpoint;

        new Thread(() -> {
            try {
                Request.Builder reqBuilder = new Request.Builder().url(url);
                if (provider.keyInHeader && apiKey != null && !apiKey.isEmpty()) {
                    String headerVal = provider.keyHeader.equals("Authorization")
                            ? "Bearer " + apiKey : apiKey;
                    reqBuilder.addHeader(provider.keyHeader, headerVal);
                }
                Request request = reqBuilder
                        .post(RequestBody.create(bodyJson, JSON))
                        .build();

                Response response = client.newCall(request).execute();
                String respBody = response.body() != null ? response.body().string() : "";
                response.close();

                if (!response.isSuccessful()) {
                    cb.onError("HTTP " + response.code() + ": " + respBody);
                    return;
                }

                String toolCallsJson = extractToolCalls(respBody);
                if (toolCallsJson != null) {
                    List<ToolCall> calls = parseToolCalls(toolCallsJson);
                    if (!calls.isEmpty()) {
                        cb.onToolCalls(calls, () -> {});
                        return;
                    }
                }

                String text = parseSimpleResponse(respBody);
                if (text != null) {
                    cb.onSuccess(text);
                } else {
                    cb.onError("Gagal parse response");
                }
            } catch (Exception e) {
                cb.onError(e.getMessage());
            }
        }).start();
    }

    public static void chat(AiProvider provider, String apiKey, String message, String model, String customEndpoint, AiCallback cb) {
        chat(provider, apiKey, message, model, null, 0.7f, 1024, customEndpoint, cb);
    }

    public static void chat(AiProvider provider, String apiKey, String message, String model,
                            String systemPrompt, float temperature, int maxTokens, String customEndpoint, AiCallback cb) {
        String endpoint = provider.endpoint;
        if (customEndpoint != null && !customEndpoint.trim().isEmpty()) {
            endpoint = customEndpoint;
        }

        String resolvedModel = (model != null && !model.trim().isEmpty()) ? model : provider.defaultModel;
        String bodyJson;

        switch (provider.requestFormat) {
            case "anthropic":
                bodyJson = buildAnthropicBody(resolvedModel, message, systemPrompt, maxTokens);
                break;
            case "gemini":
                bodyJson = buildGeminiBody(message, systemPrompt);
                break;
            case "openai":
            default:
                bodyJson = buildOpenaiBody(resolvedModel, message, systemPrompt, temperature, maxTokens);
                break;
        }

        final String url;
        if ("gemini".equals(provider.requestFormat)) {
            url = endpoint + resolvedModel + ":generateContent?key=" + apiKey;
        } else {
            url = endpoint;
        }

        new Thread(() -> {
            try {
                Request.Builder reqBuilder = new Request.Builder().url(url);

                if (provider.requestFormat.equals("anthropic")) {
                    if (apiKey != null && !apiKey.isEmpty()) {
                        reqBuilder.addHeader("x-api-key", apiKey);
                    }
                    reqBuilder.addHeader("anthropic-version", "2023-06-01");
                    reqBuilder.addHeader("Content-Type", "application/json");
                } else if (provider.keyInHeader && apiKey != null && !apiKey.isEmpty()
                        && !provider.requestFormat.equals("gemini")) {
                    String headerVal = provider.keyHeader.equals("Authorization")
                            ? "Bearer " + apiKey : apiKey;
                    reqBuilder.addHeader(provider.keyHeader, headerVal);
                }

                Request request = reqBuilder
                        .post(RequestBody.create(bodyJson, JSON))
                        .build();

                Response response = client.newCall(request).execute();
                String respBody = response.body() != null ? response.body().string() : "";
                response.close();

                if (!response.isSuccessful()) {
                    cb.onError("HTTP " + response.code() + ": " + respBody);
                    return;
                }

                String text = parseResponse(provider, respBody);
                if (text != null) {
                    cb.onSuccess(text);
                } else {
                    cb.onError("Gagal parse response");
                }
            } catch (Exception e) {
                cb.onError(e.getMessage());
            }
        }).start();
    }

    private static String buildOpenaiToolBody(String model, String message, String systemPrompt,
            float temperature, int maxTokens, List<ToolDefinition> tools, List<String> history) {
        JsonObject body = new JsonObject();
        body.addProperty("model", model != null && !model.isEmpty() ? model : "gpt-4o");
        body.addProperty("max_tokens", maxTokens);
        body.addProperty("temperature", temperature);
        JsonArray messages = new JsonArray();
        if (systemPrompt != null && !systemPrompt.trim().isEmpty()) {
            JsonObject sysMsg = new JsonObject();
            sysMsg.addProperty("role", "system");
            sysMsg.addProperty("content", systemPrompt);
            messages.add(sysMsg);
        }
        if (history != null) {
            for (String h : history) {
                messages.add(JsonParser.parseString(h).getAsJsonObject());
            }
        }
        JsonObject userMsg = new JsonObject();
        userMsg.addProperty("role", "user");
        userMsg.addProperty("content", message);
        messages.add(userMsg);
        body.add("messages", messages);
        if (tools != null && !tools.isEmpty()) {
            body.add("tools", buildToolsArray(tools));
        }
        return body.toString();
    }

    private static JsonArray buildToolsArray(List<ToolDefinition> tools) {
        JsonArray arr = new JsonArray();
        for (ToolDefinition td : tools) {
            JsonObject tool = new JsonObject();
            tool.addProperty("type", "function");
            JsonObject func = new JsonObject();
            func.addProperty("name", td.name);
            func.addProperty("description", td.description);
            func.add("parameters", td.parameters);
            tool.add("function", func);
            arr.add(tool);
        }
        return arr;
    }

    private static String extractToolCalls(String responseJson) {
        try {
            JsonObject root = JsonParser.parseString(responseJson).getAsJsonObject();
            JsonArray choices = root.getAsJsonArray("choices");
            if (choices != null && choices.size() > 0) {
                JsonObject msg = choices.get(0).getAsJsonObject().getAsJsonObject("message");
                if (msg != null && msg.has("tool_calls")) {
                    return msg.get("tool_calls").toString();
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    private static List<ToolCall> parseToolCalls(String json) {
        List<ToolCall> calls = new ArrayList<>();
        try {
            JsonArray arr = JsonParser.parseString(json).getAsJsonArray();
            for (int i = 0; i < arr.size(); i++) {
                JsonObject tc = arr.get(i).getAsJsonObject();
                String id = tc.get("id").getAsString();
                JsonObject func = tc.getAsJsonObject("function");
                String name = func.get("name").getAsString();
                String args = func.get("arguments").getAsString();
                calls.add(new ToolCall(id, name, args));
            }
        } catch (Exception ignored) {}
        return calls;
    }

    private static String parseSimpleResponse(String json) {
        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            JsonArray choices = root.getAsJsonArray("choices");
            if (choices != null && choices.size() > 0) {
                JsonObject msg = choices.get(0).getAsJsonObject().getAsJsonObject("message");
                if (msg != null && msg.has("content") && !msg.get("content").isJsonNull()) {
                    return msg.get("content").getAsString();
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    private static String buildOpenaiBody(String model, String message, String systemPrompt,
                                          float temperature, int maxTokens) {
        JsonObject body = new JsonObject();
        body.addProperty("model", model != null && !model.isEmpty() ? model : "gpt-4o");
        body.addProperty("max_tokens", maxTokens);
        body.addProperty("temperature", temperature);
        JsonArray messages = new JsonArray();
        if (systemPrompt != null && !systemPrompt.trim().isEmpty()) {
            JsonObject sysMsg = new JsonObject();
            sysMsg.addProperty("role", "system");
            sysMsg.addProperty("content", systemPrompt);
            messages.add(sysMsg);
        }
        JsonObject userMsg = new JsonObject();
        userMsg.addProperty("role", "user");
        userMsg.addProperty("content", message);
        messages.add(userMsg);
        body.add("messages", messages);
        return body.toString();
    }

    private static String buildAnthropicBody(String model, String message, String systemPrompt,
                                              int maxTokens) {
        JsonObject body = new JsonObject();
        body.addProperty("model", model != null && !model.isEmpty() ? model : "claude-sonnet-4-20250514");
        body.addProperty("max_tokens", maxTokens);
        if (systemPrompt != null && !systemPrompt.trim().isEmpty()) {
            body.addProperty("system", systemPrompt);
        }
        JsonArray messages = new JsonArray();
        JsonObject userMsg = new JsonObject();
        userMsg.addProperty("role", "user");
        userMsg.addProperty("content", message);
        messages.add(userMsg);
        body.add("messages", messages);
        return body.toString();
    }

    private static String buildGeminiBody(String message, String systemPrompt) {
        JsonObject body = new JsonObject();
        JsonArray contents = new JsonArray();
        JsonObject content = new JsonObject();
        JsonArray parts = new JsonArray();
        JsonObject part = new JsonObject();
        part.addProperty("text", message);
        parts.add(part);
        content.add("parts", parts);
        contents.add(content);
        body.add("contents", contents);
        return body.toString();
    }

    private static String parseResponse(AiProvider provider, String json) {
        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();

            switch (provider.requestFormat) {
                case "anthropic": {
                    JsonArray content = root.getAsJsonArray("content");
                    if (content != null && content.size() > 0) {
                        return content.get(0).getAsJsonObject().get("text").getAsString();
                    }
                    break;
                }
                case "gemini": {
                    JsonArray candidates = root.getAsJsonArray("candidates");
                    if (candidates != null && candidates.size() > 0) {
                        JsonObject candidate = candidates.get(0).getAsJsonObject();
                        JsonObject c = candidate.getAsJsonObject("content");
                        if (c != null) {
                            JsonArray parts = c.getAsJsonArray("parts");
                            if (parts != null && parts.size() > 0) {
                                return parts.get(0).getAsJsonObject().get("text").getAsString();
                            }
                        }
                    }
                    break;
                }
                default: {
                    JsonArray choices = root.getAsJsonArray("choices");
                    if (choices != null && choices.size() > 0) {
                        JsonObject choice = choices.get(0).getAsJsonObject();
                        JsonObject msg = choice.getAsJsonObject("message");
                        if (msg != null) {
                            String content = msg.get("content").getAsString();
                            return content;
                        }
                    }
                    break;
                }
            }
        } catch (Exception ignored) {}
        return null;
    }
}

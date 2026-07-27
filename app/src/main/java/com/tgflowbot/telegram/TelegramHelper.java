package com.tgflowbot.telegram;

import java.net.URLEncoder;
import java.util.Map;

import okhttp3.FormBody;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class TelegramHelper {

    private final OkHttpClient client;
    private final String botToken;

    public TelegramHelper(String botToken) {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                .build();
        this.botToken = botToken;
    }

    public interface Callback {
        void onSuccess(String result);
        void onError(String error);
    }

    public void sendMessage(String chatId, String text, Callback callback) {
        new Thread(() -> {
            try {
                String url = "https://api.telegram.org/bot" + botToken
                        + "/sendMessage?chat_id=" + chatId
                        + "&text=" + URLEncoder.encode(text, "UTF-8");

                Request request = new Request.Builder().url(url).build();
                Response response = client.newCall(request).execute();

                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body().string());
                } else {
                    String errBody = response.body() != null ? response.body().string() : "";
                    callback.onError("HTTP " + response.code() + ": " + errBody);
                }
                response.close();
            } catch (Exception e) {
                callback.onError(e.getMessage());
            }
        }).start();
    }

    public void getUpdates(int offset, Callback callback) {
        callMethod("getUpdates", null, new Callback() {
            @Override
            public void onSuccess(String result) {
                callback.onSuccess(result);
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }

    public void callMethod(String methodName, Map<String, String> params, Callback callback) {
        new Thread(() -> {
            try {
                String url = "https://api.telegram.org/bot" + botToken + "/" + methodName;
                FormBody.Builder formBuilder = new FormBody.Builder();
                if (params != null) {
                    for (Map.Entry<String, String> e : params.entrySet()) {
                        String key = e.getKey();
                        String value = e.getValue();
                        if (key != null && value != null) {
                            formBuilder.add(key, value);
                        }
                    }
                }
                RequestBody body = formBuilder.build();
                Request request = new Request.Builder().url(url).post(body).build();
                Response response = client.newCall(request).execute();
                String responseBody = response.body() != null ? response.body().string() : "";
                response.close();

                if (response.isSuccessful()) {
                    callback.onSuccess(responseBody);
                } else {
                    callback.onError("HTTP " + response.code() + ": " + responseBody);
                }
            } catch (Exception e) {
                callback.onError(e.getMessage());
            }
        }).start();
    }

    public void callMethodMultipart(String methodName, Map<String, String> params,
                                    byte[] fileData, String fileName, String fieldName,
                                    String mimeType, Callback callback) {
        new Thread(() -> {
            try {
                String url = "https://api.telegram.org/bot" + botToken + "/" + methodName;
                MultipartBody.Builder mpBuilder = new MultipartBody.Builder()
                        .setType(MultipartBody.FORM);

                if (fileData != null && fieldName != null) {
                    RequestBody fileBody = RequestBody.create(
                            fileData,
                            okhttp3.MediaType.parse(mimeType != null ? mimeType : "application/octet-stream")
                    );
                    mpBuilder.addFormDataPart(fieldName, fileName != null ? fileName : "file", fileBody);
                }

                if (params != null) {
                    for (Map.Entry<String, String> e : params.entrySet()) {
                        String key = e.getKey();
                        String value = e.getValue();
                        if (key != null && value != null && !key.equals(fieldName)) {
                            mpBuilder.addFormDataPart(key, value);
                        }
                    }
                }

                RequestBody body = mpBuilder.build();
                Request request = new Request.Builder().url(url).post(body).build();
                Response response = client.newCall(request).execute();
                String responseBody = response.body() != null ? response.body().string() : "";
                response.close();

                if (response.isSuccessful()) {
                    callback.onSuccess(responseBody);
                } else {
                    callback.onError("HTTP " + response.code() + ": " + responseBody);
                }
            } catch (Exception e) {
                callback.onError(e.getMessage());
            }
        }).start();
    }

    public void callMethodGet(String methodName, Map<String, String> params, Callback callback) {
        new Thread(() -> {
            try {
                StringBuilder urlBuilder = new StringBuilder(
                        "https://api.telegram.org/bot" + botToken + "/" + methodName);
                if (params != null && !params.isEmpty()) {
                    urlBuilder.append("?");
                    boolean first = true;
                    for (Map.Entry<String, String> e : params.entrySet()) {
                        if (!first) urlBuilder.append("&");
                        first = false;
                        urlBuilder.append(e.getKey())
                                .append("=")
                                .append(URLEncoder.encode(e.getValue() != null ? e.getValue() : "", "UTF-8"));
                    }
                }

                Request request = new Request.Builder().url(urlBuilder.toString()).build();
                Response response = client.newCall(request).execute();
                String responseBody = response.body() != null ? response.body().string() : "";
                response.close();

                if (response.isSuccessful()) {
                    callback.onSuccess(responseBody);
                } else {
                    callback.onError("HTTP " + response.code() + ": " + responseBody);
                }
            } catch (Exception e) {
                callback.onError(e.getMessage());
            }
        }).start();
    }
}

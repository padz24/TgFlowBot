package com.tgflowbot.telegram;

import java.net.URLEncoder;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class TelegramHelper {

    private final OkHttpClient client;
    private final String botToken;

    public TelegramHelper(String botToken) {
        this.client = new OkHttpClient();
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
                    callback.onError("HTTP " + response.code());
                }
                response.close();
            } catch (Exception e) {
                callback.onError(e.getMessage());
            }
        }).start();
    }

    public void getUpdates(int offset, Callback callback) {
        new Thread(() -> {
            try {
                String url = "https://api.telegram.org/bot" + botToken
                        + "/getUpdates?timeout=30";
                if (offset > 0) {
                    url += "&offset=" + offset;
                }

                Request request = new Request.Builder().url(url).build();
                Response response = client.newCall(request).execute();

                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body().string());
                } else {
                    callback.onError("HTTP " + response.code());
                }
                response.close();
            } catch (Exception e) {
                callback.onError(e.getMessage());
            }
        }).start();
    }
}

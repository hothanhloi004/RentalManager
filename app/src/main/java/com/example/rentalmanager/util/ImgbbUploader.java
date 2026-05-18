package com.example.rentalmanager.util;

import android.util.Log;
import androidx.annotation.NonNull;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ImgbbUploader {

    private static final String TAG = "ImgbbUploader";
    private static final String IMGBB_API_URL = "https://api.imgbb.com/1/upload";

    // ImgBB free API key — lấy từ https://api.imgbb.com sau khi đăng ký
    private static final String API_KEY = "7b1b32a85809c2e714b2d44b72a0f934";

    private static final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build();

    public interface UploadCallback {
        void onSuccess(String imageUrl);
        void onFailure(String error);
    }

    public static void upload(File file, UploadCallback callback) {
        if (file == null || !file.exists()) {
            if (callback != null) callback.onFailure("File not found");
            return;
        }

        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("key", API_KEY)
                .addFormDataPart("image", file.getName(),
                        RequestBody.create(file, MediaType.parse("image/jpeg")))
                .build();

        Request request = new Request.Builder()
                .url(IMGBB_API_URL)
                .post(requestBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "Upload failed: " + e.getMessage());
                if (callback != null) callback.onFailure(e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (!response.isSuccessful()) {
                    Log.e(TAG, "Upload failed, code: " + response.code());
                    if (callback != null) callback.onFailure("HTTP " + response.code());
                    return;
                }

                String body = response.body() != null ? response.body().string() : "";
                Log.d(TAG, "Upload response: " + body);

                // Parse JSON response: { "data": { "url": "https://i.ibb.co/..." } }
                String imageUrl = parseImgbbUrl(body);
                if (imageUrl != null) {
                    if (callback != null) callback.onSuccess(imageUrl);
                } else {
                    if (callback != null) callback.onFailure("Parse error");
                }
            }
        });
    }

    private static String parseImgbbUrl(String json) {
        int urlIndex = json.indexOf("\"url\":\"");
        if (urlIndex == -1) return null;
        int start = urlIndex + 7;
        int end = json.indexOf("\"", start);
        if (end == -1) return null;
        return json.substring(start, end).replace("\\/", "/");
    }
}
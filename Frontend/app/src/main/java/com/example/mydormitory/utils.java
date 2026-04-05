package com.example.mydormitory;

import static android.content.Context.MODE_PRIVATE;

import android.app.DownloadManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.icu.text.SimpleDateFormat;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.crypto.SecretKey;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

public class utils {

    private static ExecutorService executor = Executors.newSingleThreadExecutor();
    private static Handler mainHandler = new Handler(Looper.getMainLooper());

    public static String uploadFileToServer(Context context, Uri fileUri, String folder, String fileType) throws Exception {
        String uploadUrl = "http://10.0.2.2:3000/file/" + folder;
        ContentResolver resolver = context.getContentResolver();
        InputStream inputStream = resolver.openInputStream(fileUri);

        // Определяем MIME тип
        String mimeType = resolver.getType(fileUri);
        if (mimeType == null) {
            // Устанавливаем MIME тип по умолчанию в зависимости от типа файла
            if ("photo".equals(fileType)) {
                mimeType = "image/jpeg";
            } else {
                mimeType = "application/octet-stream";
            }
        }

        HttpURLConnection connection = (HttpURLConnection) new URL(uploadUrl).openConnection();
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setDoInput(true);
        connection.setRequestProperty("Content-Type", mimeType);

        DataOutputStream outputStream = new DataOutputStream(connection.getOutputStream());
        byte[] buffer = new byte[4096];
        int bytesRead;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, bytesRead);
        }
        inputStream.close();
        outputStream.flush();
        outputStream.close();

        int responseCode = connection.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK && responseCode != HttpURLConnection.HTTP_CREATED) {
            throw new Exception("Ошибка загрузки файла: " + responseCode);
        }

        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        reader.close();

        JSONObject jsonResponse = new JSONObject(response.toString());
        return jsonResponse.getString("file_path");
    }

    public static Bitmap downloadImageFromServer(String imagePath) throws Exception {
        String downloadUrl = "http://10.0.2.2:3000/file" + imagePath;
        Log.d("IMAGE_DEBUG", "🔄 Loading image: " + downloadUrl);

        HttpURLConnection connection = (HttpURLConnection) new URL(downloadUrl).openConnection();
        connection.setRequestMethod("GET");

        try {
            int responseCode = connection.getResponseCode();
            Log.d("IMAGE_DEBUG", "📊 Response code: " + responseCode);

            if (responseCode == HttpURLConnection.HTTP_OK) {
                InputStream inputStream = connection.getInputStream();

                // Читаем все данные в массив байтов
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    byteArrayOutputStream.write(buffer, 0, bytesRead);
                }

                byte[] imageData = byteArrayOutputStream.toByteArray();
                Log.d("IMAGE_DEBUG", "📦 Received " + imageData.length + " bytes");

                // Пробуем создать Bitmap из байтов
                Bitmap bitmap = BitmapFactory.decodeByteArray(imageData, 0, imageData.length);

                inputStream.close();
                byteArrayOutputStream.close();

                if (bitmap == null) {
                    Log.e("IMAGE_DEBUG", "❌ BitmapFactory returned null - invalid image data");
                    throw new Exception("Invalid image data received");
                }

                Log.d("IMAGE_DEBUG", "✅ Successfully created bitmap: " + bitmap.getWidth() + "x" + bitmap.getHeight());
                return bitmap;
            } else {
                throw new Exception("HTTP error: " + responseCode);
            }
        } catch (Exception e) {
            Log.e("IMAGE_DEBUG", "💥 Exception: " + e.getMessage());
            throw e;
        } finally {
            connection.disconnect();
        }
    }

    public static List<String> getUserRolesFromToken(Context context, String token, String refreshToken) {
        List<String> defaultEmptyList = new ArrayList<>();

        if (token == null || token.isEmpty()) {
            Log.d("TokenDebug", "Токен пустой");
            return defaultEmptyList;
        }

        try {
            String secretKey = "my_super_secret_key_bytes_min_wawawawawwawawwawawawawaw";
            SecretKey key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));

            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            List<String> user_roles = claims.get("roles", List.class);
            if (user_roles == null || user_roles.isEmpty()) {
                Log.d("TokenDebug", "Роли не найдены в токене");
                return defaultEmptyList;
            }
            return user_roles;
        }
        catch (Exception e) {
            Log.d("TokenDebug", "Ошибка парсинга ролей, пробуем обновить: " + e.getMessage());

            if (refreshToken != null && !refreshToken.isEmpty()) {
                // Запускаем обновление в фоне и синхронно ждем результат
                final boolean[] refreshed = {false};
                final Object lock = new Object();

                executor.execute(() -> {
                    boolean result = refreshAccessTokenSync(context, refreshToken);
                    synchronized (lock) {
                        refreshed[0] = result;
                        lock.notify();
                    }
                });

                try {
                    synchronized (lock) {
                        lock.wait(10000); // ждем до 10 секунд
                    }
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }

                if (refreshed[0]) {
                    SharedPreferences prefs = context.getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE);
                    String newToken = prefs.getString("access_token", null);

                    if (newToken != null) {
                        return getUserRolesFromToken(context, newToken, refreshToken);
                    }
                }
            }
            Log.d("TokenDebug", "Не удалось получить роли даже после обновления токена");
            return defaultEmptyList;
        }
    }

    // Метод для проверки конкретной роли
    public static boolean hasRole(Context context, String token, String refreshToken, String roleName) {
        List<String> roles = getUserRolesFromToken(context, token, refreshToken);
        return roles.contains(roleName);
    }

    public static int getUserIdFromToken(Context context, String token, String refreshToken) {
        try {
            if (token == null || token.isEmpty()) {
                Log.d("TokenDebug", "Токен пустой");
                return -1;
            }

            try {
                String secretKey = "my_super_secret_key_bytes_min_wawawawawwawawwawawawawaw";
                SecretKey key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));

                Claims claims = Jwts.parserBuilder()
                        .setSigningKey(key)
                        .build()
                        .parseClaimsJws(token)
                        .getBody();

                Number id = claims.get("Id", Number.class);
                if (id == null) {
                    return -1;
                }
                return id.intValue();
            }
            catch (Exception e) {
                Log.d("TokenDebug", "Ошибка парсинга токена, пробуем обновить: " + e.getMessage());

                if (refreshToken != null && !refreshToken.isEmpty()) {
                    // Запускаем обновление в фоне и синхронно ждем результат
                    final boolean[] refreshed = {false};
                    final Object lock = new Object();

                    executor.execute(() -> {
                        boolean result = refreshAccessTokenSync(context, refreshToken);
                        synchronized (lock) {
                            refreshed[0] = result;
                            lock.notify();
                        }
                    });

                    try {
                        synchronized (lock) {
                            lock.wait(10000); // ждем до 10 секунд
                        }
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }

                    if (refreshed[0]) {
                        SharedPreferences prefs = context.getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
                        String newToken = prefs.getString("access_token", null);
                        String newRfrToken = prefs.getString("refresh_token", null);

                        if (newToken != null) {
                            return getUserIdFromToken(context, newToken, newRfrToken);
                        }
                    }
                }
                return -1;
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    // Синхронная версия для внутреннего использования
    private static boolean refreshAccessTokenSync(Context context, String refreshToken) {
        try {
            String refreshUrl = "http://10.0.2.2:3000/refresh";

            JSONObject jsonBody = new JSONObject();
            jsonBody.put("refresh_token", refreshToken);

            HttpURLConnection conn = (HttpURLConnection) new URL(refreshUrl).openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            conn.setDoOutput(true);

            try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(conn.getOutputStream(), "UTF-8"))) {
                writer.write(jsonBody.toString());
            }

            int code = conn.getResponseCode();

            BufferedReader reader;
            if (code == HttpURLConnection.HTTP_OK) {
                reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
            } else {
                reader = new BufferedReader(new InputStreamReader(conn.getErrorStream(), "UTF-8"));
            }

            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            Log.d("RefreshDebug", "Response code: " + code);
            Log.d("RefreshDebug", "Response body: " + response.toString());

            if (code == HttpURLConnection.HTTP_OK) {
                JSONObject json = new JSONObject(response.toString());
                String newAccess = json.getString("access_token");
                String newRefresh = json.getString("refresh_token");

                SharedPreferences prefs = context.getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
                prefs.edit()
                        .putString("access_token", newAccess)
                        .putString("refresh_token", newRefresh)
                        .apply();

                conn.disconnect();
                return true;
            } else {
                Log.e("RefreshError", "Server returned error: " + response.toString());
                conn.disconnect();
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean refreshAccessToken(Context context, String refreshToken) {
        return refreshAccessTokenSync(context, refreshToken);
    }

    public static String changeDate(String date) {
        if (date != null && date.length() >= 16) {
            try {
                String datePart = date.substring(0, 10);
                String timePart = date.substring(11, 16);
                LocalDateTime parsedDate = LocalDateTime.parse(datePart + "T" + timePart); // ISO

                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMMM yyyy HH:mm");
                return parsedDate.format(formatter);
            } catch (Exception e) {
                return date;
            }
        }
        return date;
    }

    public static String formatDate(String inputDate) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat("dd MMMM yyyy", Locale.getDefault());

            Date date = inputFormat.parse(inputDate);
            return outputFormat.format(date);
        } catch (Exception e) {
            e.printStackTrace();
            return inputDate; // возвращаем исходную строку в случае ошибки
        }
    }

    public static List<newsforrepairman> parseNewsFromJson(JSONArray jsonArray) throws JSONException {
        List<newsforrepairman> news = new ArrayList<>();

        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject guideJson = jsonArray.getJSONObject(i);

            int id = guideJson.getInt("id");
            String type = guideJson.getString("type");
            String body = guideJson.getString("body");
            String date = utils.changeDate(guideJson.getString("date"));
            int room = guideJson.getInt("room");
            int user_id = guideJson.getInt("user_id");
            boolean activity = guideJson.getBoolean("activity");
            int repairman_id = guideJson.getInt("repairman_id");
            boolean ending = guideJson.getBoolean("ending");

            // Парсим массив изображений
            List<String> imagePaths = new ArrayList<>();
            if (guideJson.has("repair_path")) {
                JSONArray imagesArray = guideJson.getJSONArray("repair_path");
                for (int j = 0; j < imagesArray.length(); j++) {
                    imagePaths.add(imagesArray.getString(j));
                }
            }

            news.add(new newsforrepairman(id, type, body, date, room, user_id, repairman_id, activity, ending, imagePaths));
        }

        return news;
    }

    public static void addFileToContainer(Context context, LinearLayout container, String filePath) {
        ImageView imageView = new ImageView(container.getContext());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(300, 300);
        params.setMargins(0, 0, 16, 15);
        imageView.setLayoutParams(params);
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        imageView.setBackground(null);

        if (filePath.endsWith(".jpg") || filePath.endsWith(".jpeg") || filePath.endsWith(".png")) {
            loadImage(imageView, filePath);
        } else {
            if (filePath.endsWith(".pdf")) {
                imageView.setImageResource(R.drawable.ic_pdf);
            } else if (filePath.endsWith(".doc") || filePath.endsWith(".docx")) {
                imageView.setImageResource(R.drawable.ic_word);
            } else {
                imageView.setImageResource(R.drawable.ic_downloads);
            }
        }

        imageView.setOnClickListener(v -> {
            String fileName = filePath.substring(filePath.lastIndexOf("/"));
            String fullUrl = "http://10.0.2.2:3000/file" + filePath;

            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(fullUrl));
            request.setTitle(fileName);
            request.setDescription("Скачивание файла");
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);

            // MIME тип тоже можно определить прямо здесь
            if (filePath.endsWith(".pdf")) {
                request.setMimeType("application/pdf");
            } else if (filePath.endsWith(".doc") || filePath.endsWith(".docx")) {
                request.setMimeType("application/msword");
            } else if (filePath.endsWith(".jpg") || filePath.endsWith(".jpeg")) {
                request.setMimeType("image/jpeg");
            } else if (filePath.endsWith(".png")) {
                request.setMimeType("image/png");
            }

            DownloadManager dm = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
            dm.enqueue(request);

            Toast.makeText(context, "Скачивание: " + fullUrl, Toast.LENGTH_SHORT).show();
        });

        container.addView(imageView);
    }

    public static void loadImage(ImageView imageView, String imagePath) {
        new Thread(() -> {
            try {
                android.graphics.Bitmap bitmap = utils.downloadImageFromServer(imagePath);
                if (bitmap != null) {
                    imageView.post(() -> imageView.setImageBitmap(bitmap));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}
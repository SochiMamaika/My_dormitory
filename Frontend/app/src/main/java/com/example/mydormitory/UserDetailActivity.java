package com.example.mydormitory;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class UserDetailActivity extends AppCompatActivity {
    ImageButton menuButton;
    Button makeUserToAdminButton;
    Button removeAdminButton;

    private TextView userName, userLastName, userSurname, userPhone, noDocumentsText;
    private LinearLayout filesContainer;

    private int targetUserId;
    private static final String BASE_URL = "http://10.0.2.2:3000";
    private static final String API_URL = BASE_URL + "/user";
    private static final String API_URLS = BASE_URL + "/users";
    private String accessToken, refreshToken;
    private int currentUserId;
    private boolean isAdmin = false;
    private boolean targetIsAdmin = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.userdetail);

        SharedPreferences prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
        accessToken = prefs.getString("access_token", null);
        refreshToken = prefs.getString("refresh_token", null);
        currentUserId = utils.getUserIdFromToken(this, accessToken, refreshToken);

        if (accessToken == null) {
            Toast.makeText(this, "Пользователь не авторизован", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, loginActivity.class));
            finish();
        }

        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("user_id"))
        {
            targetUserId = intent.getIntExtra("user_id", -1);
            if (targetUserId == -1)
            {
                Toast.makeText(this, "Ошибка: не указан пользователь", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
        else
        {
            Toast.makeText(this, "Ошибка: не указан пользователь", Toast.LENGTH_SHORT).show();
            finish();
        }
        menuButton = findViewById(R.id.menuButton);
        makeUserToAdminButton = findViewById(R.id.makeUserToAdminButton);
        removeAdminButton = findViewById(R.id.removeAdminButton);
        userName = findViewById(R.id.userName);
        userLastName = findViewById(R.id.userLastName);
        userSurname = findViewById(R.id.userSurname);
        userPhone = findViewById(R.id.userPhone);
        filesContainer = findViewById(R.id.filesContainer);
        noDocumentsText = findViewById(R.id.noDocumentsText);

        loadUserDetails();
        checkIfCurrentUserIsAdmin();
        setupClickListeners();
    }

    private void setupClickListeners() {
        menuButton.setOnClickListener(v -> startActivity(new Intent(this, searchActivity.class)));
        makeUserToAdminButton.setOnClickListener(v -> makeUserAdmin());
        removeAdminButton.setOnClickListener(v -> removeAdminRights());
    }

    private void loadUserDetails() {
        new Thread(() -> {
            try
            {
                String userDetailUrl = API_URLS + "/" + targetUserId;
                String response = sendGetRequest(userDetailUrl);

                if (response != null) {
                    JSONObject userJson = new JSONObject(response);
                    runOnUiThread(() -> {
                        try {
                            displayUserData(userJson);
                        }
                        catch (JSONException e) {
                            showToast("Ошибка обработки данных: " + e.getMessage());
                        }
                    });
                }
            }
            catch (Exception e) {
                runOnUiThread(() -> showToast("Ошибка загрузки данных пользователя: " + e.getMessage()));
            }
        }).start();
    }

    private void displayUserData(JSONObject userJson) throws JSONException {
        // Основные данные
        userName.setText(userJson.getString("name"));
        userLastName.setText(userJson.getString("last_name"));
        userSurname.setText(userJson.getString("surname"));
        userPhone.setText(userJson.getString("phone_number"));

        // Документы - отображаем как иконки
        displayDocuments(userJson);

        // Проверяем, является ли ЦЕЛЕВОЙ пользователь админом
        checkTargetUserIsAdmin(userJson);

        // Проверяем права текущего пользователя
        checkIfCurrentUserIsAdmin();

        // Обновляем кнопки
        updateButtonVisibility();
    }

    private void displayDocuments(JSONObject userJson) throws JSONException {
        filesContainer.removeAllViews();

        if (userJson.has("document"))
        {
            JSONArray documentsArray = userJson.getJSONArray("document");

            if (documentsArray.length() > 0)
            {
                noDocumentsText.setVisibility(View.GONE);
                filesContainer.setVisibility(View.VISIBLE);

                for (int i = 0; i < documentsArray.length(); i++)
                {
                    String filePath = documentsArray.getString(i);
                    utils.addFileToContainer(this, filesContainer, filePath);
                }
            }
            else
            {
                showNoDocuments();
            }
        }
        else
        {
            showNoDocuments();
        }
    }

    private void showNoDocuments() {
        noDocumentsText.setVisibility(View.VISIBLE);
        filesContainer.setVisibility(View.GONE);
    }

    private void checkIfCurrentUserIsAdmin() {
        isAdmin = utils.hasRole(this, accessToken, refreshToken, "user_write");
    }

    private void checkTargetUserIsAdmin(JSONObject userJson) throws JSONException {
        targetIsAdmin = false;

        if (userJson.has("roles")) {
            JSONArray rolesArray = userJson.getJSONArray("roles");

            for (int i = 0; i < rolesArray.length(); i++) {
                String role = rolesArray.getString(i);

                // Проверяем наличие write-прав
                if (role.contains("write") || role.contains("admin")) {
                    targetIsAdmin = true;
                    break;
                }
            }
        }
        else {
            Log.d("ADMIN_DEBUG", "У целевого пользователя нет поля 'roles'");
        }
    }

    private void updateButtonVisibility() {
        boolean canChangeRights = isAdmin && targetUserId != currentUserId;

        if (canChangeRights) {
            if (targetIsAdmin) {
                Log.d("ADMIN_DEBUG", "Показываем кнопку 'Убрать права'");
                makeUserToAdminButton.setVisibility(View.GONE);
                removeAdminButton.setVisibility(View.VISIBLE);
            }
            else {
                Log.d("ADMIN_DEBUG", "Показываем кнопку 'Дать права'");
                makeUserToAdminButton.setVisibility(View.VISIBLE);
                removeAdminButton.setVisibility(View.GONE);
            }
        }
        else {
            Log.d("ADMIN_DEBUG", "Скрываем кнопки");
            makeUserToAdminButton.setVisibility(View.GONE);
            removeAdminButton.setVisibility(View.GONE);
        }
    }

    private void makeUserAdmin() {
        new Thread(() -> {
            try {
                String addRoleUrl = API_URL + "/" + targetUserId;
                String response = sendPutRequest(addRoleUrl, "{}");

                runOnUiThread(() -> {
                    if (response != null) {
                        showToast("Админские права успешно добавлены");
                        // Обновляем данные пользователя
                        loadUserDetails();
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> showToast("Ошибка: " + e.getMessage()));
            }
        }).start();
    }

    private void removeAdminRights() {
        new Thread(() -> {
            try {
                String removeRoleUrl = API_URL + "/" + targetUserId;
                String response = sendDeleteRequest(removeRoleUrl);

                runOnUiThread(() -> {
                    if (response != null) {
                        showToast("Админские права успешно удалены");
                        // Обновляем данные пользователя
                        loadUserDetails();
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> showToast("Ошибка: " + e.getMessage()));
            }
        }).start();
    }

    private String sendGetRequest(String url) throws Exception {
        return sendRequest("GET", url, null);
    }

    private String sendPutRequest(String url, String requestBody) throws Exception {
        return sendRequest("PUT", url, requestBody);
    }

    private String sendDeleteRequest(String url) throws Exception {
        return sendRequest("DELETE", url, null);
    }

    private String sendRequest(String method, String url, String requestBody) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod(method);
        connection.setRequestProperty("Content-Type", "application/json; utf-8");
        connection.setRequestProperty("Authorization", "Bearer " + accessToken);

        if (requestBody != null && (method.equals("PUT") || method.equals("POST"))) {
            connection.setDoOutput(true);
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = requestBody.getBytes("utf-8");
                os.write(input, 0, input.length);
            }
        }

        int responseCode = connection.getResponseCode();

        if (responseCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
            connection.disconnect();
            if (utils.refreshAccessToken(this, refreshToken)) {
                SharedPreferences prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
                accessToken = prefs.getString("access_token", null);
                refreshToken = prefs.getString("refresh_token", null);
                return sendRequest(method, url, requestBody);
            } else {
                handleSessionExpired();
                return null;
            }
        }
        if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_CREATED) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();
            connection.disconnect();

            return response.toString();
        }

        // Также принимаем 204 (NO CONTENT)
        if (responseCode == HttpURLConnection.HTTP_NO_CONTENT) {
            connection.disconnect();
            return "{}";
        }

        // Для отладки получаем сообщение об ошибке
        BufferedReader errorReader = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
        StringBuilder errorResponse = new StringBuilder();
        String line;
        while ((line = errorReader.readLine()) != null) {
            errorResponse.append(line);
        }
        errorReader.close();
        connection.disconnect();

        throw new Exception("Ошибка сервера: " + responseCode + " - " + errorResponse.toString());
    }

    private void handleSessionExpired() {
        runOnUiThread(() -> {
            SharedPreferences prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
            prefs.edit().remove("access_token").remove("refresh_token").apply();

            showToast("Сессия истекла. Войдите снова");

            Intent intent = new Intent(this, loginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
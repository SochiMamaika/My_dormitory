package com.example.mydormitory;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class guideActivity extends AppCompatActivity {
    ImageButton menuButton, addGuideButton;
    private RecyclerView guideRecyclerView;
    private guideAdapter guideAdapter;
    private List<guide> guideList = new ArrayList<>();
    private static final String API_URL = "http://10.0.2.2:3000/tutor";
    private String accessToken;
    private String refreshToken;
    private boolean hasGuidesWriteRole = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.guide);

        // Получаем токены из SharedPreferences
        SharedPreferences prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
        accessToken = prefs.getString("access_token", null);
        refreshToken = prefs.getString("refresh_token", null);

        // Проверяем авторизацию
        if (accessToken == null) {
            Toast.makeText(this, "Пользователь не авторизован", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, loginActivity.class));
            finish();
            return;
        }

        hasGuidesWriteRole = utils.hasRole(this, accessToken, refreshToken, "tutor_write");

        // Инициализация элементов
        menuButton = findViewById(R.id.menuButton);
        addGuideButton = findViewById(R.id.addGuideButton);
        guideRecyclerView = findViewById(R.id.guideList);

        // Настройка RecyclerView
        guideAdapter = new guideAdapter(guideList, hasGuidesWriteRole);

        if (hasGuidesWriteRole) {
            guideAdapter.setOnGuideClickListener((item, position) -> {
                Toast.makeText(guideActivity.this, "Удаление гайда: " + item.getBody(), Toast.LENGTH_SHORT).show();
                deleteGuideFromServer(item.getId(), position);
            });
        }

        guideRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        guideRecyclerView.setAdapter(guideAdapter);

        TextInputEditText searchGuide = findViewById(R.id.searchGuide);
        searchGuide.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                guideAdapter.getFilter().filter(s);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Загрузка данных с API
        loadGuidesFromApi();

        menuButton.setOnClickListener(v -> {
            Intent intent = new Intent(guideActivity.this, allWidjet.class);
            startActivity(intent);
        });

        // Показываем/скрываем кнопку добавления в зависимости от роли
        if (hasGuidesWriteRole) {
            addGuideButton.setVisibility(View.VISIBLE);
            addGuideButton.setOnClickListener(v -> {
                Intent intent = new Intent(guideActivity.this, addGuideActivity.class);
                startActivity(intent);
            });
        }
        else {
            addGuideButton.setVisibility(View.GONE);
        }
    }

    private void deleteGuideFromServer(int id, int position) {
        new Thread(() -> {
            try {
                boolean success = sendDeleteRequest(id);
                runOnUiThread(() -> {
                    if (success)
                    {
                        guideList.remove(position);
                        guideAdapter.notifyItemRemoved(position);
                        Toast.makeText(guideActivity.this, "Объявление удалено", Toast.LENGTH_SHORT).show();
                    }
                    else
                    {
                        Toast.makeText(guideActivity.this, "Ошибка удаления", Toast.LENGTH_SHORT).show();
                    }
                });
            }
            catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(guideActivity.this, "Ошибка: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                e.printStackTrace();
            }
        }).start();
    }

    private boolean sendDeleteRequest(int id) throws Exception
    {
        String urlStr = API_URL+ "/" + id;
        HttpURLConnection connection = (HttpURLConnection) new URL(urlStr).openConnection();
        connection.setRequestMethod("DELETE");
        connection.setRequestProperty("Authorization", "Bearer " + accessToken);

        int responseCode = connection.getResponseCode();

        if (responseCode == HttpURLConnection.HTTP_UNAUTHORIZED)
        {
            connection.disconnect();
            if (utils.refreshAccessToken(this, refreshToken))
            {
                SharedPreferences prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
                accessToken = prefs.getString("access_token", null);
                refreshToken = prefs.getString("refresh_token", null);
                return sendDeleteRequest(id);
            }
            else
            {
                // Сессия истекла
                runOnUiThread(() -> {
                    SharedPreferences prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.remove("access_token");
                    editor.remove("refresh_token");
                    editor.apply();
                    Toast.makeText(guideActivity.this, "Сессия истекла. Войдите снова", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(guideActivity.this, loginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                });
                return false;
            }
        }
        connection.disconnect();
        return responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_NO_CONTENT;
    }

    private void loadGuidesFromApi() {
        new Thread(() -> {
            try {
                String response = sendGetRequest(accessToken, refreshToken);
                JSONArray jsonArray = new JSONArray(response);
                final List<guide> guides = parseGuidesFromJson(jsonArray);

                runOnUiThread(() -> {
                    guideList.clear();
                    guideList.addAll(guides);
                    guideAdapter.updateData(guideList);
                });

            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(guideActivity.this, "Ошибка загрузки гайдов: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                e.printStackTrace();
            }
        }).start();
    }

    private String sendGetRequest(String accessToken, String refreshToken) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(API_URL).openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Content-Type", "application/json; utf-8");
        connection.setRequestProperty("Authorization", "Bearer " + accessToken);

        int responseCode = connection.getResponseCode();

        if (responseCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
            // Токен устарел → делаем запрос на бек /refresh
            connection.disconnect();
            if (utils.refreshAccessToken(guideActivity.this, refreshToken)) {
                // Читаем новые токены из SharedPreferences
                SharedPreferences prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
                String newAccess = prefs.getString("access_token", null);
                String newRefresh = prefs.getString("refresh_token", null);

                // Обновляем переменные класса
                guideActivity.this.accessToken = newAccess;
                guideActivity.this.refreshToken = newRefresh;

                // Повторяем исходный запрос с новым токеном
                return sendGetRequest(newAccess, newRefresh);
            }
            else
            {
                // Сессия истекла
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        SharedPreferences prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
                        SharedPreferences.Editor editor = prefs.edit();
                        editor.remove("access_token");
                        editor.remove("refresh_token");
                        editor.apply();
                        Toast.makeText(guideActivity.this, "Сессия истекла. Войдите снова", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(guideActivity.this, loginActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    }
                });
                return null;
            }
        }

        if (responseCode != HttpURLConnection.HTTP_OK) {
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
            StringBuilder errorResponse = new StringBuilder();
            String line;
            while ((line = errorReader.readLine()) != null) {
                errorResponse.append(line);
            }
            errorReader.close();
            throw new Exception("Ошибка получения данных: " + errorResponse.toString());
        }

        // Читаем успешный ответ
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

    private List<guide> parseGuidesFromJson(JSONArray jsonArray) throws JSONException {
        List<guide> guides = new ArrayList<>();

        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject guideJson = jsonArray.getJSONObject(i);

            int id = guideJson.getInt("id");
            String header = guideJson.getString("header");
            String body = guideJson.getString("body");
            String date = utils.changeDate(guideJson.getString("date"));

            // Парсим массив изображений
            List<String> imagePaths = new ArrayList<>();
            if (guideJson.has("tutor_path")) {
                JSONArray imagesArray = guideJson.getJSONArray("tutor_path");
                for (int j = 0; j < imagesArray.length(); j++) {
                    imagePaths.add(imagesArray.getString(j));
                }
            }

            guides.add(new guide(id, header, body, date, imagePaths));
        }

        return guides;
    }
}
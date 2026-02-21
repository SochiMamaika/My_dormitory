package com.example.mydormitory;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.AdapterView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class avitostanActivity extends AppCompatActivity
{
    ImageButton menuButton, addAvitoButton;
    Spinner categoryAvitostanView;
    private RecyclerView avitostanRecyclerView;
    private avitostanAdapter avitostanAdapter;
    private List<avitostan> avitostanList = new ArrayList<>();
    private List<avitostan> allAvitostans = new ArrayList<>();
    private static final String API_URL = "http://10.0.2.2:3000/thing";
    private String accessToken;
    private String refreshToken;
    private int user_id;
    private boolean hasAvitostanWriteRole = false;
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.avitostan);
        // Получаем токены из SharedPreferences
        SharedPreferences prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
        accessToken = prefs.getString("access_token", null);
        refreshToken = prefs.getString("refresh_token", null);
        user_id = utils.getUserIdFromToken(this, accessToken, refreshToken);

        // Проверяем авторизацию
        if (accessToken == null) {
            Toast.makeText(this, "Пользователь не авторизован", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, loginActivity.class));
            finish();
            return;
        }

        hasAvitostanWriteRole = utils.hasRole(this, accessToken, refreshToken, "thing_write");
        menuButton = findViewById(R.id.menuButton);
        addAvitoButton = findViewById(R.id.addAvitoButton);
        avitostanRecyclerView = findViewById(R.id.avitostanList);
        categoryAvitostanView = findViewById(R.id.categoryAvitostanView);

        // Настройка RecyclerView
        avitostanAdapter = new avitostanAdapter(avitostanList, hasAvitostanWriteRole);

        // Устанавливаем слушатель только если есть права
        if (hasAvitostanWriteRole) {
            avitostanAdapter.setOnAvitostanClickListener((item, position) -> {
                Toast.makeText(avitostanActivity.this, "Удаление объявления: " + item.getBody(), Toast.LENGTH_SHORT).show();
                deleteAvitostanFromServer(item.getId(), position);
            });
        }
        avitostanRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        avitostanRecyclerView.setAdapter(avitostanAdapter);

        // Настройка слушателя для Spinner
        setupSpinnerListener();

        // Загрузка данных с API
        loadAvitostanFromApi();

        menuButton.setOnClickListener(v -> {
            Intent intent = new Intent(avitostanActivity.this, allWidjet.class);
            startActivity(intent);
        });

        addAvitoButton.setOnClickListener(v -> {
            Intent intent = new Intent(avitostanActivity.this, addAvitoActivity.class);
            startActivity(intent);
        });
    }

    private void deleteAvitostanFromServer(int id, int position) {
        new Thread(() -> {
            try {
                boolean success = sendDeleteRequest(id, user_id);
                runOnUiThread(() -> {
                    if (success)
                    {
                        avitostanList.remove(position);
                        avitostanAdapter.notifyItemRemoved(position);
                        Toast.makeText(avitostanActivity.this, "Объявление удалено", Toast.LENGTH_SHORT).show();
                    }
                    else
                    {
                        Toast.makeText(avitostanActivity.this, "Ошибка удаления", Toast.LENGTH_SHORT).show();
                    }
                });
            }
            catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(avitostanActivity.this, "Ошибка: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                e.printStackTrace();
            }
        }).start();
    }

    private boolean sendDeleteRequest(int id, int user_id) throws Exception
    {
        String urlStr = API_URL+ "/" + id + "/" + user_id;
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
                return sendDeleteRequest(id, user_id);
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
                    Toast.makeText(avitostanActivity.this, "Сессия истекла. Войдите снова", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(avitostanActivity.this, loginActivity.class);
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

    private void loadAvitostanFromApi() {
        new Thread(() -> {
            try {
                String response = sendGetRequest(accessToken, refreshToken);
                JSONArray jsonArray = new JSONArray(response);
                final List<avitostan> avitostans = parseAvitostansFromJson(jsonArray);

                runOnUiThread(() -> {
                    avitostanList.clear();
                    avitostanList.addAll(avitostans);
                    avitostanAdapter.notifyDataSetChanged();
                });

            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(avitostanActivity.this, "Ошибка загрузки Обьявлений (Возможно еще нету ни одного обьявления) " + e.getMessage(), Toast.LENGTH_SHORT).show());
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
            if (utils.refreshAccessToken(avitostanActivity.this, refreshToken)) {
                // Читаем новые токены из SharedPreferences
                SharedPreferences prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
                String newAccess = prefs.getString("access_token", null);
                String newRefresh = prefs.getString("refresh_token", null);

                // Обновляем переменные класса
                avitostanActivity.this.accessToken = newAccess;
                avitostanActivity.this.refreshToken = newRefresh;

                // Повторяем исходный запрос с новым токеном
                return sendGetRequest(newAccess, newRefresh);
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
                    Toast.makeText(avitostanActivity.this, "Сессия истекла. Войдите снова", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(avitostanActivity.this, loginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
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

    // Простейший метод фильтрации
    // Метод фильтрации
    private void filterAvitostans(String filterText) {
        List<avitostan> filteredList = new ArrayList<>();

        // Определяем, какой тип искать в зависимости от выбранного текста
        String searchType;
        switch (filterText) {
            case "Отдам":
                searchType = "Отдам вещь";
                break;
            case "Возьму":
                searchType = "Возьму вещь";
                break;
            case "Потеряшки":
                searchType = "Нашел вещь";
                break;
            default:
                searchType = filterText;
        }

        // Фильтруем по типу
        for (avitostan item : allAvitostans) {
            if (item.getType() != null && item.getType().equals(searchType)) {
                filteredList.add(item);
            }
        }

        // Обновляем список в адаптере
        avitostanList.clear();
        avitostanList.addAll(filteredList);
        avitostanAdapter.notifyDataSetChanged();
    }

    // Настройка слушателя выбора в Spinner
    private void setupSpinnerListener() {
        categoryAvitostanView.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // Получаем выбранный текст
                String selectedCategory = parent.getItemAtPosition(position).toString();

                if (position == 0)
                {
                    showAllAvitostans();
                }
                else
                {
                    filterAvitostans(selectedCategory);
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Ничего не выбрано - ничего не делаем
            }
        });
    }

    // Метод показа всех объявлений
    private void showAllAvitostans() {
        avitostanList.clear();
        avitostanList.addAll(allAvitostans);
        avitostanAdapter.notifyDataSetChanged();
    }
    private List<avitostan> parseAvitostansFromJson(JSONArray jsonArray) throws JSONException {
        List<avitostan> avitostans = new ArrayList<>();

        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject guideJson = jsonArray.getJSONObject(i);

            int id = guideJson.getInt("id");
            String type = guideJson.getString("type");
            String body = guideJson.getString("body");
            int room = guideJson.getInt("room");
            String date = utils.changeDate(guideJson.getString("date"));

            // Парсим массив изображений
            List<String> imagePaths = new ArrayList<>();
            if (guideJson.has("files_path")) {
                JSONArray imagesArray = guideJson.getJSONArray("files_path");
                for (int j = 0; j < imagesArray.length(); j++) {
                    imagePaths.add(imagesArray.getString(j));
                }
            }
            avitostans.add(new avitostan(id, type, body, room, date, imagePaths));
        }
        // Сохраняем ВСЕ данные в отдельный список
        allAvitostans.clear();
        allAvitostans.addAll(avitostans);
        return avitostans;
    }

}

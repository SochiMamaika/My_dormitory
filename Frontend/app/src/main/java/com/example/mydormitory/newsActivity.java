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

public class newsActivity extends AppCompatActivity
{
    ImageButton menuButton, addNewsButton;

    private RecyclerView newsRecyclerView;
    private newsAdapter newsAdapter;
    private List<news> newsList = new ArrayList<>();
    private static final String API_URL = "http://10.0.2.2:3000/news/";
    private static final String API_DELETE = "http://10.0.2.2:3000/news/";
    private static final int NEWS_LIMIT = 50; // лимит новостей
    private String userType;
    private String accessToken;
    private String refreshToken;
    private int user_id;
    private boolean hasNewsWriteRole = false;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.news);
        SharedPreferences prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
        accessToken = prefs.getString("access_token", null);
        refreshToken = prefs.getString("refresh_token", null);
        userType = prefs.getString("type", null);
        user_id = utils.getUserIdFromToken(this, accessToken, refreshToken);

        if (accessToken == null)
        {
            Toast.makeText(this, "Пользователь не авторизован", Toast.LENGTH_SHORT).show();
            // Пользователь не авторизован
            startActivity(new Intent(this, loginActivity.class));
            finish();
            return;
        }

        hasNewsWriteRole = utils.hasRole(this, accessToken, refreshToken, "news_write");

        menuButton = findViewById(R.id.menuButton);
        addNewsButton = findViewById(R.id.addNewsButton);
        newsRecyclerView = findViewById(R.id.newsList);

        // Настройка RecyclerView
        newsAdapter = new newsAdapter(newsList, hasNewsWriteRole);

        if (hasNewsWriteRole) {
            newsAdapter.setOnNewsClickListener((newsItem, position) -> {
                Toast.makeText(newsActivity.this, "Удаление... " + newsItem.getHeader(), Toast.LENGTH_SHORT).show();
                deleteNewsFromServer(newsItem.getId(), position);
            });
        }

        TextInputEditText searchNews = findViewById(R.id.searchNews);
        searchNews.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                newsAdapter.getFilter().filter(s);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        newsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        newsRecyclerView.setAdapter(newsAdapter);

        // Загрузка данных с API
        loadNewsFromApi();

        menuButton.setOnClickListener(v -> {
            Intent intent = new Intent (newsActivity.this, allWidjet.class);
            startActivity(intent);
        });

        // Показываем/скрываем кнопку добавления в зависимости от роли
        if (hasNewsWriteRole) {
            addNewsButton.setVisibility(View.VISIBLE);
            addNewsButton.setOnClickListener(v -> {
                Intent intent = new Intent(newsActivity.this, addNewsActivity.class);
                startActivity(intent);
            });
        }
        else {
            addNewsButton.setVisibility(View.INVISIBLE);
        }
    }

    private void deleteNewsFromServer(int newsId, int position)
    {
        new Thread(() -> {
            try {
                boolean success = deleteFromServer(accessToken,
                                                   refreshToken,
                                                   newsId,
                                                   user_id);

                runOnUiThread(() -> {
                    if (success)
                    {
                        newsList.remove(position);
                        newsAdapter.notifyItemRemoved(position);
                        Toast.makeText(newsActivity.this, "Новость удалена", Toast.LENGTH_SHORT).show();
                    }
                    else
                    {
                        Toast.makeText(newsActivity.this, "Ошибка удаления", Toast.LENGTH_SHORT).show();
                    }
                });

            }
            catch (Exception e) {
                runOnUiThread(() ->
                        Toast.makeText(newsActivity.this, "Ошибка: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
                e.printStackTrace();
            }
        }).start();
    }

    private boolean deleteFromServer(String accessToken,
                                    String refreshToken,
                                    int newsId,
                                    int userId) throws Exception
    {
        String url = API_DELETE + newsId + "/" + userId;

        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();

        connection.setRequestMethod("DELETE");
        connection.setRequestProperty("Authorization", "Bearer " + accessToken);
        connection.setRequestProperty("Content-Type", "application/json");

        int responseCode = connection.getResponseCode();

        if (responseCode == HttpURLConnection.HTTP_UNAUTHORIZED)
        {
            connection.disconnect();
            if (utils.refreshAccessToken(this, refreshToken))
            {
                SharedPreferences prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
                String newAccess = prefs.getString("access_token", null);
                String newRefresh = prefs.getString("refresh_token", null);

                this.accessToken = newAccess;
                this.refreshToken = newRefresh;
                return deleteFromServer(newAccess, newRefresh, newsId, userId);
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

                    Toast.makeText(newsActivity.this, "Сессия истекла. Войдите снова", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(newsActivity.this, loginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                });
                return false;
            }
        }
        return responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_NO_CONTENT;
    }


    private void loadNewsFromApi() {
        new Thread(() -> {
            try {
                String response = sendGetRequest(accessToken, refreshToken, NEWS_LIMIT, userType);
                JSONArray jsonArray = new JSONArray(response);
                final List<news> news = parseNewsFromJson(jsonArray);

                runOnUiThread(() -> {
                    newsList.clear();
                    newsList.addAll(news);
                    newsAdapter.updateData(newsList);
                });

            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(newsActivity.this, "Ошибка загрузки Новостей: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                e.printStackTrace();
            }
        }).start();
    }

    private String sendGetRequest(String accessToken, String refreshToken, int limit, String userType) throws Exception {
        // Добавляем лимит в URL
        String urlWithLimit = API_URL + limit + "/" + userType;
        HttpURLConnection connection = (HttpURLConnection) new URL(urlWithLimit).openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Content-Type", "application/json; utf-8");
        connection.setRequestProperty("Authorization", "Bearer " + accessToken);

        int responseCode = connection.getResponseCode();

        if (responseCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
            connection.disconnect();
            if (utils.refreshAccessToken(newsActivity.this, refreshToken))
            {
                // Читаем новые токены из SharedPreferences
                SharedPreferences prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
                String newAccess = prefs.getString("access_token", null);
                String newRefresh = prefs.getString("refresh_token", null);

                // Обновляем переменные класса
                newsActivity.this.accessToken = newAccess;
                newsActivity.this.refreshToken = newRefresh;

                // Повторяем исходный запрос с новым токеном
                return sendGetRequest(newAccess, newRefresh, limit, userType);
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

                    Toast.makeText(newsActivity.this,
                            "Сессия истекла. Войдите снова",
                            Toast.LENGTH_SHORT).show();

                    Intent intent = new Intent(newsActivity.this, loginActivity.class);
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

    private List<news> parseNewsFromJson(JSONArray jsonArray) throws JSONException {
        List<news> news = new ArrayList<>();

        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject guideJson = jsonArray.getJSONObject(i);

            int id = guideJson.getInt("id");
            String header = guideJson.getString("header");
            String body = guideJson.getString("body");
            String author = guideJson.getString("author");
            String date = utils.changeDate(guideJson.getString("date"));
            String dateStart = utils.formatDate(guideJson.getString("date_start"));
            String dateEnd = utils.formatDate(guideJson.getString("date_end"));

            // Парсим массив изображений
            List<String> imagePaths = new ArrayList<>();
            if (guideJson.has("news_path")) {
                JSONArray imagesArray = guideJson.getJSONArray("news_path");
                for (int j = 0; j < imagesArray.length(); j++) {
                    imagePaths.add(imagesArray.getString(j));
                }
            }

            news.add(new news(id, header, body, author, date, dateStart, dateEnd, imagePaths));
        }

        return news;
    }
}
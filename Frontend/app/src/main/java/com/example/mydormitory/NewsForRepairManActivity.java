package com.example.mydormitory;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class NewsForRepairManActivity extends AppCompatActivity implements
        NewsForRepairManAdapter.OnRepairButtonClickListener,
        AdapterView.OnItemSelectedListener  // Добавляем слушатель для Spinner
{
    private RecyclerView newsRecyclerView;
    private NewsForRepairManAdapter newsAdapter;
    private Spinner professionSpinner;

    private List<newsforrepairman> newsList = new ArrayList<>(); // Отфильтрованный список
    private List<newsforrepairman> allFreeNews = new ArrayList<>(); // Все свободные заказы

    private static final String API_URL = "http://10.0.2.2:3000/news/";
    private static final String UPDATE_STATUS_URL = "http://10.0.2.2:3000/activaterepair";
    private static final int NEWS_LIMIT = 50;

    private String accessToken;
    private String refreshToken;
    private String userType;
    private int currentUserId;
    private String currentProfession = "Все профессии";

    ImageButton allWidjetForRepairBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.newsforrepairman);

        SharedPreferences prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
        accessToken = prefs.getString("access_token", null);
        refreshToken = prefs.getString("refresh_token", null);
        userType = prefs.getString("type", null);
        currentUserId = utils.getUserIdFromToken(this, accessToken, refreshToken);

        allWidjetForRepairBtn = findViewById(R.id.allWidjetForRepairBtn);
        newsRecyclerView = findViewById(R.id.newsListForRepairman);
        professionSpinner = findViewById(R.id.professionSpinner);

        if (accessToken == null)
        {
            Toast.makeText(this, "Пользователь не авторизован", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, loginActivity.class));
            finish();
            return;
        }

        // Настройка Spinner
        ArrayAdapter<CharSequence> spinnerAdapter = ArrayAdapter.createFromResource(this,
                R.array.professions_array, android.R.layout.simple_spinner_item);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        professionSpinner.setAdapter(spinnerAdapter);
        professionSpinner.setOnItemSelectedListener(this);

        // Настройка RecyclerView
        newsAdapter = new NewsForRepairManAdapter(newsList);
        newsAdapter.setOnRepairButtonClickListener(this);
        newsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        newsRecyclerView.setAdapter(newsAdapter);

        allWidjetForRepairBtn.setOnClickListener(v -> {
            Intent intent = new Intent(NewsForRepairManActivity.this, allWidjetForRepairman.class);
            startActivity(intent);
            finish();
        });

        // Загрузка данных с API
        loadNewsFromApi();
    }

    @Override
    public void onRepairButtonClick(int position, newsforrepairman news) {
        boolean newStatus = !news.getActivity();

        if (newStatus) {
            updateRepairStatusOnServer(news.getId(), accessToken, newStatus, position, currentUserId);
            Toast.makeText(this, "Заказ взят!", Toast.LENGTH_SHORT).show();
        }
    }

    // Обработка выбора в Spinner
    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        currentProfession = parent.getItemAtPosition(position).toString();
        filterRepairsByProfession();
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        // Ничего не делаем
    }

    // Фильтрация заказов по профессии
    private void filterRepairsByProfession() {
        List<newsforrepairman> filteredList = new ArrayList<>();

        if (currentProfession.equals("Все профессии")) {
            filteredList.addAll(allFreeNews);
        } else {
            for (newsforrepairman repair : allFreeNews) {
                if (repair.getType().equals(currentProfession)) {
                    filteredList.add(repair);
                }
            }
        }

        newsList.clear();
        newsList.addAll(filteredList);
        newsAdapter.notifyDataSetChanged();
    }

    private void updateRepairStatusOnServer(int repairId, String access, boolean newStatus, int position, int userId) {
        final int safePosition = position;
        new Thread(() -> {
            try {
                sendPatchRequest(UPDATE_STATUS_URL, access, repairId, newStatus, userId);

                runOnUiThread(() -> {
                    // Удаляем из обоих списков
                    if (safePosition >= 0 && safePosition < newsList.size()) {
                        newsforrepairman removed = newsList.remove(safePosition);
                        allFreeNews.remove(removed);
                        newsAdapter.notifyItemRemoved(safePosition);
                        Toast.makeText(NewsForRepairManActivity.this,
                                "Заказ взят и перемещён в «Мои заказы»", Toast.LENGTH_SHORT).show();
                    } else {
                        loadNewsFromApi();
                    }
                });

            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(NewsForRepairManActivity.this,
                        "Ошибка: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                loadNewsFromApi();
            }
        }).start();
    }

    private void sendPatchRequest(String urlString, String accessToken, int repairId, boolean newStatus, int userId) throws Exception {
        JSONObject jsonBody = new JSONObject();
        jsonBody.put("repair_id", repairId);
        jsonBody.put("activity", newStatus);
        jsonBody.put("user_id", userId);

        HttpURLConnection connection = (HttpURLConnection) new URL(urlString).openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json; utf-8");
        connection.setRequestProperty("Authorization", "Bearer " + accessToken);
        connection.setDoOutput(true);

        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(connection.getOutputStream(), "UTF-8")))
        {
            writer.write(jsonBody.toString());
        }

        int responseCode = connection.getResponseCode();

        if (responseCode == HttpURLConnection.HTTP_UNAUTHORIZED)
        {
            connection.disconnect();
            if (utils.refreshAccessToken(NewsForRepairManActivity.this, refreshToken))
            {
                SharedPreferences prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
                String newAccess = prefs.getString("access_token", null);
                String newRefresh = prefs.getString("refresh_token", null);
                NewsForRepairManActivity.this.accessToken = newAccess;
                NewsForRepairManActivity.this.refreshToken = newRefresh;
                sendPatchRequest(urlString, newAccess, repairId, newStatus, userId);
                return;
            }
            else
            {
                handleSessionExpired();
                return;
            }
        }

        if (responseCode != HttpURLConnection.HTTP_OK && responseCode != HttpURLConnection.HTTP_CREATED)
        {
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
            StringBuilder errorResponse = new StringBuilder();
            String line;
            while ((line = errorReader.readLine()) != null)
            {
                errorResponse.append(line);
            }
            errorReader.close();
            throw new Exception("Ошибка отправки данных: " + errorResponse.toString());
        }
        connection.disconnect();
    }

    private void loadNewsFromApi() {
        new Thread(() -> {
            try {
                String response = sendGetRequest(accessToken, refreshToken, NEWS_LIMIT, userType);
                JSONArray jsonArray = new JSONArray(response);
                List<newsforrepairman> allNews = utils.parseNewsFromJson(jsonArray);

                List<newsforrepairman> freeNews = new ArrayList<>();
                for (newsforrepairman n : allNews) {
                    if (!n.getActivity()) {
                        freeNews.add(n);
                    }
                }

                runOnUiThread(() -> {
                    allFreeNews.clear();
                    allFreeNews.addAll(freeNews);

                    // Применяем текущую фильтрацию
                    filterRepairsByProfession();
                });

            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(NewsForRepairManActivity.this,
                        "Ошибка загрузки: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                e.printStackTrace();
            }
        }).start();
    }

    private String sendGetRequest(String accessToken, String refreshToken, int limit, String userType) throws Exception {
        String urlWithLimit = API_URL + limit + "/" + userType;
        HttpURLConnection connection = (HttpURLConnection) new URL(urlWithLimit).openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Content-Type", "application/json; utf-8");
        connection.setRequestProperty("Authorization", "Bearer " + accessToken);

        int responseCode = connection.getResponseCode();

        if (responseCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
            connection.disconnect();
            if (utils.refreshAccessToken(NewsForRepairManActivity.this, refreshToken))
            {
                SharedPreferences prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
                String newAccess = prefs.getString("access_token", null);
                String newRefresh = prefs.getString("refresh_token", null);
                NewsForRepairManActivity.this.accessToken = newAccess;
                NewsForRepairManActivity.this.refreshToken = newRefresh;
                return sendGetRequest(newAccess, newRefresh, limit, userType);
            }
            else
            {
                handleSessionExpired();
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

    private void handleSessionExpired() {
        runOnUiThread(() -> {
            SharedPreferences prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.remove("access_token");
            editor.remove("refresh_token");
            editor.apply();

            Toast.makeText(NewsForRepairManActivity.this,
                    "Сессия истекла. Войдите снова", Toast.LENGTH_SHORT).show();

            Intent intent = new Intent(NewsForRepairManActivity.this, loginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }
}
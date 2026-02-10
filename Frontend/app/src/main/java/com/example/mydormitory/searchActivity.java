package com.example.mydormitory;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
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

public class searchActivity extends AppCompatActivity
{
    ImageButton menuButton;

    private RecyclerView userRecyclerView;
    private userAdapter userAdapter;
    private List<user> usersList = new ArrayList<>();
    private static final String API_URL = "http://10.0.2.2:3000/users";
    private static final String API = "http://10.0.2.2:3000/user";
    private String userType;
    private String accessToken;
    private String refreshToken;
    private int user_id;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.search);
        SharedPreferences prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
        accessToken = prefs.getString("access_token", null);
        refreshToken = prefs.getString("refresh_token", null);
        userType = prefs.getString("type", null);
        user_id = utils.getUserIdFromToken(this, accessToken, refreshToken);

        if (accessToken == null)
        {
            Toast.makeText(this, "Пользователь не авторизован", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, loginActivity.class));
            finish();
            return;
        }

        menuButton = findViewById(R.id.menuButton);
        userRecyclerView = findViewById(R.id.usersList);

        userRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        userAdapter = new userAdapter(usersList);
        userRecyclerView.setAdapter(userAdapter);

        userAdapter.setOnItemClickListener(user -> {
            Intent intent = new Intent(searchActivity.this, UserDetailActivity.class);
            intent.putExtra("user_id", user.getId());
            startActivity(intent);
        });

        TextInputEditText searchInput = findViewById(R.id.searchInput);
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                userAdapter.getFilter().filter(s);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Загрузка данных с API
        loadUsersFromApi();

        menuButton.setOnClickListener(v -> {
            Intent intent = new Intent (searchActivity.this, allWidjet.class);
            startActivity(intent);
        });
    }

    private void loadUsersFromApi() {
        new Thread(() -> {
            try {
                String response = sendGetRequest(accessToken, refreshToken, API_URL, userType);
                JSONArray jsonArray = new JSONArray(response);
                final List<user> users = parseUsersFromJson(jsonArray);

                runOnUiThread(() -> {
                    userAdapter.updateData(users);
                });

            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(searchActivity.this,
                        "Ошибка загрузки пользователей: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                e.printStackTrace();
            }
        }).start();
    }

    private String sendGetRequest(String accessToken, String refreshToken, String API_URL, String userType) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(API_URL).openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Content-Type", "application/json; utf-8");
        connection.setRequestProperty("Authorization", "Bearer " + accessToken);

        int responseCode = connection.getResponseCode();

        if (responseCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
            connection.disconnect();
            if (utils.refreshAccessToken(searchActivity.this, refreshToken))
            {
                SharedPreferences prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
                String newAccess = prefs.getString("access_token", null);
                String newRefresh = prefs.getString("refresh_token", null);

                searchActivity.this.accessToken = newAccess;
                searchActivity.this.refreshToken = newRefresh;

                return sendGetRequest(newAccess, newRefresh, API_URL, userType);
            }
            else
            {
                runOnUiThread(() -> {
                    SharedPreferences prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.remove("access_token");
                    editor.remove("refresh_token");
                    editor.apply();

                    Toast.makeText(searchActivity.this, "Сессия истекла. Войдите снова", Toast.LENGTH_SHORT).show();

                    Intent intent = new Intent(searchActivity.this, loginActivity.class);
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

    private List<user> parseUsersFromJson(JSONArray jsonArray) throws JSONException {
        List<user> users = new ArrayList<>();

        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject userJson = jsonArray.getJSONObject(i);

            int id = userJson.getInt("id");
            String phone_number = userJson.getString("phone_number");
            String name = userJson.getString("name");
            String last_name = userJson.getString("last_name");
            String surname = userJson.getString("surname");

            // Роли из токена (если нужно из API, то парсите из JSON)
            List<String> roles = new ArrayList<>();
            if (userJson.has("roles")) {
                JSONArray imagesArray = userJson.getJSONArray("roles");
                for (int j = 0; j < imagesArray.length(); j++) {
                    roles.add(imagesArray.getString(j));
                }
            }

            // Документы (если есть)
            List<String> imagePaths = new ArrayList<>();
            if (userJson.has("document_path")) {
                JSONArray imagesArray = userJson.getJSONArray("document_path");
                for (int j = 0; j < imagesArray.length(); j++) {
                    imagePaths.add(imagesArray.getString(j));
                }
            }

            users.add(new user(id, phone_number, name, last_name, surname, roles, imagePaths));
        }

        return users;
    }
}
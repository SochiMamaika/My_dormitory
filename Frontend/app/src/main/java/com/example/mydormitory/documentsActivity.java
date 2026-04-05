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

public class documentsActivity extends AppCompatActivity
{
    ImageButton menuButton, addDocumentButton;
    private RecyclerView documentsRecyclerView;
    private documentsAdapter documentsAdapter;
    private List<documents> documentsList = new ArrayList<>();
    private static final String API_URL = "http://10.0.2.2:3000/file";
    private static final String DELETE_API = "http://10.0.2.2:3000/file/";
    private String accessToken;
    private String refreshToken;
    private boolean hasDocumentsWriteRole = false;
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.documents);
        SharedPreferences prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
        accessToken = prefs.getString("access_token", null);
        refreshToken = prefs.getString("refresh_token", null);

        if (accessToken == null)
        {
            Toast.makeText(this, "Пользователь не авторизован", Toast.LENGTH_SHORT).show();
            // Пользователь не авторизован
            startActivity(new Intent(this, loginActivity.class));
            finish();
            return;
        }

        hasDocumentsWriteRole = utils.hasRole(this, accessToken, refreshToken, "file_write");

        menuButton = findViewById(R.id.menuButton);
        addDocumentButton = findViewById(R.id.addDocumentButton);
        documentsRecyclerView = findViewById(R.id.documentsList);

        // Настройка RecyclerView
        documentsAdapter = new documentsAdapter(documentsList, this, hasDocumentsWriteRole);

        // Устанавливаем слушатель только если есть права
        if (hasDocumentsWriteRole) {
            documentsAdapter.setOnDocumentClickListener((document, position) -> {
                Toast.makeText(documentsActivity.this, "Удаление документа...", Toast.LENGTH_SHORT).show();
                deleteDocumentFromServer(document.getId(), position);
            });
        }

        documentsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        documentsRecyclerView.setAdapter(documentsAdapter);

        TextInputEditText searchDoc = findViewById(R.id.searchDoc);
        searchDoc.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                documentsAdapter.getFilter().filter(s);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Загрузка данных с API
        loadDocumentsFromApi();

        menuButton.setOnClickListener(v -> {
            Intent intent = new Intent (documentsActivity.this, allWidjet.class);
            startActivity(intent);
        });

        // Показываем/скрываем кнопку добавления в зависимости от роли
        if (hasDocumentsWriteRole) {
            addDocumentButton.setVisibility(View.VISIBLE);
            addDocumentButton.setOnClickListener(v -> {
                Intent intent = new Intent (documentsActivity.this, addDocumentsActivity.class);
                startActivity(intent);
            });
        }
        else {
            addDocumentButton.setVisibility(View.GONE);
        }
    }

    private void deleteDocumentFromServer(int fileId, int position) {
        new Thread(() -> {
            try {
                boolean success = deleteFromServer(accessToken,
                                                   refreshToken,
                                                   fileId);
                runOnUiThread(() -> {
                    if (success)
                    {
                        documentsList.remove(position);
                        documentsAdapter.updateData(documentsList);

                        Toast.makeText(documentsActivity.this, "Документ удалён", Toast.LENGTH_SHORT).show();
                    }
                    else
                    {
                        Toast.makeText(documentsActivity.this, "Ошибка удаления", Toast.LENGTH_SHORT).show();
                    }
                });

            }
            catch (Exception e) {
                runOnUiThread(() ->
                        Toast.makeText(documentsActivity.this, "Ошибка: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
            }
        }).start();
    }

    private boolean deleteFromServer(String accessToken,
                                     String refreshToken,
                                     int fileId) throws Exception
    {
        String url = DELETE_API + fileId;

        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod("DELETE");
        connection.setRequestProperty("Authorization", "Bearer " + accessToken);

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
                return deleteFromServer(newAccess, newRefresh, fileId);
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

                    Toast.makeText(documentsActivity.this, "Сессия истекла. Войдите снова", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(documentsActivity.this, loginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                });
                return false;
            }
        }
        return responseCode == HttpURLConnection.HTTP_OK  || responseCode == HttpURLConnection.HTTP_NO_CONTENT;
    }

    private void loadDocumentsFromApi() {
        new Thread(() -> {
            try {
                String response = sendGetRequest(accessToken, refreshToken);
                JSONArray jsonArray = new JSONArray(response);
                final List<documents> documents = parseNewsFromJson(jsonArray);

                runOnUiThread(() -> {
                    documentsList.clear();
                    documentsList.addAll(documents);
                    documentsAdapter.updateData(documentsList);
                });

            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(documentsActivity.this, "Ошибка загрузки Документов: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                e.printStackTrace();
            }
        }).start();
    }

    private String sendGetRequest(String accessToken, String refreshToken) throws Exception {
        // Добавляем лимит в URL
        String url = API_URL;
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Content-Type", "application/json; utf-8");
        connection.setRequestProperty("Authorization", "Bearer " + accessToken);

        int responseCode = connection.getResponseCode();

        if (responseCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
            connection.disconnect();
            if (utils.refreshAccessToken(documentsActivity.this, refreshToken))
            {
                // Читаем новые токены из SharedPreferences
                SharedPreferences prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
                String newAccess = prefs.getString("access_token", null);
                String newRefresh = prefs.getString("refresh_token", null);

                // Обновляем переменные класса
                documentsActivity.this.accessToken = newAccess;
                documentsActivity.this.refreshToken = newRefresh;

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

                    Toast.makeText(documentsActivity.this,
                            "Сессия истекла. Войдите снова",
                            Toast.LENGTH_SHORT).show();

                    Intent intent = new Intent(documentsActivity.this, loginActivity.class);
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

    private List<documents> parseNewsFromJson(JSONArray jsonArray) throws JSONException {
        List<documents> documents = new ArrayList<>();

        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject guideJson = jsonArray.getJSONObject(i);

            int id = guideJson.getInt("id");
            String body = guideJson.getString("body");
            String date = utils.changeDate(guideJson.getString("date"));

            // Парсим массив документов
            List<String> files_path = new ArrayList<>();
            if (guideJson.has("files_path")) {
                JSONArray fileArray = guideJson.getJSONArray("files_path");
                for (int j = 0; j < fileArray.length(); j++) {
                    files_path.add(fileArray.getString(j));
                }
            }

            documents.add(new documents(id, body, date, files_path));
        }

        return documents;
    }
}
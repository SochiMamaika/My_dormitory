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

public class MyRepairsActivity extends AppCompatActivity implements
        NewsForRepairManAdapter.OnRepairButtonClickListener,
        NewsForRepairManAdapter.OnDeleteButtonClickListener,
        AdapterView.OnItemSelectedListener  // Добавляем слушатель для Spinner
{
    private RecyclerView recyclerView;
    private NewsForRepairManAdapter adapter;
    private ImageButton allWidjetForRepairBtn;
    private Spinner professionSpinner;

    private List<newsforrepairman> repairs = new ArrayList<>();
    private List<newsforrepairman> allRepairs = new ArrayList<>(); // Полный список всех заказов

    private static final String MY_REPAIRS_URL = "http://10.0.2.2:3000/myrepair";
    private static final String REPAIRS_DELETE = "http://10.0.2.2:3000/repair/";
    private static final String UPDATE_STATUS_URL = "http://10.0.2.2:3000/activaterepair";

    private String accessToken;
    private String refreshToken;
    private int currentUserId;

    private String currentProfession = "Все профессии"; // Текущая выбранная профессия

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.mynewsforrepairman);

        SharedPreferences prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);

        accessToken = prefs.getString("access_token", null);
        refreshToken = prefs.getString("refresh_token", null);
        currentUserId = utils.getUserIdFromToken(this, accessToken, refreshToken);

        if (accessToken == null) {
            startActivity(new Intent(this, loginActivity.class));
            finish();
            return;
        }

        allWidjetForRepairBtn = findViewById(R.id.allWidjetForRepairBtn);
        recyclerView = findViewById(R.id.newsListForRepairman);
        professionSpinner = findViewById(R.id.professionSpinner);

        // Настройка Spinner
        ArrayAdapter<CharSequence> spinnerAdapter = ArrayAdapter.createFromResource(this,
                R.array.professions_array, android.R.layout.simple_spinner_item);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        professionSpinner.setAdapter(spinnerAdapter);
        professionSpinner.setOnItemSelectedListener(this);

        adapter = new NewsForRepairManAdapter(repairs);
        adapter.setOnRepairButtonClickListener(this);
        adapter.setOnDeleteButtonClickListener(this);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        allWidjetForRepairBtn.setOnClickListener(v -> {
            Intent intent = new Intent(MyRepairsActivity.this, allWidjetForRepairman.class);
            startActivity(intent);
            finish();
        });

        loadMyRepairs();
    }

    @Override
    public void onRepairButtonClick(int position, newsforrepairman news) {
        cancelRepair(news.getId(), position);
    }

    @Override
    public void onDeleteButtonClick(int position, newsforrepairman news) {
        deleteRepair(news.getId(), position);
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
            filteredList.addAll(allRepairs);
        } else {
            for (newsforrepairman repair : allRepairs) {
                if (repair.getType().equals(currentProfession)) {
                    filteredList.add(repair);
                }
            }
        }

        repairs.clear();
        repairs.addAll(filteredList);
        adapter.notifyDataSetChanged();
    }

    private void cancelRepair(int repairId, int position) {
        final int finalPosition = position;
        new Thread(() -> {
            try {
                sendPatchRequest(repairId, false, currentUserId);

                runOnUiThread(() -> {
                    if (finalPosition >= 0 && finalPosition < repairs.size()) {
                        // Удаляем из обоих списков
                        newsforrepairman removed = repairs.remove(finalPosition);
                        allRepairs.remove(removed);
                        adapter.notifyItemRemoved(finalPosition);
                        Toast.makeText(this, "Заказ отменён", Toast.LENGTH_SHORT).show();
                    } else {
                        loadMyRepairs();
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() ->
                        Toast.makeText(this, "Ошибка отмены", Toast.LENGTH_SHORT).show()
                );
            }
        }).start();
    }

    private void deleteRepair(int repairId, int position) {
        final int finalPosition = position;
        new Thread(() -> {
            try {
                boolean success = sendDeleteRequest(repairId);

                runOnUiThread(() -> {
                    if (success) {
                        if (finalPosition >= 0 && finalPosition < repairs.size()) {
                            // Удаляем из обоих списков
                            newsforrepairman removed = repairs.remove(finalPosition);
                            allRepairs.remove(removed);
                            adapter.notifyItemRemoved(finalPosition);
                            Toast.makeText(this, "Заказ удалён", Toast.LENGTH_SHORT).show();
                        } else {
                            loadMyRepairs();
                        }
                    } else {
                        Toast.makeText(this, "Ошибка удаления", Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() ->
                        Toast.makeText(this, "Ошибка: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
            }
        }).start();
    }

    private boolean sendDeleteRequest(int repairId) throws Exception {
        String url = REPAIRS_DELETE + repairId + "/" + currentUserId;
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();

        conn.setRequestMethod("DELETE");
        conn.setRequestProperty("Authorization", "Bearer " + accessToken);

        int responseCode = conn.getResponseCode();

        if (responseCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
            conn.disconnect();
            if (utils.refreshAccessToken(this, refreshToken)) {
                SharedPreferences prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
                accessToken = prefs.getString("access_token", null);
                refreshToken = prefs.getString("refresh_token", null);
                return sendDeleteRequest(repairId);
            } else {
                handleSessionExpired();
                return false;
            }
        }

        return responseCode == HttpURLConnection.HTTP_OK ||
                responseCode == HttpURLConnection.HTTP_NO_CONTENT;
    }

    private void loadMyRepairs() {
        new Thread(() -> {
            try {
                String response = sendGetRequest();
                JSONArray array = new JSONArray(response);
                List<newsforrepairman> list = utils.parseNewsFromJson(array);

                List<newsforrepairman> myRepairs = new ArrayList<>();

                for (newsforrepairman n : list) {
                    if (n.getActivity() && n.getRepairmanId() == currentUserId) {
                        myRepairs.add(n);
                    }
                }

                runOnUiThread(() -> {
                    allRepairs.clear();
                    allRepairs.addAll(myRepairs);

                    // Применяем текущую фильтрацию
                    filterRepairsByProfession();
                });

            } catch (Exception e) {
                runOnUiThread(() ->
                        Toast.makeText(this, "Ошибка загрузки", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private String sendGetRequest() throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(MY_REPAIRS_URL).openConnection();

        conn.setRequestMethod("GET");
        conn.setRequestProperty("Authorization", "Bearer " + accessToken);

        int responseCode = conn.getResponseCode();

        if (responseCode == 401) {
            conn.disconnect();
            if (utils.refreshAccessToken(MyRepairsActivity.this, refreshToken)) {
                SharedPreferences prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
                String newAccess = prefs.getString("access_token", null);
                String newRefresh = prefs.getString("refresh_token", null);
                MyRepairsActivity.this.accessToken = newAccess;
                MyRepairsActivity.this.refreshToken = newRefresh;
                return sendGetRequest();
            } else {
                handleSessionExpired();
                return null;
            }
        }

        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder result = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            result.append(line);
        }
        reader.close();
        conn.disconnect();

        return result.toString();
    }

    private void sendPatchRequest(int repairId, boolean activity, int repairManId) throws Exception {
        JSONObject json = new JSONObject();
        json.put("repair_id", repairId);
        json.put("activity", activity);
        json.put("user_id", repairManId);

        HttpURLConnection conn = (HttpURLConnection) new URL(UPDATE_STATUS_URL).openConnection();

        conn.setRequestMethod("POST");
        conn.setRequestProperty("Authorization", "Bearer " + accessToken);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);

        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(conn.getOutputStream()));
        writer.write(json.toString());
        writer.close();

        if (conn.getResponseCode() != 200) {
            throw new RuntimeException("Failed");
        }
        conn.disconnect();
    }

    private void handleSessionExpired() {
        runOnUiThread(() -> {
            SharedPreferences prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.remove("access_token");
            editor.remove("refresh_token");
            editor.apply();

            Toast.makeText(MyRepairsActivity.this, "Сессия истекла. Войдите снова", Toast.LENGTH_SHORT).show();

            Intent intent = new Intent(MyRepairsActivity.this, loginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }
}
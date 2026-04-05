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

public class MyOrders extends AppCompatActivity implements
        MyOrdersAdapter.OnDeleteButtonClickListener
{
    private RecyclerView myOrdersList;
    private MyOrdersAdapter adapter;
    private ImageButton allWidjet;
    private String userType;
    private List<newsforrepairman> repairs = new ArrayList<>();
    private static final String MY_REPAIRS_URL = "http://10.0.2.2:3000/myrepair";
    private static final String REPAIRS_DELETE = "http://10.0.2.2:3000/repair/";
    private static final String UPDATE_STATUS_URL = "http://10.0.2.2:3000/endingrepair";

    private String accessToken;
    private String refreshToken;
    private int currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.my_orders);

        SharedPreferences prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);

        accessToken = prefs.getString("access_token", null);
        refreshToken = prefs.getString("refresh_token", null);
        userType = prefs.getString("type", null);
        currentUserId = utils.getUserIdFromToken(this, accessToken, refreshToken);

        if (accessToken == null) {
            startActivity(new Intent(this, loginActivity.class));
            finish();
            return;
        }

        allWidjet = findViewById(R.id.allWidjet);
        myOrdersList = findViewById(R.id.myOrdersList);

        adapter = new MyOrdersAdapter(repairs);
        adapter.setOnDeleteButtonClickListener(this);

        myOrdersList.setLayoutManager(new LinearLayoutManager(this));
        myOrdersList.setAdapter(adapter);

        allWidjet.setOnClickListener(v -> {
            Intent intent = new Intent(MyOrders.this, allWidjet.class);
            startActivity(intent);
            finish();
        });

        loadMyOrders();
    }

    @Override
    public void onDeleteButtonClick(int position, newsforrepairman news) {
        // Студент подтверждает, что ремонт выполнен
        endRepair(news.getId(), position);
    }

    private void endRepair(int repairId, int position) {
        final int finalPosition = position;
        new Thread(() -> {
            try {
                boolean success = sendDeleteRequest(repairId);

                runOnUiThread(() -> {
                    if (success) {
                        if (finalPosition >= 0 && finalPosition < repairs.size()) {
                            // Удаляем из обоих списков
                            newsforrepairman removed = repairs.remove(finalPosition);
                            adapter.notifyItemRemoved(finalPosition);
                            Toast.makeText(this, "Заказ удалён", Toast.LENGTH_SHORT).show();
                        } else {
                            loadMyOrders();
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

    private void loadMyOrders() {
        new Thread(() -> {
            try {
                String response = sendGetRequest();
                JSONArray array = new JSONArray(response);
                List<newsforrepairman> list = utils.parseNewsFromJson(array);

                List<newsforrepairman> myRepairs = new ArrayList<>();

                for (newsforrepairman n : list) {
                    if (n.getUserId() == currentUserId) {
                        myRepairs.add(n);
                    }
                }

                runOnUiThread(() -> {
                    repairs.clear();
                    repairs.addAll(myRepairs);
                    adapter.notifyDataSetChanged();
                });

            } catch (Exception e) {
                runOnUiThread(() ->
                        Toast.makeText(this, "Ошибка загрузки", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private String sendGetRequest() throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(MY_REPAIRS_URL + "/" + userType).openConnection();

        conn.setRequestMethod("GET");
        conn.setRequestProperty("Authorization", "Bearer " + accessToken);

        int responseCode = conn.getResponseCode();

        if (responseCode == 401) {
            conn.disconnect();
            if (utils.refreshAccessToken(MyOrders.this, refreshToken)) {
                SharedPreferences prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
                String newAccess = prefs.getString("access_token", null);
                String newRefresh = prefs.getString("refresh_token", null);
                MyOrders.this.accessToken = newAccess;
                MyOrders.this.refreshToken = newRefresh;
                return sendGetRequest();
            }
            else
            {
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

    private void handleSessionExpired() {
        runOnUiThread(() -> {
            SharedPreferences prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.remove("access_token");
            editor.remove("refresh_token");
            editor.apply();

            Toast.makeText(MyOrders.this, "Сессия истекла. Войдите снова", Toast.LENGTH_SHORT).show();

            Intent intent = new Intent(MyOrders.this, loginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }
}
package com.example.mydormitory;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class allWidjetForRepairman extends AppCompatActivity
{
    private ImageButton openAllOrdersButton, openMyOrdersButton, openMyInfoButton, exitFromRepairman;
    private String accessToken, refreshToken, userType;
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.all_widjet_for_repairman);
        SharedPreferences prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
        accessToken = prefs.getString("access_token", null);
        refreshToken = prefs.getString("refresh_token", null);
        userType = prefs.getString("type", null);
        Toast.makeText(this, "type = " + userType, Toast.LENGTH_SHORT).show();

        if (accessToken == null)
        {
            Toast.makeText(this, "Пользователь не авторизован", Toast.LENGTH_SHORT).show();
            // Пользователь не авторизован
            startActivity(new Intent(this, loginActivity.class));
            finish();
            return;
        }
        openAllOrdersButton = findViewById(R.id.openAllOrdersButton);
        openMyOrdersButton = findViewById(R.id.openMyOrdersButton);
        openMyInfoButton = findViewById(R.id.openMyInfoButton);
        exitFromRepairman = findViewById(R.id.exitFromRepairman);

        openAllOrdersButton.setOnClickListener(v -> {
            Intent intent = new Intent (allWidjetForRepairman.this, NewsForRepairManActivity.class);
            startActivity(intent);
            finish();
        });

        exitFromRepairman.setOnClickListener(v -> {
            SharedPreferences prefs1 = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs1.edit();
            editor.remove("access_token");
            editor.remove("refresh_token");
            editor.apply();
            Intent intent = new Intent (allWidjetForRepairman.this, loginActivity.class);
            startActivity(intent);
            finish();
        });

        openMyOrdersButton.setOnClickListener(v -> {
            Intent intent = new Intent (allWidjetForRepairman.this, MyRepairsActivity.class);
            startActivity(intent);
            finish();
        });

        openMyInfoButton.setOnClickListener(v -> {
            Intent intent = new Intent (allWidjetForRepairman.this, UserDetailActivity.class);
            intent.putExtra("user_id", utils.getUserIdFromToken(this, accessToken, refreshToken));
            intent.putExtra("user_type", "Ремонтник");
            startActivity(intent);
            finish();
        });
    }
}

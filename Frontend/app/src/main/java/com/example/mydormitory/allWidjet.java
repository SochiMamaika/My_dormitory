package com.example.mydormitory;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class allWidjet extends AppCompatActivity
{
    private ImageButton openNewsButton, openDocumentButton, openMachineButton, openAvitostanButton, openGuideButton, openRepairButton, exitButton, searchButton, openMyInfoBtn;
    private String accessToken, refreshToken, userType;
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.all_widjet);
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
        openNewsButton = findViewById(R.id.openNewsButton);
        openDocumentButton = findViewById(R.id.openDocumentButton);
        openMachineButton = findViewById(R.id.openMachineButton);
        openAvitostanButton = findViewById(R.id.openAvitostanButton);
        openGuideButton = findViewById(R.id.openGuideButton);
        openRepairButton = findViewById(R.id.openRepairButton);
        exitButton = findViewById(R.id.exitButton);
        searchButton = findViewById(R.id.searchButton);
        openMyInfoBtn = findViewById(R.id.openMyInfoBtn);

        openNewsButton.setOnClickListener(v -> {
            Intent intent = new Intent (allWidjet.this, newsActivity.class);
            startActivity(intent);
            finish();
        });

        openMyInfoBtn.setOnClickListener(v -> {
            Intent intent = new Intent (allWidjet.this, UserDetailActivity.class);
            intent.putExtra("user_id", utils.getUserIdFromToken(this, accessToken, refreshToken));
            intent.putExtra("user_type", "Студент");
            startActivity(intent);
            finish();
        });

        searchButton.setOnClickListener(v -> {
            Intent intent = new Intent (allWidjet.this, searchActivity.class);
            startActivity(intent);
            finish();
        });

        exitButton.setOnClickListener(v -> {
            SharedPreferences prefs1 = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs1.edit();
            editor.remove("access_token");
            editor.remove("refresh_token");
            editor.apply();
            Intent intent = new Intent (allWidjet.this, loginActivity.class);
            startActivity(intent);
            finish();
        });

        openDocumentButton.setOnClickListener(v -> {
            Intent intent = new Intent (allWidjet.this, documentsActivity.class);
            startActivity(intent);
            finish();
        });

        openMachineButton.setOnClickListener(v -> {
            Intent intent = new Intent (allWidjet.this, reserveMachineActivity.class);
            startActivity(intent);
            finish();
        });

        openAvitostanButton.setOnClickListener(v -> {
            Intent intent = new Intent (allWidjet.this, avitostanActivity.class);
            startActivity(intent);
            finish();
        });

        openGuideButton.setOnClickListener(v -> {
            Intent intent = new Intent (allWidjet.this, guideActivity.class);
            startActivity(intent);
            finish();
        });

        openRepairButton.setOnClickListener(v -> {
            Intent intent = new Intent (allWidjet.this, repairActivity.class);
            startActivity(intent);
            finish();
        });
    }
}

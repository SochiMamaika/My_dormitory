package com.example.mydormitory;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

public class repairActivity extends AppCompatActivity
{
    ImageButton menuButton;
    LinearLayout plumberLayout, carpenterLayout, electricianLayout;
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.repair);
        menuButton = findViewById(R.id.menuButton);
        plumberLayout = findViewById(R.id.plumberLayout);
        carpenterLayout = findViewById(R.id.carpenterLayout);
        electricianLayout = findViewById(R.id.electricianLayout);

        menuButton.setOnClickListener(v -> {
            Intent intent = new Intent (repairActivity.this, allWidjet.class);
            startActivity(intent);
        });

        plumberLayout.setOnClickListener(v -> {
            Intent intent = new Intent (repairActivity.this, addRepairActivity.class);
            intent.putExtra("repair_type", "plumber");
            startActivity(intent);
        });

        carpenterLayout.setOnClickListener(v -> {
            Intent intent = new Intent (repairActivity.this, addRepairActivity.class);
            intent.putExtra("repair_type", "carpenter");
            startActivity(intent);
        });

        electricianLayout.setOnClickListener(v -> {
            Intent intent = new Intent (repairActivity.this, addRepairActivity.class);
            intent.putExtra("repair_type", "electrician");
            startActivity(intent);
        });
    }
}
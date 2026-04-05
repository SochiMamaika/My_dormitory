package com.example.mydormitory;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

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

public class addAvitoActivity extends AppCompatActivity
{
    Button publishAvitostanButton;
    private static final int PICK_IMAGE_REQUEST = 5;
    private List<Uri> selectedImages = new ArrayList<>();
    ImageButton backToAvitoButton;
    Spinner categoryAvitostan;
    EditText bodyAvitostan, roomAvitostan;
    LinearLayout addPhotoForAvitostan, imagesContainerForAvitostan;
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.add_avito);
        publishAvitostanButton = findViewById(R.id.publishAvitostanButton);
        backToAvitoButton = findViewById(R.id.backToAvitoButton);
        categoryAvitostan = findViewById(R.id.categoryAvitostan);
        bodyAvitostan = findViewById(R.id.bodyAvitostan);
        roomAvitostan = findViewById(R.id.roomAvitostan);
        addPhotoForAvitostan = findViewById(R.id.addPhotoForAvitostan);
        imagesContainerForAvitostan = findViewById(R.id.imagesContainerForAvitostan);
        SharedPreferences prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
        String accessToken = prefs.getString("access_token", null);
        String refreshToken = prefs.getString("refresh_token", null);

        if (accessToken != null)
        {
            // Можно использовать токен в запросах
            Log.d("TOKEN", "Access: " + accessToken);
        }
        else
        {
            // Пользователь не авторизован
            startActivity(new Intent(this, loginActivity.class));
            finish();
        }

        backToAvitoButton.setOnClickListener(v -> {
            Intent intent = new Intent (addAvitoActivity.this, avitostanActivity.class);
            startActivity(intent);
        });

        addPhotoForAvitostan.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            startActivityForResult(intent, PICK_IMAGE_REQUEST);
        });

        publishAvitostanButton.setOnClickListener(v -> {
            String type = categoryAvitostan.getSelectedItem().toString();
            String body = bodyAvitostan.getText().toString();
            String room = roomAvitostan.getText().toString();
            if(type.equals("Выберите категорию") || body.isEmpty() || room.isEmpty())
            {
                Toast.makeText(addAvitoActivity.this, "Нужно заполнить все поля", Toast.LENGTH_SHORT).show();
                return;
            }
            int rooms;
            try {
                rooms = Integer.parseInt(room);
                if (rooms <= 0) {
                    Toast.makeText(addAvitoActivity.this, "Номер комнаты должен быть больше 0", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
            catch (NumberFormatException e) {
                Toast.makeText(addAvitoActivity.this, "Комната должна быть числом", Toast.LENGTH_SHORT).show();
                return;
            }

            // Запускаем в отдельном потоке чтобы не блокировать UI
            new Thread(() -> {
                try
                {
                    // 1. Отправляем фото и получаем их пути
                    List<String> photoPaths = new ArrayList<>();
                    for (Uri imageUri : selectedImages)
                    {
                        String path = utils.uploadFileToServer(addAvitoActivity.this, imageUri, "thing", "photo");
                        photoPaths.add(path);
                    }

                    // 2. Отправляем данные о ремонте
                    sendAvitostanData(type,
                                      body,
                                      rooms,
                                      photoPaths,
                                      accessToken,
                                      refreshToken);

                    // Показываем успех
                    runOnUiThread(() -> {
                        Toast.makeText(addAvitoActivity.this, "Успешно отправлено!", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(addAvitoActivity.this, avitostanActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        finish();
                    });

                }
                catch (Exception e)
                {
                    runOnUiThread(() -> {
                        Toast.makeText(addAvitoActivity.this, "Ошибка: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
                }
            }).start();

        });
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK)
        {
            if (data.getClipData() != null)
            {
                int count = data.getClipData().getItemCount();
                int photosToAdd = Math.min(count, 3 - selectedImages.size());

                for (int i = 0; i < photosToAdd; i++)
                {
                    Uri imageUri = data.getClipData().getItemAt(i).getUri();
                    selectedImages.add(imageUri);
                    addImageToContainer(imageUri);
                }
            }
            else if (data.getData() != null && selectedImages.size() < 3)
            {
                // Выбрано одно фото, но только если у нас меньше 3
                Uri imageUri = data.getData();
                selectedImages.add(imageUri);
                addImageToContainer(imageUri);
            }
        }
    }

    private void addImageToContainer(Uri imageUri)
    {
        RelativeLayout imageLayout = new RelativeLayout(this);
        imageLayout.setLayoutParams(new RelativeLayout.LayoutParams(150, 150));
        imageLayout.setPadding(8, 8, 8, 8);

        // Само фото
        ImageView imageView = new ImageView(this);
        imageView.setLayoutParams(new RelativeLayout.LayoutParams(250, 250));
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        imageView.setImageURI(imageUri);

        ImageButton deleteButton = new ImageButton(this);
        RelativeLayout.LayoutParams deleteParams = new RelativeLayout.LayoutParams(60, 60);
        deleteParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        deleteParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        deleteButton.setLayoutParams(deleteParams);
        deleteButton.setImageResource(android.R.drawable.ic_delete);
        deleteButton.setPadding(8, 8, 8, 8);
        deleteButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                imagesContainerForAvitostan.removeView(imageLayout);
                selectedImages.remove(imageUri);
            }
        });

        imageLayout.addView(imageView);
        imageLayout.addView(deleteButton);
        imagesContainerForAvitostan.addView(imageLayout);
    }


    private void sendAvitostanData(String type,
                                   String body,
                                   int rooms,
                                   List<String> photos,
                                   String accessToken,
                                   String refreshToken) throws Exception
    {
        String apiUrl = "http://10.0.2.2:3000/thing";

        JSONObject jsonBody = new JSONObject();
        jsonBody.put("type", type);
        jsonBody.put("body", body);
        jsonBody.put("room", rooms);
        jsonBody.put("thing_paths", new JSONArray(photos));

        HttpURLConnection connection = (HttpURLConnection) new URL(apiUrl).openConnection();
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
            //токен устарел → делаем запрос на бек /refresh
            connection.disconnect();
            if (utils.refreshAccessToken(addAvitoActivity.this, refreshToken))
            {
                // читаем новые токены из SharedPreferences
                SharedPreferences prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
                String newAccess = prefs.getString("access_token", null);
                String newRefresh = prefs.getString("refresh_token", null);
                // повторяем исходный запрос с новым токеном
                sendAvitostanData(type, body, rooms, photos, newAccess, newRefresh);
                return;
            }
            else
            {
                // Сессия истекла
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        SharedPreferences prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
                        SharedPreferences.Editor editor = prefs.edit();
                        editor.remove("access_token");
                        editor.remove("refresh_token");
                        editor.apply();
                        Toast.makeText(addAvitoActivity.this, "Сессия истекла. Войдите снова", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(addAvitoActivity.this, loginActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    }
                });
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
}
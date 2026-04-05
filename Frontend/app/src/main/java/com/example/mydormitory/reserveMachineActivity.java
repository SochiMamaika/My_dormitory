package com.example.mydormitory;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.icu.text.SimpleDateFormat;
import android.icu.util.Calendar;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class reserveMachineActivity extends AppCompatActivity {

    private static final String API_URL = "http://10.0.2.2:3000/washmachine";
    private static final String API = "http://10.0.2.2:3000/reserve";
    ImageButton menuButton, addWashMachineButton;
    Spinner machines_categories_view;
    RecyclerView bookingsRecyclerView;
    private final List<Machine> machines = new ArrayList<>();
    private final List<Button> dateButtonsList = new ArrayList<>();
    private Button selectedDateButton = null;
    private ArrayAdapter<String> spinnerAdapter;
    private List<ReserveWashMachine> userReservationObjects = new ArrayList<>();
    private String accessToken;
    private String refreshToken;
    private boolean hasWashMachineWriteRole = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.reserve_machine);

        // Получаем токены из SharedPreferences
        SharedPreferences prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
        accessToken = prefs.getString("access_token", null);
        refreshToken = prefs.getString("refresh_token", null);

        // Проверяем авторизацию
        if (accessToken == null) {
            Toast.makeText(this, "Пользователь не авторизован", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, loginActivity.class));
            finish();
            return;
        }
        hasWashMachineWriteRole = utils.hasRole(this, accessToken, refreshToken, "wash_machine_write");

        menuButton = findViewById(R.id.menuButton);
        addWashMachineButton = findViewById(R.id.addWashMachineButton);
        machines_categories_view = findViewById(R.id.machines_categories_view);
        spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, new ArrayList<String>());
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        machines_categories_view.setAdapter(spinnerAdapter);
        machines_categories_view.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position > 0 && machines.size() >= position) {
                    // Получаем выбранную машину
                    Machine selectedMachine = machines.get(position - 1);
                    Toast.makeText(reserveMachineActivity.this, "Выбрана: " + selectedMachine.getName(), Toast.LENGTH_SHORT).show();
                    showDatesForSelectedMachine();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Ничего не выбрано
            }
        });

        loadMachinesFromServer();

        menuButton.setOnClickListener(v -> {
            Intent intent = new Intent(reserveMachineActivity.this, allWidjet.class);
            startActivity(intent);
        });



        if (hasWashMachineWriteRole)
        {
            addWashMachineButton.setVisibility(View.VISIBLE);
            addWashMachineButton.setOnClickListener(v -> {
                Intent intent = new Intent(reserveMachineActivity.this, addWashMachineActivity.class);
                startActivity(intent);
            });
        }
        else
        {
            addWashMachineButton.setVisibility(View.GONE);
        }
    }

    private void showDatesForSelectedMachine() {
        LinearLayout dateContainer = findViewById(R.id.dateContainer);
        dateContainer.removeAllViews();
        dateButtonsList.clear(); // Очищаем список
        selectedDateButton = null;

        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM\nEEE", new Locale("ru", "RU"));
        Calendar calendar = Calendar.getInstance();

        for (int i = 0; i < 7; i++) {
            Button button = new Button(this);
            button.setText(sdf.format(calendar.getTime()));
            button.setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16));
            button.setMinimumWidth(dpToPx(80));

            // Устанавливаем исходные цвета
            button.setBackgroundColor(Color.TRANSPARENT);
            button.setTextColor(Color.BLACK);

            final String date = sdf.format(calendar.getTime());

            button.setOnClickListener(v ->
            {
                // Если уже была выбрана кнопка, сбрасываем её
                if (selectedDateButton != null && selectedDateButton != button) {
                    selectedDateButton.setBackgroundColor(Color.TRANSPARENT);
                    selectedDateButton.setTextColor(Color.BLACK);
                }

                // Устанавливаем новый выбор
                button.setBackgroundColor(Color.parseColor("#2196F3"));
                button.setTextColor(Color.WHITE);
                selectedDateButton = button;
                Toast.makeText(this, "Выбрана дата: " + date, Toast.LENGTH_SHORT).show();
                loadTimeForSelectedDay(date);
            });

            dateContainer.addView(button);
            dateButtonsList.add(button);
            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    private void loadTimeForSelectedDay(String date) {
        RecyclerView rv = findViewById(R.id.TimeSelectedDateRecyclerView);
        TextView bookingSummaryText = findViewById(R.id.bookingSummaryText);
        Button bookButton = findViewById(R.id.bookButton);
        Button resetButton = findViewById(R.id.resetTimeButton);

        rv.setLayoutManager(new GridLayoutManager(this, 3));

        // Получаем выбранную машину
        int selectedPosition = machines_categories_view.getSelectedItemPosition();
        if (selectedPosition <= 0) {
            Toast.makeText(this, "Сначала выберите машину", Toast.LENGTH_SHORT).show();
            return;
        }

        Machine selectedMachine = machines.get(selectedPosition - 1);

        String selectedDateBackendFormat = convertDateToBackendFormat(date);

        // Получаем список занятых времен для этой машины на выбранную дату
        List<ReserveWashMachine> reservationsForSelectedDate = new ArrayList<>();
        for (ReserveWashMachine reservation : selectedMachine.getReservations()) {
            if (reservation.getDate().equals(selectedDateBackendFormat)) {
                reservationsForSelectedDate.add(reservation);
            }
        }

        // Создаем временные слоты и отмечаем занятые
        List<TimeSlot> slots = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", new Locale("ru", "RU"));
        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, 8);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);

        for (int i = 0; i < 30; i++) {
            String time = sdf.format(c.getTime());
            TimeSlot slot = new TimeSlot(time, i);

            // Проверяем, занят ли этот слот
            boolean isBooked = isTimeSlotBooked(time, reservationsForSelectedDate);
            slot.setBooked(isBooked);

            slots.add(slot);
            c.add(Calendar.MINUTE, 30);
        }

        final int[] start = {-1};
        final int[] end = {-1};

        final TimeAdapter[] adapterHolder = new TimeAdapter[1];

        adapterHolder[0] = new TimeAdapter(slots, index -> {
            // Если слот занят, не позволяем выбирать
            if (slots.get(index).isBooked()) {
                Toast.makeText(this, "Это время уже занято", Toast.LENGTH_SHORT).show();
                return;
            }

            if (start[0] == -1) {
                start[0] = index;
                adapterHolder[0].setInterval(start[0], start[0]);

                bookingSummaryText.setText("Начало: " + slots.get(index).getTime() + " — выберите конец");
                bookButton.setEnabled(false);
                return;
            }

            // Проверяем, не пересекается ли выбранный интервал с занятыми слотами
            if (isIntervalBooked(start[0], index, slots)) {
                Toast.makeText(this, "Выбранный интервал содержит занятое время", Toast.LENGTH_SHORT).show();
                return;
            }

            if (index <= start[0]) {
                Toast.makeText(this, "Конец должен быть позже начала", Toast.LENGTH_SHORT).show();
                return;
            }

            if (index - start[0] > 5) {
                Toast.makeText(this, "Максимум 2.5 часа", Toast.LENGTH_SHORT).show();
                return;
            }

            end[0] = index;
            adapterHolder[0].setInterval(start[0], end[0]);

            bookingSummaryText.setText("Бронируете с " + slots.get(start[0]).getTime() + " до " + slots.get(end[0]).getTime());
            bookButton.setEnabled(true);
        });

        rv.setAdapter(adapterHolder[0]);
        bookButton.setEnabled(false);

        resetButton.setOnClickListener(v -> {
            start[0] = -1;
            end[0] = -1;

            adapterHolder[0].reset();

            bookingSummaryText.setText("Выберите интервал");
            bookButton.setEnabled(false);

            Toast.makeText(this, "Выбор времени сброшен", Toast.LENGTH_SHORT).show();
        });

        bookButton.setOnClickListener(v -> {
            if (start[0] != -1 && end[0] != -1) {
                loadReserveOnServer(date, slots.get(start[0]).getTime(), slots.get(end[0]).getTime());
            }
        });
    }

    private void showUserReservations() {
        if (bookingsRecyclerView == null) {
            return;
        }

        // Парсим user_id из access token (JWT token)
        int currentUserId = utils.getUserIdFromToken(this, accessToken, refreshToken);

        if (currentUserId == -1) {
            Toast.makeText(this, "Ошибка: не удалось получить ID пользователя", Toast.LENGTH_SHORT).show();
            return;
        }

        // Собираем все бронирования текущего пользователя
        List<String> userReservations = new ArrayList<>();
        List<ReserveWashMachine> reservationObjects = new ArrayList<>(); // Новый список для объектов

        for (Machine machine : machines) {
            if (machine.getReservations() != null) {
                for (ReserveWashMachine reservation : machine.getReservations()) {
                    if (reservation.getUserId() == currentUserId) {
                        String dateStr = reservation.getDate();
                        String formattedDate = dateStr.substring(8, 10) + "-" + dateStr.substring(5, 7) + "-" + dateStr.substring(0, 4);

                        // Форматируем время: 08:00:00 → 8:00
                        String timeStr = reservation.getStartTime();
                        String formattedTime = timeStr;

                        if (timeStr.length() >= 8) {
                            formattedTime = timeStr.substring(0, 5);
                            if (formattedTime.startsWith("0")) {
                                formattedTime = formattedTime.substring(1); // "8:00"
                            }
                        }

                        String item = "• " + machine.getName() + "\n" +
                                "  " + formattedDate + " " + formattedTime +
                                " (" + reservation.getDuration() + " ч)";
                        userReservations.add(item);
                        reservationObjects.add(reservation); // Сохраняем объект бронирования
                    }
                }
            }
        }
        // Сохраняем объекты в поле класса
        this.userReservationObjects = reservationObjects;

        // Настраиваем RecyclerView
        bookingsRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        if (userReservations.isEmpty()) {
            List<String> emptyList = new ArrayList<>();
            emptyList.add("У вас пока нет бронирований");
            ReservationAdapter adapter = new ReservationAdapter(emptyList);
            bookingsRecyclerView.setAdapter(adapter);
        } else {
            ReservationAdapter adapter = new ReservationAdapter(userReservations);

            adapter.setOnDeleteClickListener(position -> {
                if (position >= 0 && position < reservationObjects.size()) {
                    // Получаем объект бронирования по позиции
                    ReserveWashMachine reservation = reservationObjects.get(position);
                    // Вызываем метод удаления
                    deleteReservationFromServer(reservation.getIdReserve());
                }
            });

            bookingsRecyclerView.setAdapter(adapter);
            Toast.makeText(this, "Найдено бронирований: " + userReservations.size(), Toast.LENGTH_SHORT).show();
        }
    }

    private void deleteReservationFromServer(int reservationId) {
        new Thread(() -> {
            try {
                // Получаем user_id из токена
                int currentUserId = utils.getUserIdFromToken(this, accessToken, refreshToken);

                if (currentUserId == -1) {
                    runOnUiThread(() ->
                            Toast.makeText(this, "Ошибка: не удалось получить ID пользователя", Toast.LENGTH_SHORT).show());
                    return;
                }

                String deleteUrl = API + "/" + reservationId + "/" + currentUserId;

                HttpURLConnection connection = (HttpURLConnection) new URL(deleteUrl).openConnection();
                connection.setRequestMethod("DELETE");
                connection.setRequestProperty("Authorization", "Bearer " + accessToken);
                connection.setRequestProperty("Content-Type", "application/json; utf-8");

                int responseCode = connection.getResponseCode();
                System.out.println("DELETE Response Code: " + responseCode);
                System.out.println("DELETE URL: " + deleteUrl);

                if (responseCode == HttpURLConnection.HTTP_NO_CONTENT) { // 204
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Бронирование успешно удалено", Toast.LENGTH_SHORT).show();
                        // Обновляем список бронирований
                        loadMachinesFromServer();
                    });
                }
                else if (responseCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
                    // Обработка истекшего токена
                    connection.disconnect();
                    if (utils.refreshAccessToken(reserveMachineActivity.this, refreshToken)) {
                        SharedPreferences prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
                        String newAccess = prefs.getString("access_token", null);
                        String newRefresh = prefs.getString("refresh_token", null);

                        // Обновляем токены
                        reserveMachineActivity.this.accessToken = newAccess;
                        reserveMachineActivity.this.refreshToken = newRefresh;

                        deleteReservationFromServer(reservationId);
                    }
                }
                else {
                    // Читаем ошибку
                    BufferedReader errorReader = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                    StringBuilder errorResponse = new StringBuilder();
                    String line;
                    while ((line = errorReader.readLine()) != null) {
                        errorResponse.append(line);
                    }
                    errorReader.close();

                    runOnUiThread(() ->
                            Toast.makeText(this, "Ошибка удаления: код " + responseCode, Toast.LENGTH_SHORT).show());
                }

                connection.disconnect();

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() ->
                        Toast.makeText(this, "Ошибка: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private String convertDateToBackendFormat(String date) {
        try {
            SimpleDateFormat formatter = new SimpleDateFormat("dd.MM\nEEE", new Locale("ru", "RU"));
            Date parsedDate = formatter.parse(date);

            Calendar cal = Calendar.getInstance();
            cal.setTime(parsedDate);
            cal.set(Calendar.YEAR, Calendar.getInstance().get(Calendar.YEAR));

            SimpleDateFormat backendFormatter = new SimpleDateFormat("yyyy-MM-dd");
            return backendFormatter.format(cal.getTime());
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    // Вспомогательный метод для проверки занятости слота
    private boolean isTimeSlotBooked(String time, List<ReserveWashMachine> reservations) {
        if (reservations == null) return false;

        for (ReserveWashMachine reservation : reservations) {
            String startTime = reservation.getStartTime();
            float duration = reservation.getDuration();

            // Преобразуем время в минуты
            int slotMinutes = timeToMinutes(time);
            int reservationStartMinutes = timeToMinutes(startTime);
            int reservationEndMinutes = reservationStartMinutes + (int)(duration * 60);

            // Проверяем, попадает ли слот в интервал бронирования
            if (slotMinutes >= reservationStartMinutes && slotMinutes < reservationEndMinutes) {
                return true;
            }
        }
        return false;
    }

    // Вспомогательный метод для проверки занятости интервала
    private boolean isIntervalBooked(int startIndex, int endIndex, List<TimeSlot> slots) {
        for (int i = startIndex; i <= endIndex; i++) {
            if (slots.get(i).isBooked()) {
                return true;
            }
        }
        return false;
    }

    // Вспомогательный метод для преобразования времени в минуты
    private int timeToMinutes(String time) {
        try {
            String[] parts = time.split(":");
            int hours = Integer.parseInt(parts[0]);
            int minutes = Integer.parseInt(parts[1]);
            return hours * 60 + minutes;
        } catch (Exception e) {
            return 0;
        }
    }

    private void loadReserveOnServer(String date, String start, String end) {
        new Thread(() -> {
            try {
                sendPostReserve(accessToken, refreshToken, date, start, end);
                // Показываем успех
                runOnUiThread(() -> {
                    Toast.makeText(reserveMachineActivity.this, "Успешно отправлено!", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(reserveMachineActivity.this, reserveMachineActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                });
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(reserveMachineActivity.this, "Ошибка загрузки: Проверьте корректность данных " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void sendPostReserve(String accessToken, String refreshToken, String date, String startTime, String endTime) throws Exception {
        String reserveUrl = API;

        // Вычисляем duration в часах
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        java.util.Date start = timeFormat.parse(startTime);
        java.util.Date end = timeFormat.parse(endTime);

        // Разница в миллисекундах
        long differenceMs = end.getTime() - start.getTime();
        // Конвертируем в часы
        float durationHours = (float) differenceMs / (1000 * 60 * 60);

        // Получаем выбранную машину
        int selectedPosition = machines_categories_view.getSelectedItemPosition();
        if (selectedPosition <= 0) {
            throw new Exception("Машина не выбрана");
        }

        Machine selectedMachine = machines.get(selectedPosition - 1);
        int machineId = selectedMachine.getId();

        Calendar cal = Calendar.getInstance();

        SimpleDateFormat formatter = new SimpleDateFormat("dd.MM\nEEE", new Locale("ru", "RU"));
        Date parsedDate = formatter.parse(date);

        cal.setTime(parsedDate);
        cal.set(Calendar.YEAR, Calendar.getInstance().get(Calendar.YEAR));

        SimpleDateFormat backendFormatter = new SimpleDateFormat("yyyy-MM-dd");
        String format = backendFormatter.format(cal.getTime());

        JSONObject json = new JSONObject();
        json.put("machine_id", machineId);
        json.put("date_start", format);
        json.put("time_start", startTime);     // "08:00" или другое время
        json.put("duration", durationHours);

        // Логируем отправляемые данные
        System.out.println("Отправляемые данные бронирования:");
        System.out.println("machine_id: " + machineId);
        System.out.println("date_start: " + format);
        System.out.println("time_start: " + startTime);
        System.out.println("duration: " + durationHours);
        System.out.println("JSON: " + json);

        HttpURLConnection connection = (HttpURLConnection) new URL(reserveUrl).openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json; utf-8");
        connection.setRequestProperty("Authorization", "Bearer " + accessToken);
        connection.setDoOutput(true);

        DataOutputStream outputStream = new DataOutputStream(connection.getOutputStream());
        outputStream.write(json.toString().getBytes(StandardCharsets.UTF_8));
        outputStream.flush();
        outputStream.close();

        int responseCode = connection.getResponseCode();
        System.out.println("Response Code: " + responseCode);

        if (responseCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
            connection.disconnect();
            if (utils.refreshAccessToken(reserveMachineActivity.this, refreshToken)) {
                SharedPreferences prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
                String newAccess = prefs.getString("access_token", null);
                String newRefresh = prefs.getString("refresh_token", null);
                sendPostReserve(newAccess, newRefresh, date, startTime, endTime);
                return;
            }
            else {
                // Сессия истекла
                runOnUiThread(() -> {
                    SharedPreferences prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.remove("access_token");
                    editor.remove("refresh_token");
                    editor.apply();
                    Toast.makeText(reserveMachineActivity.this, "Сессия истекла. Войдите снова", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(reserveMachineActivity.this, loginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                });
                return;
            }
        }

        // Читаем ответ
        BufferedReader reader;
        if (responseCode >= 200 && responseCode < 300) {
            reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        } else {
            reader = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
        }

        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        reader.close();
        connection.disconnect();

        if (responseCode != HttpURLConnection.HTTP_OK && responseCode != HttpURLConnection.HTTP_CREATED) {
            throw new Exception("Ошибка отправки данных (код " + responseCode + "): " + response);
        }

        System.out.println("Бронирование успешно: " + response);
    }

    private void loadMachinesFromServer() {
        new Thread(() -> {
            try {
                String response = sendGetRequest(accessToken, refreshToken);
                if (response != null) {
                    final List<Machine> loadedMachines = parseMachines(response);

                    runOnUiThread(() -> {
                        // Сохраняем в наш массив
                        machines.clear();
                        machines.addAll(loadedMachines);

                        // Обновляем спиннер
                        updateSpinnerWithMachines(loadedMachines);
                        Toast.makeText(reserveMachineActivity.this, "Загружено машин: " + loadedMachines.size(), Toast.LENGTH_SHORT).show();
                        bookingsRecyclerView = findViewById(R.id.bookingsRecyclerView);
                        showUserReservations();
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(reserveMachineActivity.this, "Ошибка загрузки: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private String sendGetRequest(String accessToken, String refreshToken) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(API_URL).openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Content-Type", "application/json; utf-8");
        connection.setRequestProperty("Authorization", "Bearer " + accessToken);

        int responseCode = connection.getResponseCode();

        if (responseCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
            // Токен устарел → делаем запрос на бек /refresh
            connection.disconnect();
            if (utils.refreshAccessToken(reserveMachineActivity.this, refreshToken)) {
                // Читаем новые токены из SharedPreferences
                SharedPreferences prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
                String newAccess = prefs.getString("access_token", null);
                String newRefresh = prefs.getString("refresh_token", null);

                // Обновляем переменные класса
                reserveMachineActivity.this.accessToken = newAccess;
                reserveMachineActivity.this.refreshToken = newRefresh;

                // Повторяем исходный запрос с новым токеном
                return sendGetRequest(newAccess, newRefresh);
            }
            else {
                runOnUiThread(() -> {
                    SharedPreferences prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.remove("access_token");
                    editor.remove("refresh_token");
                    editor.apply();
                    Toast.makeText(reserveMachineActivity.this, "Сессия истекла. Войдите снова", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(reserveMachineActivity.this, loginActivity.class);
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
            throw new Exception("Ошибка получения данных: " + errorResponse);
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

    private List<Machine> parseMachines(String jsonResponse) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(jsonResponse, new TypeReference<>() {
        });
    }

    private void updateSpinnerWithMachines(List<Machine> machines) {
        List<String> machineNames = new ArrayList<>();
        machineNames.add("Выберите стиральную машину");

        for (Machine m : machines) {
            machineNames.add(m.getName());
        }
        spinnerAdapter.clear();
        spinnerAdapter.addAll(machineNames);
        spinnerAdapter.notifyDataSetChanged();
    }
}
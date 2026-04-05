package com.example.mydormitory;


import com.fasterxml.jackson.annotation.JsonProperty;

public class ReserveWashMachine {
    @JsonProperty("id_reserve")
    private int idReserve;

    @JsonProperty("user_id")
    private int userId;

    @JsonProperty("date")
    private String date;

    @JsonProperty("start_time")
    private String startTime;

    @JsonProperty("duration")
    private float duration;

    public ReserveWashMachine(int idReserve, int userId, String date, String startTime, float duration) {
        this.idReserve = idReserve;
        this.userId = userId;
        this.date = date;
        this.startTime = startTime;
        this.duration = duration;
    }

    public ReserveWashMachine() {
    }

    public int getIdReserve() {
        return idReserve;
    }

    public void setIdReserve(int idReserve) {
        this.idReserve = idReserve;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public float getDuration() {
        return duration;
    }

    public void setDuration(float duration) {
        this.duration = duration;
    }
}

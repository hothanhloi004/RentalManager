package com.example.rentalmanager.util;

public class ResultState {

    public boolean success;
    public String message;

    public ResultState(boolean success, String message) {
        this.success = success;
        this.message = message;
    }
}
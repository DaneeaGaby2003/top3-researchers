package com.technoready.top3.service;

public class ApiException extends Exception {
    private final int statusCode;
    public ApiException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }
    public int getStatusCode() { return statusCode; }
}

package com.zendo.apps.data.models;

public class AuthResultState<T> {
    public enum Status {
        LOADING,
        SUCCESS,
        ERROR
    }

    private final Status status;
    private final T data;
    private final String message;

    private AuthResultState(Status status, T data, String message) {
        this.status = status;
        this.data = data;
        this.message = message;
    }

    public static <T> AuthResultState<T> loading() {
        return new AuthResultState<>(Status.LOADING, null, null);
    }

    public static <T> AuthResultState<T> success(T data) {
        return new AuthResultState<>(Status.SUCCESS, data, null);
    }

    public static <T> AuthResultState<T> error(String message) {
        return new AuthResultState<>(Status.ERROR, null, message);
    }

    public Status getStatus() {
        return status;
    }

    public T getData() {
        return data;
    }

    public String getMessage() {
        return message;
    }
}

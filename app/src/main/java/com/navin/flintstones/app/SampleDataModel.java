package com.navin.flintstones.app;

import com.google.gson.annotations.SerializedName;

public class SampleDataModel {
    @SerializedName("id")
    private final int id;

    @SerializedName("message")
    private String message;

    public SampleDataModel(int id, String message) {
        this.id = id;
        this.message = message;
    }

    public int id() {
        return id;
    }

    public String message() {
        return message;
    }
}

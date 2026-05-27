package com.example.simpleapp;

import java.io.Serializable;

public class CalendarEvent implements Serializable {
    private String date;
    private String title;

    public CalendarEvent(String date, String title) {
        this.date = date;
        this.title = title;
    }

    public String getDate() { return date; }
    public String getTitle() { return title; }
}
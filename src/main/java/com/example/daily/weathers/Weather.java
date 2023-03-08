package com.example.daily.weathers;

import java.util.List;

public class Weather {
    private String city;
    private String update_time;
    private List<DayData> data;

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getUpdate_time() {
        return update_time;
    }

    public void setUpdate_time(String update_time) {
        this.update_time = update_time;
    }

    public List<DayData> getData() {
        return data;
    }

    public void setData(List<DayData> data) {
        this.data = data;
    }
}

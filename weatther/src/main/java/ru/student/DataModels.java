package com.example;

import java.util.List;

// Классы для хранения данных (POJO)
public class DataModels {

    // Пункт 3 для данных которые приходят от Geocoding API
    public static class CityInfo {
        String name;
        double lat;
        double lon;
        String country;
        String state;

        @Override
        public String toString() {
            return name + ", " + country + (state != null ? " (" + state + ")" : "");
        }
    }

    // Пункт 5 для текущей погоды
    public static class CurrentWeatherResponse {
        MainData main;
        List<WeatherInfo> weather;
        Wind wind;
        Clouds clouds;
        String name;
    }

    // Пункт 6 для прогноза на 5 дней
    public static class ForecastResponse {
        List<ForecastItem> list;
    }

    public static class ForecastItem {
        long dt; // время
        MainData main;
        List<WeatherInfo> weather;
        String dt_txt; // дата текстом
    }

    public static class MainData {
        double temp;
        double feels_like;
        double temp_min;
        double temp_max;
        int pressure;
        int humidity;
    }

    public static class WeatherInfo {
        String main;
        String description;
        String icon;
    }

    public static class Wind {
        double speed;
        int deg;
    }

    public static class Clouds {
        int all;
    }
}
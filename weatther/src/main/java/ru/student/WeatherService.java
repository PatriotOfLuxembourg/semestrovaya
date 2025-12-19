package com.example;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Scanner;

// Отвечает за логику работы с API и файлами
public class WeatherService {

    // п. 2 - подключить апи ключ. Подключили 
    private static final String API_KEY = "152b691fdeab564bc7e141617013cf49";
    
    private final Gson gson = new Gson();
    private static final String CONFIG_FILE = "saved_city.txt";

    // П.3 - Поиск городов (Geocoding API)
    public List<DataModels.CityInfo> searchCities(String query) throws WeatherException {
        try {
            // Лимит 5 городов
            String urlString = String.format("http://api.openweathermap.org/geo/1.0/direct?q=%s&limit=5&appid=%s", 
                    query, API_KEY);
            String jsonResponse = makeHttpRequest(urlString);
            
            return gson.fromJson(jsonResponse, new TypeToken<List<DataModels.CityInfo>>(){}.getType());
        } catch (Exception e) {
            throw new WeatherException("Ошибка поиска города: " + e.getMessage());
        }
    }

    // П.5 - Получение текущей погоды
    public DataModels.CurrentWeatherResponse getCurrentWeather(double lat, double lon) throws WeatherException {
        try {
            String urlString = String.format("https://api.openweathermap.org/data/2.5/weather?lat=%f&lon=%f&units=metric&lang=ru&appid=%s",
                    lat, lon, API_KEY);
            String jsonResponse = makeHttpRequest(urlString);
            return gson.fromJson(jsonResponse, DataModels.CurrentWeatherResponse.class);
        } catch (Exception e) {
            throw new WeatherException("Не удалось получить текущую погоду: " + e.getMessage());
        }
    }

    // П.6 - Прогноз (используем endpoint /forecast - он бесплатный и дает 5 дней каждые 3 часа)
    public DataModels.ForecastResponse getForecast(double lat, double lon) throws WeatherException {
        try {
            String urlString = String.format("https://api.openweathermap.org/data/2.5/forecast?lat=%f&lon=%f&units=metric&lang=ru&appid=%s",
                    lat, lon, API_KEY);
            String jsonResponse = makeHttpRequest(urlString);
            return gson.fromJson(jsonResponse, DataModels.ForecastResponse.class);
        } catch (Exception e) {
            throw new WeatherException("Не удалось получить прогноз: " + e.getMessage());
        }
    }

    // Метод для выполнения HTTP запроса (без сторонних либ, только Java Standard Library)
    private String makeHttpRequest(String urlString) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.connect();

        int responseCode = conn.getResponseCode();
        if (responseCode != 200) {
            throw new IOException("HTTP Error code: " + responseCode);
        }

        StringBuilder info = new StringBuilder();
        Scanner scanner = new Scanner(url.openStream(), StandardCharsets.UTF_8);
        while (scanner.hasNext()) {
            info.append(scanner.nextLine());
        }
        scanner.close();
        return info.toString();
    }

    // П.4 - Сохранение выбранного города в файл
    public void saveCity(DataModels.CityInfo city) throws IOException {
        try (Writer writer = new FileWriter(CONFIG_FILE)) {
            gson.toJson(city, writer);
        }
    }

    // П.4 - Загрузка города при старте
    public DataModels.CityInfo loadSavedCity() {
        try (Reader reader = new FileReader(CONFIG_FILE)) {
            return gson.fromJson(reader, DataModels.CityInfo.class);
        } catch (IOException e) {
            return null; // Файла нет, значит первый запуск
        }
    }
}
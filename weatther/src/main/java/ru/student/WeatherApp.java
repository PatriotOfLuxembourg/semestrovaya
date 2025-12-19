package com.example;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class WeatherApp extends Application {

    // Логгер (п.10, 12)
    private static final Logger logger = LoggerFactory.getLogger(WeatherApp.class);
    
    private WeatherService service = new WeatherService();
    
    // Элементы UI
    private TextField searchField;
    private Label cityNameLabel;
    private VBox weatherContainer;
    private HBox forecastContainer;
    private ContextMenu suggestionsMenu; // Выпадающий список подсказок (п.3)

    @Override
    public void start(Stage primaryStage) {
        // п. 9 - действия пользователя в консоль
        logger.info("Запуск приложения Погода");

        // Создание UI
        VBox root = new VBox(15);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-font-family: 'Arial'; -fx-background-color: #f0f4f8;");

        // Строка поиска (п.3)
        Label searchLabel = new Label("Поиск города:");
        searchField = new TextField();
        searchField.setPromptText("Введите название города...");
        suggestionsMenu = new ContextMenu(); // Меню для подсказок

        // Слушатель ввода текста для подсказок
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.length() > 2) {
                showSuggestions(newValue, searchField);
            }
        });
        // пункт 7 для выскакивания ошибки
        // Если список пуст — значит города нет и мы показываем messagebox
        searchField.setOnAction(event -> {
            String query = searchField.getText();
            if (!query.isEmpty()) {
                try {
                    List<DataModels.CityInfo> cities = service.searchCities(query);

                    if (cities.isEmpty()) {
                        
                        showAlert("Не найдено", "Город с таким названием не найден!");
                    } else {
                        // Если нашли, берем первый вариант и грузим погоду
                        loadWeatherData(cities.get(0));
                        suggestionsMenu.hide(); // Скрываем подсказки
                    }
                } catch (WeatherException e) {
                    logger.error("Ошибка поиска по Enter", e);
                    showAlert("Ошибка", e.getMessage());
                }
            }
        });

        // Основной контейнер погоды
        cityNameLabel = new Label("Выберите город");
        cityNameLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");
        
        weatherContainer = new VBox(10);
        weatherContainer.setStyle("-fx-background-color: white; -fx-padding: 15; -fx-background-radius: 10; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 0);");
        
        Label forecastTitle = new Label("Прогноз на 4 дня:");
        forecastTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-padding: 10 0 0 0;");
        
        forecastContainer = new HBox(15);
        forecastContainer.setAlignment(Pos.CENTER);

        root.getChildren().addAll(searchLabel, searchField, cityNameLabel, weatherContainer, forecastTitle, forecastContainer);

        // Загрузка сохраненного города (п.4)
        DataModels.CityInfo savedCity = service.loadSavedCity();
        if (savedCity != null) {
            logger.info("Загружен сохраненный город: " + savedCity.name);
            loadWeatherData(savedCity);
        }

        Scene scene = new Scene(root, 600, 700);
        primaryStage.setTitle("Java Погода (Вариант 2)");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    // Логика показа подсказок (п.3)
    private void showSuggestions(String query, TextField field) {
        try {
            List<DataModels.CityInfo> cities = service.searchCities(query);
            suggestionsMenu.getItems().clear();

            for (DataModels.CityInfo city : cities) {
                MenuItem item = new MenuItem(city.toString());
                item.setOnAction(e -> {
                    searchField.setText(city.name);
                    loadWeatherData(city); // Загружаем погоду при клике
                });
                suggestionsMenu.getItems().add(item);
            }
            
            if (!cities.isEmpty()) {
                suggestionsMenu.show(field, javafx.geometry.Side.BOTTOM, 0, 0);
            }
        } catch (WeatherException e) {
            logger.error("Ошибка при поиске подсказок", e); // Лог в файл
        }
    }

    // Загрузка и отображение данных
    private void loadWeatherData(DataModels.CityInfo city) {
        logger.info("Запрос погоды для города: " + city.name);
        
        try {
            // Сохраняем выбор (п.4)
            service.saveCity(city);
            
            // 1. Текущая погода
            DataModels.CurrentWeatherResponse current = service.getCurrentWeather(city.lat, city.lon);
            updateCurrentWeatherUI(current);
            
            // 2. Прогноз (п.6)
            DataModels.ForecastResponse forecast = service.getForecast(city.lat, city.lon);
            updateForecastUI(forecast);
            
            cityNameLabel.setText("Погода в " + city.name);

        } catch (Exception e) {
            logger.error("Критическая ошибка при загрузке данных", e);
            showAlert("Ошибка", "Не удалось загрузить данные: " + e.getMessage());
        }
    }

    // Отрисовка текущей погоды (п.5)
    private void updateCurrentWeatherUI(DataModels.CurrentWeatherResponse w) {
        weatherContainer.getChildren().clear();

        // Иконка (п.5 - ссылка)
        String iconUrl = "https://openweathermap.org/img/wn/" + w.weather.get(0).icon + "@2x.png";
        ImageView iconView = new ImageView(new Image(iconUrl));
        iconView.setFitWidth(80);
        iconView.setFitHeight(80);

        Label tempLabel = new Label(String.format("Температура: %.1f°C", w.main.temp));
        tempLabel.setStyle("-fx-font-size: 18px;");

        // Мин/Макс, Влажность, Давление, Ветер, Облачность
        String details = String.format(
            "Мин/Макс: %.1f°C / %.1f°C\n" +
            "Влажность: %d%%\n" +
            "Давление: %d гПа\n" +
            "Ветер: %.1f м/с\n" +
            "Облачность: %d%%",
            w.main.temp_min, w.main.temp_max,
            w.main.humidity,
            w.main.pressure,
            w.wind.speed,
            w.clouds.all
        );

        Label detailsLabel = new Label(details);
        
        HBox topBox = new HBox(20, iconView, tempLabel);
        topBox.setAlignment(Pos.CENTER_LEFT);
        
        weatherContainer.getChildren().addAll(topBox, detailsLabel);
    }

    // Отрисовка прогноза на 4 дня (п.6)
    private void updateForecastUI(DataModels.ForecastResponse forecast) {
        forecastContainer.getChildren().clear();
        
        // API возвращает данные каждые 3 часа. Нам нужно 4 дня.
        // Берем данные примерно на полдень следующего дня и т.д.
        // Индексы: 8 (24ч/3ч = 8 отсчетов в сутки). 8, 16, 24, 32.
        
        int count = 0;
        // Пропускаем сегодня, начинаем с завтра
        for (int i = 7; i < forecast.list.size(); i += 8) {
            if (count >= 4) break;
            
            DataModels.ForecastItem item = forecast.list.get(i);
            VBox dayCard = createDayCard(item);
            forecastContainer.getChildren().add(dayCard);
            count++;
        }
    }

    private VBox createDayCard(DataModels.ForecastItem item) {
        VBox card = new VBox(5);
        card.setStyle("-fx-background-color: white; -fx-padding: 10; -fx-background-radius: 5; -fx-border-color: #ddd; -fx-border-radius: 5;");
        card.setAlignment(Pos.CENTER);
        
        // Дата
        Date date = new Date(item.dt * 1000);
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM");
        Label dateLabel = new Label(sdf.format(date));
        dateLabel.setStyle("-fx-font-weight: bold;");

        // Иконка
        String iconUrl = "https://openweathermap.org/img/wn/" + item.weather.get(0).icon + ".png";
        ImageView icon = new ImageView(new Image(iconUrl));

        // Температура
        Label temp = new Label(String.format("%.0f°C", item.main.temp));

        card.getChildren().addAll(dateLabel, icon, temp);
        return card;
    }

    // П.7 - MessageBox с уведомлением (Alert)
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
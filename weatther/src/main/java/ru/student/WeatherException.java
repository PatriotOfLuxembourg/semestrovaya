package com.example;
// п. 8 - создали подкласс от Exception, чтобы throw new WeatherException сработало
public class WeatherException extends Exception {
    public WeatherException(String message) {
        super(message); // Конструктор родителя, чтобы сохранить текст ошибки
    }
}
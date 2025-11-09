package com.example.weather;

import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class WeatherResponse {
    private double temperature; // Celsius
    private double windSpeed; // m/s
    private String symbolCode; // yr.no symbol code (e.g., "clearsky_day")
    private String condition; // simplified: sunny, cloudy, rainy, etc.
    private boolean shouldGoOutside; // recommendation based on weather

    // Optional: Add more fields if needed
    private String message;

    public WeatherResponse(double temperature, double windSpeed, String symbolCode,
                           String condition, boolean shouldGoOutside) {
        this.temperature = temperature;
        this.windSpeed = windSpeed;
        this.symbolCode = symbolCode;
        this.condition = condition;
        this.shouldGoOutside = shouldGoOutside;
        this.message = generateMessage();
    }

    private String generateMessage() {
        if (shouldGoOutside) {
            return "Beautiful weather! You should go outside and enjoy the sunshine! ☀️";
        } else if (condition.equals("rainy")) {
            return "It's raining outside. Perfect weather for staying in and playing cards! 🌧️";
        } else if (condition.equals("snowy")) {
            return "It's snowing! Maybe build a snowman later? ⛄";
        } else if (temperature < 0) {
            return "It's freezing outside! Stay warm and play some cards! 🥶";
        } else if (temperature > 30) {
            return "It's very hot outside! Stay cool indoors with some card games! 🥵";
        } else {
            return "Decent weather, but card games are fun too! 🎴";
        }
    }
}

package com.example.weather;

import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/weather")
// NO @CrossOrigin - CORS handled by API Gateway
public class WeatherController {

    private final RestTemplate restTemplate = new RestTemplate();

    // Default location: Oslo
    private static final String DEFAULT_LAT = "59.9333";
    private static final String DEFAULT_LON = "10.7166";

    @GetMapping
    public ResponseEntity<Map<String, Object>> getWeather(
            @RequestParam(required = false, defaultValue = DEFAULT_LAT) String lat,
            @RequestParam(required = false, defaultValue = DEFAULT_LON) String lon) {

        try {
            // Build Met.no API URL
            String metNoUrl = String.format(
                    "https://api.met.no/weatherapi/nowcast/2.0/complete?lat=%s&lon=%s",
                    lat, lon
            );

            // Set required User-Agent header (Met.no requirement)
            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "CardGameWeatherService/1.0 github.com/yourproject");

            HttpEntity<String> entity = new HttpEntity<>(headers);

            // Call Met.no API
            ResponseEntity<Map> response = restTemplate.exchange(
                    metNoUrl,
                    HttpMethod.GET,
                    entity,
                    Map.class
            );

            // Extract and simplify the response with "go outside" logic
            Map<String, Object> weatherData = analyzeWeatherData(response.getBody());

            return ResponseEntity.ok(weatherData);

        } catch (Exception e) {
            // Return error response
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to fetch weather data");
            errorResponse.put("message", e.getMessage());
            errorResponse.put("temperature", 0);
            errorResponse.put("windSpeed", 0);
            errorResponse.put("condition", "unavailable");
            errorResponse.put("shouldGoOutside", false);
            errorResponse.put("message", "Weather data unavailable. Enjoy your game! 🎮");
            return ResponseEntity.status(HttpStatus.OK).body(errorResponse);
        }
    }

    private Map<String, Object> analyzeWeatherData(Map<String, Object> metNoData) {
        Map<String, Object> weatherResponse = new HashMap<>();

        try {
            // Extract relevant data from Met.no response
            Map<String, Object> properties = (Map<String, Object>) metNoData.get("properties");

            if (properties != null && properties.containsKey("timeseries")) {
                var timeseries = (java.util.List<?>) properties.get("timeseries");

                if (!timeseries.isEmpty()) {
                    Map<String, Object> firstEntry = (Map<String, Object>) timeseries.get(0);
                    Map<String, Object> data = (Map<String, Object>) firstEntry.get("data");
                    Map<String, Object> instant = (Map<String, Object>) data.get("instant");
                    Map<String, Object> details = (Map<String, Object>) instant.get("details");

                    // Extract weather details
                    double temperature = getDoubleValue(details.get("air_temperature"));
                    double windSpeed = getDoubleValue(details.get("wind_speed"));
                    double precipitation = 0.0;
                    String symbolCode = "unknown";

                    // Get precipitation and symbol code if available
                    if (data.containsKey("next_1_hours")) {
                        Map<String, Object> next1Hour = (Map<String, Object>) data.get("next_1_hours");
                        Map<String, Object> next1Details = (Map<String, Object>) next1Hour.get("details");
                        if (next1Details != null && next1Details.containsKey("precipitation_amount")) {
                            precipitation = getDoubleValue(next1Details.get("precipitation_amount"));
                        }

                        Map<String, Object> summary = (Map<String, Object>) next1Hour.get("summary");
                        if (summary != null && summary.containsKey("symbol_code")) {
                            symbolCode = summary.get("symbol_code").toString();
                        }
                    }

                    // Determine weather condition and advice
                    WeatherDecision decision = makeWeatherDecision(
                            temperature, windSpeed, precipitation, symbolCode
                    );

                    // Build response
                    weatherResponse.put("temperature", temperature);
                    weatherResponse.put("windSpeed", windSpeed);
                    weatherResponse.put("precipitation", precipitation);
                    weatherResponse.put("symbolCode", symbolCode);
                    weatherResponse.put("condition", decision.condition);
                    weatherResponse.put("shouldGoOutside", decision.shouldGoOutside);
                    weatherResponse.put("message", decision.message);
                    weatherResponse.put("source", "MET Norway");
                }
            }

        } catch (Exception e) {
            weatherResponse.put("error", "Failed to parse weather data");
            weatherResponse.put("temperature", 0);
            weatherResponse.put("windSpeed", 0);
            weatherResponse.put("condition", "unavailable");
            weatherResponse.put("shouldGoOutside", false);
            weatherResponse.put("message", "Weather data unavailable. Enjoy your game! 🎮");
        }

        return weatherResponse;
    }

    /**
     * Analyzes weather conditions and decides if user should go outside
     */
    private WeatherDecision makeWeatherDecision(
            double temperature, double windSpeed, double precipitation, String symbolCode) {

        WeatherDecision decision = new WeatherDecision();

        // Analyze symbol code for sunny conditions
        boolean isSunny = symbolCode.contains("clearsky") ||
                symbolCode.contains("fair") ||
                (symbolCode.contains("partlycloudy") && !symbolCode.contains("rain"));

        boolean isRaining = symbolCode.contains("rain") ||
                symbolCode.contains("sleet") ||
                precipitation > 0.5;

        boolean isStormy = symbolCode.contains("thunder") || windSpeed > 15;

        boolean isCold = temperature < 5;
        boolean isHot = temperature > 28;
        boolean isComfortable = temperature >= 15 && temperature <= 25;

        // Decision logic: Should go outside?
        if (isSunny && isComfortable && windSpeed < 10 && !isRaining) {
            // Perfect weather - must go outside!
            decision.shouldGoOutside = true;
            decision.condition = "sunny";
            decision.message = "Perfect weather! Stop playing and go outside! The sun is shining! ☀️";
        } else if (isSunny && !isRaining && windSpeed < 10) {
            // Sunny but not perfect temperature
            decision.shouldGoOutside = true;
            decision.condition = "sunny";
            if (isCold) {
                decision.message = "It's sunny but a bit chilly. Grab a jacket and enjoy the sunshine! 🧥☀️";
            } else if (isHot) {
                decision.message = "Beautiful sunny day! Maybe find some shade and enjoy it outside! 🌞";
            } else {
                decision.message = "Nice weather outside! Take a break and get some fresh air! 🌤️";
            }
        } else if (isRaining) {
            // Rainy - stay inside
            decision.shouldGoOutside = false;
            decision.condition = "rainy";
            decision.message = "It's raining outside. Perfect weather for gaming! 🌧️🎮";
        } else if (isStormy) {
            // Stormy - definitely stay inside
            decision.shouldGoOutside = false;
            decision.condition = "thunderstorm";
            decision.message = "Storm outside! Stay safe indoors and enjoy your game! ⛈️";
        } else if (symbolCode.contains("cloudy")) {
            // Cloudy but no rain
            decision.shouldGoOutside = false;
            decision.condition = "cloudy";
            decision.message = "Cloudy day - perfect atmosphere for gaming! ☁️🎮";
        } else if (symbolCode.contains("fog")) {
            decision.shouldGoOutside = false;
            decision.condition = "foggy";
            decision.message = "Foggy outside. Cozy gaming weather! 🌫️";
        } else if (symbolCode.contains("snow")) {
            decision.shouldGoOutside = false;
            decision.condition = "snowy";
            decision.message = "Snowy weather! Stay warm inside with your game! ❄️";
        } else {
            // Default: partly cloudy or unclear
            decision.shouldGoOutside = false;
            decision.condition = "partly_cloudy";
            decision.message = "Nice day for gaming! Enjoy! 🎮";
        }

        return decision;
    }

    private double getDoubleValue(Object value) {
        if (value == null) return 0.0;
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    /**
     * Inner class to hold weather decision data
     */
    private static class WeatherDecision {
        boolean shouldGoOutside = false;
        String condition = "unknown";
        String message = "Enjoy your game!";
    }
}
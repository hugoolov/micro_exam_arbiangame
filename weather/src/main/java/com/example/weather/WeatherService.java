package com.example.weather;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class WeatherService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    // User-Agent is REQUIRED by yr.no API
    private static final String USER_AGENT = "ArabianCardGame/1.0 (hugo@@harnaes.no)";
    private static final String YR_API_URL = "https://api.met.no/weatherapi/nowcast/2.0/complete";

    public WeatherService() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Fetch weather from yr.no API
     * @param lat Latitude
     * @param lon Longitude
     * @return WeatherResponse with current conditions
     */
    public WeatherResponse getWeather(double lat, double lon) {
        try {
            String url = String.format("%s?lat=%.4f&lon=%.4f", YR_API_URL, lat, lon);

            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", USER_AGENT);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            return parseWeatherResponse(response.getBody());

        } catch (RestClientException e) {
            System.err.println("Error fetching weather: " + e.getMessage());
            return createErrorResponse();
        }
    }


    /**
     * Parse the yr.no JSON response
     */
    private WeatherResponse parseWeatherResponse(String jsonResponse) {
        try {
            JsonNode root = objectMapper.readTree(jsonResponse);
            JsonNode timeseries = root.path("properties").path("timeseries");

            if (timeseries.isArray() && timeseries.size() > 0) {
                JsonNode current = timeseries.get(0);
                JsonNode data = current.path("data").path("instant").path("details");
                JsonNode next1h = current.path("data").path("next_1_hours");

                double temperature = data.path("air_temperature").asDouble();
                double windSpeed = data.path("wind_speed").asDouble();
                String symbolCode = next1h.path("summary").path("symbol_code").asText("unknown");

                return new WeatherResponse(
                        temperature,
                        windSpeed,
                        symbolCode,
                        determineCondition(symbolCode),
                        shouldGoOutside(symbolCode, temperature)
                );
            }

            return createErrorResponse();

        } catch (Exception e) {
            System.err.println("Error parsing weather response: " + e.getMessage());
            return createErrorResponse();
        }
    }

    /**
     * Determine weather condition from symbol code
     */
    private String determineCondition(String symbolCode) {
        if (symbolCode.contains("clearsky") || symbolCode.contains("fair")) {
            return "sunny";
        } else if (symbolCode.contains("partlycloudy")) {
            return "partly_cloudy";
        } else if (symbolCode.contains("cloudy")) {
            return "cloudy";
        } else if (symbolCode.contains("rain") || symbolCode.contains("drizzle")) {
            return "rainy";
        } else if (symbolCode.contains("snow")) {
            return "snowy";
        } else if (symbolCode.contains("sleet")) {
            return "sleet";
        } else if (symbolCode.contains("thunder")) {
            return "thunderstorm";
        } else if (symbolCode.contains("fog")) {
            return "foggy";
        }
        return "unknown";
    }

    /**
     * Determine if user should go outside based on weather
     */
    private boolean shouldGoOutside(String symbolCode, double temperature) {
        // Suggest going outside if:
        // - Clear or fair weather (sunny)
        // - Temperature is pleasant (between 15-25°C)
        boolean isSunny = symbolCode.contains("clearsky") || symbolCode.contains("fair");
        boolean pleasantTemp = temperature >= 15 && temperature <= 25;

        return isSunny && pleasantTemp;
    }

    /**
     * Create error response when API fails
     */
    private WeatherResponse createErrorResponse() {
        return new WeatherResponse(
                0.0,
                0.0,
                "unknown",
                "unavailable",
                false
        );
    }
}
package org.hackathon12.shophub.infrastructure.weather.openmeteo;

import org.hackathon12.shophub.domain.weather.model.WeatherSnapshot;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class OpenMeteoWeatherClient {

    private final RestClient geocodingClient;
    private final RestClient forecastClient;
    private final OpenMeteoProperties properties;

    public OpenMeteoWeatherClient(RestClient.Builder restClientBuilder, OpenMeteoProperties properties) {
        this.geocodingClient = restClientBuilder.clone().baseUrl(properties.geocodingBaseUrl()).build();
        this.forecastClient = restClientBuilder.clone().baseUrl(properties.forecastBaseUrl()).build();
        this.properties = properties;
    }

    public Optional<WeatherSnapshot> lookupByAddress(String address) {
        Coordinates coordinates = resolveCoordinates(address);
        return fetchCurrentWeather(coordinates.latitude(), coordinates.longitude());
    }

    private Coordinates resolveCoordinates(String address) {
        for (String query : geocodingQueries(address)) {
            Optional<Coordinates> coordinates = geocode(query);
            if (coordinates.isPresent()) {
                return coordinates.get();
            }
        }
        return new Coordinates(properties.defaultLatitude(), properties.defaultLongitude());
    }

    private Optional<WeatherSnapshot> fetchCurrentWeather(double latitude, double longitude) {
        try {
            OpenMeteoForecastResponse response = forecastClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v1/forecast")
                            .queryParam("latitude", latitude)
                            .queryParam("longitude", longitude)
                            .queryParam("current", "temperature_2m,weather_code")
                            .queryParam("timezone", properties.timezone())
                            .build())
                    .retrieve()
                    .body(OpenMeteoForecastResponse.class);
            if (response == null || response.current() == null) {
                return Optional.empty();
            }

            OpenMeteoForecastResponse.CurrentWeather current = response.current();
            int weatherCode = current.weatherCode();
            int temperature = (int) Math.round(current.temperature2m());
            if (weatherCode < 0) {
                return Optional.empty();
            }
            return Optional.of(new WeatherSnapshot(toConditionLabel(weatherCode), temperature, weatherCode));
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    private Optional<Coordinates> geocode(String query) {
        if (!StringUtils.hasText(query)) {
            return Optional.empty();
        }

        try {
            OpenMeteoGeocodingResponse response = geocodingClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v1/search")
                            .queryParam("name", query)
                            .queryParam("count", 1)
                            .queryParam("language", "ko")
                            .queryParam("format", "json")
                            .build())
                    .retrieve()
                    .body(OpenMeteoGeocodingResponse.class);
            if (response == null || response.results() == null || response.results().isEmpty()) {
                return Optional.empty();
            }

            OpenMeteoGeocodingResponse.GeocodingResult first = response.results().getFirst();
            return Optional.of(new Coordinates(first.latitude(), first.longitude()));
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    private List<String> geocodingQueries(String address) {
        if (!StringUtils.hasText(address)) {
            return List.of();
        }

        String trimmed = address.trim();
        List<String> queries = new ArrayList<>();
        queries.add(trimmed);

        String[] tokens = trimmed.split("\\s+");
        if (tokens.length > 0 && !tokens[0].equals(trimmed)) {
            queries.add(tokens[0]);
        }
        if (tokens.length > 1) {
            queries.add(tokens[0] + " " + tokens[1]);
        }
        return queries;
    }

    private String toConditionLabel(int weatherCode) {
        return switch (weatherCode) {
            case 0 -> "맑음";
            case 1, 2, 3 -> "구름";
            case 45, 48 -> "안개";
            case 51, 53, 55, 56, 57 -> "이슬비";
            case 61, 63, 65, 66, 67, 80, 81, 82 -> "비";
            case 71, 73, 75, 77, 85, 86 -> "눈";
            case 95, 96, 99 -> "뇌우";
            default -> "흐림";
        };
    }

    private record Coordinates(double latitude, double longitude) {
    }
}

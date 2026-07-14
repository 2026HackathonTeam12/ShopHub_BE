package org.hackathon12.shophub.domain.weather.model;

public record WeatherSnapshot(
        String conditionLabel,
        int temperatureCelsius,
        int weatherCode
) {

    public String summaryLabel() {
        return conditionLabel + " · " + temperatureCelsius + "°C";
    }
}

package org.hackathon12.shophub.infrastructure.weather.openmeteo;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "open-meteo")
public record OpenMeteoProperties(
        String geocodingBaseUrl,
        String forecastBaseUrl,
        double defaultLatitude,
        double defaultLongitude,
        String timezone
) {
}

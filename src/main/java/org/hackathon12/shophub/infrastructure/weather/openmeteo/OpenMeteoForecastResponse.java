package org.hackathon12.shophub.infrastructure.weather.openmeteo;

import com.fasterxml.jackson.annotation.JsonProperty;

record OpenMeteoForecastResponse(CurrentWeather current) {

    record CurrentWeather(
            @JsonProperty("temperature_2m") double temperature2m,
            @JsonProperty("weather_code") int weatherCode
    ) {
    }
}

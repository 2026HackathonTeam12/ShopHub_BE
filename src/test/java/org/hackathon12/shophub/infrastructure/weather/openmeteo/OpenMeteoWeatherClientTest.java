package org.hackathon12.shophub.infrastructure.weather.openmeteo;

import org.hackathon12.shophub.domain.weather.model.WeatherSnapshot;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.client.RestClient;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class OpenMeteoWeatherClientTest {

    @Autowired
    private RestClient.Builder restClientBuilder;

    @Test
    void lookupByAddress_returns_weather_for_seed_store_address() {
        OpenMeteoWeatherClient client = new OpenMeteoWeatherClient(
                restClientBuilder,
                new OpenMeteoProperties(
                        "https://geocoding-api.open-meteo.com",
                        "https://api.open-meteo.com",
                        37.5665,
                        126.9780,
                        "Asia/Seoul"
                )
        );

        Optional<WeatherSnapshot> weather = client.lookupByAddress("서울 마포구 동교로 242");

        assertTrue(weather.isPresent(), "weather lookup should succeed, got empty");
        assertTrue(weather.get().summaryLabel().contains("°C"));
    }
}

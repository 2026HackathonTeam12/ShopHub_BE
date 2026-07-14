package org.hackathon12.shophub.infrastructure.weather.openmeteo;

import java.util.List;

record OpenMeteoGeocodingResponse(List<GeocodingResult> results) {

    record GeocodingResult(
            double latitude,
            double longitude
    ) {
    }
}

package org.hackathon12.shophub.domain.store.model;

public record BusinessHour(
        String dayOfWeek,
        String openTime,
        String closeTime,
        boolean open
) {
}

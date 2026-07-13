package org.hackathon12.shophub.domain.store.model;

import java.util.UUID;

public record MenuItem(
        UUID id,
        String name,
        String description
) {
}

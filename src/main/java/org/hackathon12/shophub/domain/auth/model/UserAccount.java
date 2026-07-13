package org.hackathon12.shophub.domain.auth.model;

import java.util.UUID;

public record UserAccount(
        UUID id,
        String email,
        String password,
        String name
) {
}

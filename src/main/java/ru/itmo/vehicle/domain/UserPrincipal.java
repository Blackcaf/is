package ru.itmo.vehicle.domain;

import java.io.Serializable;

public record UserPrincipal(Long id, String username, UserRole role) implements Serializable {
    public boolean isAdmin() {
        return role == UserRole.ADMIN;
    }
}

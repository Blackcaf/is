package ru.itmo.vehicle.web;

import ru.itmo.vehicle.domain.UserRole;

public record UserDto(Long id, String username, UserRole role) {
}

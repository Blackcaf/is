package ru.itmo.vehicle.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import ru.itmo.vehicle.domain.AppUser;
import ru.itmo.vehicle.domain.UserPrincipal;
import ru.itmo.vehicle.exception.UnauthorizedException;
import ru.itmo.vehicle.repo.AppUserRepository;

@Service
public class AuthService {
    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(AppUserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
    public UserPrincipal authenticate(String username, String password) {
        if (username == null || username.isBlank() || password == null) {
            throw new UnauthorizedException("Неверное имя пользователя или пароль");
        }
        AppUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UnauthorizedException("Неверное имя пользователя или пароль"));
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new UnauthorizedException("Неверное имя пользователя или пароль");
        }
        return new UserPrincipal(user.getId(), user.getUsername(), user.getRole());
    }
}

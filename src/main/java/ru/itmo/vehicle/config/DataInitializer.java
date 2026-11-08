package ru.itmo.vehicle.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import ru.itmo.vehicle.domain.AppUser;
import ru.itmo.vehicle.domain.UserRole;
import ru.itmo.vehicle.repo.AppUserRepository;

@Component
public class DataInitializer implements ApplicationRunner {
    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final boolean enabled;

    public DataInitializer(AppUserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           @Value("${app.users.seed-enabled:true}") boolean enabled) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.enabled = enabled;
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void run(ApplicationArguments args) {
        if (!enabled) {
            return;
        }
        createIfMissing("admin", "admin", UserRole.ADMIN);
        createIfMissing("admin2", "admin2", UserRole.ADMIN);
        createIfMissing("user", "user", UserRole.USER);
        createIfMissing("user2", "user2", UserRole.USER);
    }

    private void createIfMissing(String username, String password, UserRole role) {
        if (userRepository.findByUsername(username).isPresent()) {
            return;
        }
        userRepository.save(new AppUser(username, passwordEncoder.encode(password), role));
    }
}

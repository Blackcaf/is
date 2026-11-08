package ru.itmo.vehicle.domain;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

@Entity
@Table(name = "app_user",
        uniqueConstraints = @UniqueConstraint(name = "uk_app_user_username", columnNames = "username"))
public class AppUser implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "username", nullable = false, length = 100)
    @NotBlank(message = "Имя пользователя не может быть пустым")
    private String username;

    @Column(name = "password_hash", nullable = false, length = 100)
    @NotBlank(message = "Хеш пароля не может быть пустым")
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    @NotNull(message = "Роль пользователя не может быть null")
    private UserRole role;

    public AppUser() {
    }

    public AppUser(String username, String passwordHash, UserRole role) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    @Override
    public String toString() {
        return "AppUser{id=" + id + ", username='" + username + "', role=" + role + '}';
    }
}

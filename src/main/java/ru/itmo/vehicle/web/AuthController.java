package ru.itmo.vehicle.web;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.itmo.vehicle.domain.UserPrincipal;
import ru.itmo.vehicle.service.AuthService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;
    private final WebMapper mapper;

    public AuthController(AuthService authService, WebMapper mapper) {
        this.authService = authService;
        this.mapper = mapper;
    }

    @PostMapping("/login")
    public UserDto login(@RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        UserPrincipal principal = authService.authenticate(request.username, request.password);
        HttpSession session = httpRequest.getSession(true);
        httpRequest.changeSessionId();
        session.setAttribute(AuthInterceptor.SESSION_USER, principal);
        return mapper.toDto(principal);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }

    @GetMapping("/me")
    public UserDto me(@RequestAttribute(AuthInterceptor.REQUEST_USER) UserPrincipal principal) {
        return mapper.toDto(principal);
    }
}

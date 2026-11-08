package ru.itmo.vehicle.web;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import ru.itmo.vehicle.domain.UserPrincipal;
import ru.itmo.vehicle.exception.UnauthorizedException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@Component
public class AuthInterceptor implements HandlerInterceptor {
    public static final String SESSION_USER = "vehicleIsUser";
    public static final String REQUEST_USER = "vehicleIsCurrentUser";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        HttpSession session = request.getSession(false);
        UserPrincipal user = session == null ? null : (UserPrincipal) session.getAttribute(SESSION_USER);
        if (user == null) {
            throw new UnauthorizedException("Требуется авторизация");
        }
        request.setAttribute(REQUEST_USER, user);
        return true;
    }
}

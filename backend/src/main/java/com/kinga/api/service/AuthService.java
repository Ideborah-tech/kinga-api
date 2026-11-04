package com.kinga.api.service;

import com.kinga.api.model.AppUser;
import com.kinga.api.model.AuthSession;
import com.kinga.api.repository.AppUserRepository;
import com.kinga.api.repository.AuthSessionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Locale;
import java.util.Optional;

/** Create accounts, sign in and out, and find the user behind a token. */
@Service
public class AuthService {

    public record Session(String token, AppUser user) {
    }

    private final AppUserRepository users;
    private final AuthSessionRepository sessions;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private final SecureRandom random = new SecureRandom();

    public AuthService(AppUserRepository users, AuthSessionRepository sessions) {
        this.users = users;
        this.sessions = sessions;
    }

    public AppUser createUser(String name, String email, String password, String handle) {
        String cleanEmail = email.trim().toLowerCase(Locale.ROOT);
        if (users.existsByEmailIgnoreCase(cleanEmail)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An account with this email already exists");
        }
        AppUser user = new AppUser();
        user.name = name.trim();
        user.email = cleanEmail;
        user.handle = handle != null ? handle : uniqueHandle(user.name, cleanEmail);
        user.passwordHash = encoder.encode(password);
        return users.save(user);
    }

    public Session register(String name, String email, String password) {
        return startSession(createUser(name, email, password, null));
    }

    public Session login(String email, String password) {
        AppUser user = users.findByEmailIgnoreCase(email.trim())
                .filter(u -> encoder.matches(password, u.passwordHash))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Wrong email or password"));
        return startSession(user);
    }

    public Session loginAs(String email) {
        AppUser user = users.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new NotFoundException("Account " + email + " not found"));
        return startSession(user);
    }

    public Optional<AppUser> userForToken(String token) {
        if (token == null || token.isBlank()) return Optional.empty();
        return sessions.findById(token).flatMap(s -> users.findById(s.userId));
    }

    public void logout(String token) {
        if (token != null) sessions.deleteById(token);
    }

    private Session startSession(AppUser user) {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        AuthSession session = new AuthSession();
        session.token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        session.userId = user.id;
        sessions.save(session);
        return new Session(session.token, user);
    }

    /** "Amina Wanjiru" -> "amina-wanjiru", with a number added if it is taken. */
    private String uniqueHandle(String name, String email) {
        String base = name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
        if (base.isEmpty()) base = email.substring(0, email.indexOf('@')).replaceAll("[^a-z0-9]+", "-");
        if (base.isEmpty()) base = "developer";
        String handle = base;
        for (int i = 2; users.existsByHandle(handle); i++) handle = base + "-" + i;
        return handle;
    }
}

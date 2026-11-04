package com.kinga.api.controller;

import com.kinga.api.model.AppUser;
import com.kinga.api.seed.DemoDataSeeder;
import com.kinga.api.service.AuthService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    public record RegisterRequest(@NotBlank @Size(max = 80) String name,
                                  @NotBlank @Email String email,
                                  @NotBlank @Size(min = 8, max = 100, message = "must be at least 8 characters") String password) {
    }

    public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {
    }

    private final AuthService auth;

    public AuthController(AuthService auth) {
        this.auth = auth;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthService.Session register(@Valid @RequestBody RegisterRequest r) {
        return auth.register(r.name(), r.email(), r.password());
    }

    @PostMapping("/login")
    public AuthService.Session login(@Valid @RequestBody LoginRequest r) {
        return auth.login(r.email(), r.password());
    }

    /** One-click sign-in to the seeded demo account, for judges and the live demo. */
    @PostMapping("/demo")
    public AuthService.Session demo() {
        return auth.loginAs(DemoDataSeeder.DEMO_EMAIL);
    }

    @GetMapping("/me")
    public AppUser me(@RequestHeader(value = "Authorization", required = false) String authorization) {
        return auth.userForToken(bearer(authorization))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not signed in"));
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@RequestHeader(value = "Authorization", required = false) String authorization) {
        auth.logout(bearer(authorization));
    }

    private static String bearer(String header) {
        return header != null && header.startsWith("Bearer ") ? header.substring(7).trim() : null;
    }
}

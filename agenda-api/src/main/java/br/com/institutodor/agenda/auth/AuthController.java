package br.com.institutodor.agenda.auth;

import br.com.institutodor.agenda.user.repository.UserRepository;
import java.util.Map;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UserRepository userRepository;

    public AuthController(UserRepository userRepository) { this.userRepository = userRepository; }

    @GetMapping("/me")
    public ResponseEntity<?> me(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof OAuth2User googleUser)) {
            return ResponseEntity.status(401).body(Map.of("code", "UNAUTHENTICATED", "message", "Login necessario."));
        }
        String googleId = googleUser.getAttribute("sub");
        return userRepository.findByGoogleId(googleId)
            .map(user -> ResponseEntity.ok(Map.of("id", user.getId(), "name", user.getName(), "email", user.getEmail())))
            .orElseGet(() -> ResponseEntity.status(401).body(Map.of("code", "UNAUTHENTICATED", "message", "Login necessario.")));
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(HttpServletRequest request) {
        var session = request.getSession(false);
        if (session != null) session.invalidate();
    }
}

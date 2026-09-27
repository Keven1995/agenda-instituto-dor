package br.com.institutodor.agenda.config;

import br.com.institutodor.agenda.user.entity.User;
import br.com.institutodor.agenda.user.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;

public class GoogleLoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
    private final String frontendUrl;
    private final UserRepository userRepository;

    public GoogleLoginSuccessHandler(String frontendUrl, UserRepository userRepository) {
        this.frontendUrl = frontendUrl;
        this.userRepository = userRepository;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        var googleUser = (OAuth2User) authentication.getPrincipal();
        String googleId = googleUser.getAttribute("sub");
        String email = googleUser.getAttribute("email");
        String name = googleUser.getAttribute("name");
        userRepository.findByGoogleId(googleId).orElseGet(() ->
            userRepository.save(new User(UUID.randomUUID(), googleId, email, name)));
        getRedirectStrategy().sendRedirect(request, response, frontendUrl);
    }
}

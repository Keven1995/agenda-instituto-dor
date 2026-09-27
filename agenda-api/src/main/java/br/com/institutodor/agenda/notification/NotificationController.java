package br.com.institutodor.agenda.notification;

import br.com.institutodor.agenda.appointment.exception.ResourceNotFoundException;
import br.com.institutodor.agenda.user.repository.UserRepository;
import jakarta.validation.Valid;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    private final PushSubscriptionRepository subscriptions;
    private final UserRepository users;
    private final String vapidPublicKey;

    public NotificationController(PushSubscriptionRepository subscriptions, UserRepository users,
                                  @Value("${app.push.vapid-public-key:}") String vapidPublicKey) {
        this.subscriptions = subscriptions;
        this.users = users;
        this.vapidPublicKey = vapidPublicKey;
    }

    @GetMapping("/vapid-public-key")
    public Map<String, String> publicKey() { return Map.of("publicKey", vapidPublicKey); }

    @PostMapping("/subscriptions")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void subscribe(Authentication authentication, @Valid @RequestBody PushSubscriptionRequest request) {
        var user = findUser(authentication);
        subscriptions.findByEndpoint(request.endpoint()).ifPresentOrElse(
            subscription -> subscription.refreshKeys(request.p256dh(), request.auth()),
            () -> subscriptions.save(new PushSubscription(user, request.endpoint(), request.p256dh(), request.auth())));
    }

    private br.com.institutodor.agenda.user.entity.User findUser(Authentication authentication) {
        try {
            return users.findById(UUID.fromString(authentication.getName())).orElseThrow(ResourceNotFoundException::new);
        } catch (IllegalArgumentException exception) {
            return users.findByGoogleId(authentication.getName()).orElseThrow(ResourceNotFoundException::new);
        }
    }
}

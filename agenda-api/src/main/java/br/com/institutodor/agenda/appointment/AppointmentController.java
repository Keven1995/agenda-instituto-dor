package br.com.institutodor.agenda.appointment;

import br.com.institutodor.agenda.appointment.dto.AppointmentRequest;
import br.com.institutodor.agenda.appointment.dto.AppointmentResponse;
import br.com.institutodor.agenda.appointment.exception.ResourceNotFoundException;
import br.com.institutodor.agenda.user.repository.UserRepository;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {
    private final AppointmentService service;
    private final UserRepository userRepository;

    public AppointmentController(AppointmentService service, UserRepository userRepository) {
        this.service = service;
        this.userRepository = userRepository;
    }

    @GetMapping
    public List<AppointmentResponse> list(Authentication authentication,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant start,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant end) {
        return service.list(userId(authentication), start, end);
    }

    @GetMapping("/{id}")
    public AppointmentResponse get(Authentication authentication, @PathVariable UUID id) {
        return service.getById(userId(authentication), id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AppointmentResponse create(Authentication authentication, @Valid @RequestBody AppointmentRequest request) {
        return service.create(userId(authentication), request);
    }

    @PutMapping("/{id}")
    public AppointmentResponse update(Authentication authentication, @PathVariable UUID id,
                                      @Valid @RequestBody AppointmentRequest request) {
        return service.update(userId(authentication), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancel(Authentication authentication, @PathVariable UUID id) {
        service.cancel(userId(authentication), id);
    }

    private UUID userId(Authentication authentication) {
        try {
            return UUID.fromString(authentication.getName());
        } catch (IllegalArgumentException exception) {
            return userRepository.findByGoogleId(authentication.getName())
                .orElseThrow(ResourceNotFoundException::new).getId();
        }
    }
}

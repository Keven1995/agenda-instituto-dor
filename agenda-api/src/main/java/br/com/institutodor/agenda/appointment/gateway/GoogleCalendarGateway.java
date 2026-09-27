package br.com.institutodor.agenda.appointment.gateway;

import br.com.institutodor.agenda.appointment.entity.Appointment;
import br.com.institutodor.agenda.appointment.exception.CalendarSynchronizationException;
import java.time.ZoneId;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
public class GoogleCalendarGateway implements CalendarGateway {
    private static final String CALENDAR_EVENTS_URL = "https://www.googleapis.com/calendar/v3/calendars/primary/events";

    private final RestClient restClient;
    private final OAuth2AuthorizedClientManager authorizedClientManager;
    private final GoogleCalendarEventMapper eventMapper;

    public GoogleCalendarGateway(RestClient.Builder restClientBuilder,
                                 OAuth2AuthorizedClientManager authorizedClientManager,
                                 @Value("${app.time-zone:America/Sao_Paulo}") String timeZone) {
        this.restClient = restClientBuilder.build();
        this.authorizedClientManager = authorizedClientManager;
        this.eventMapper = new GoogleCalendarEventMapper(ZoneId.of(timeZone));
    }

    @Override
    public String create(Appointment appointment) {
        try {
            var response = restClient.post()
                .uri(CALENDAR_EVENTS_URL)
                .headers(headers -> headers.setBearerAuth(accessToken()))
                .contentType(MediaType.APPLICATION_JSON)
                .body(eventMapper.toEvent(appointment))
                .retrieve()
                .body(Map.class);
            var eventId = response == null ? null : response.get("id");
            if (!(eventId instanceof String id) || id.isBlank()) {
                throw new CalendarSynchronizationException("O Google Calendar nao retornou o identificador do evento.");
            }
            return id;
        } catch (RestClientException exception) {
            throw synchronizationFailure("criar", exception);
        }
    }

    @Override
    public void update(Appointment appointment) {
        if (appointment.getGoogleEventId() == null || appointment.getGoogleEventId().isBlank()) {
            throw new CalendarSynchronizationException("O agendamento nao possui evento correspondente no Google Calendar.");
        }
        try {
            restClient.patch()
                .uri(CALENDAR_EVENTS_URL + "/{eventId}", appointment.getGoogleEventId())
                .headers(headers -> headers.setBearerAuth(accessToken()))
                .contentType(MediaType.APPLICATION_JSON)
                .body(eventMapper.toEvent(appointment))
                .retrieve()
                .toBodilessEntity();
        } catch (RestClientException exception) {
            throw synchronizationFailure("atualizar", exception);
        }
    }

    @Override
    public void delete(Appointment appointment) {
        if (appointment.getGoogleEventId() == null || appointment.getGoogleEventId().isBlank()) return;
        try {
            restClient.delete()
                .uri(CALENDAR_EVENTS_URL + "/{eventId}", appointment.getGoogleEventId())
                .headers(headers -> headers.setBearerAuth(accessToken()))
                .retrieve()
                .toBodilessEntity();
        } catch (RestClientException exception) {
            throw synchronizationFailure("cancelar", exception);
        }
    }

    private String accessToken() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new CalendarSynchronizationException("Nao existe uma sessao Google autenticada.");
        }
        var request = OAuth2AuthorizeRequest.withClientRegistrationId("google")
            .principal(authentication)
            .attribute(HttpServletRequest.class.getName(), currentRequest())
            .attribute(HttpServletResponse.class.getName(), currentResponse())
            .build();
        var authorizedClient = authorizedClientManager.authorize(request);
        if (authorizedClient == null || authorizedClient.getAccessToken() == null) {
            throw new CalendarSynchronizationException("Nao foi possivel obter um token do Google Calendar.");
        }
        return authorizedClient.getAccessToken().getTokenValue();
    }

    private CalendarSynchronizationException synchronizationFailure(String operation, RestClientException exception) {
        if (exception instanceof RestClientResponseException responseException) {
            var detail = responseException.getResponseBodyAsString();
            return new CalendarSynchronizationException(
                "Nao foi possivel " + operation + " o evento no Google Calendar (HTTP "
                    + responseException.getStatusCode().value() + "): " + detail, exception);
        }
        return new CalendarSynchronizationException(
            "Nao foi possivel " + operation + " o evento no Google Calendar: " + exception.getMessage(), exception);
    }

    private HttpServletRequest currentRequest() {
        var attributes = RequestContextHolder.getRequestAttributes();
        if (!(attributes instanceof ServletRequestAttributes servletAttributes)) {
            throw new CalendarSynchronizationException("Nao foi possivel acessar a sessao HTTP do usuario.");
        }
        return servletAttributes.getRequest();
    }

    private HttpServletResponse currentResponse() {
        var attributes = RequestContextHolder.getRequestAttributes();
        if (!(attributes instanceof ServletRequestAttributes servletAttributes) || servletAttributes.getResponse() == null) {
            throw new CalendarSynchronizationException("Nao foi possivel acessar a resposta HTTP do usuario.");
        }
        return servletAttributes.getResponse();
    }
}

package br.com.institutodor.agenda.appointment.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.institutodor.agenda.appointment.entity.Appointment;
import br.com.institutodor.agenda.user.entity.User;
import java.time.Instant;
import java.time.ZoneId;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class GoogleCalendarEventMapperTest {
    @Test
    void createsMinimalEventWithoutPatientInformation() {
        var user = new User(UUID.randomUUID(), "google-1", "keven@example.com", "Keven");
        var appointment = Appointment.scheduled(user, "Paciente confidencial", Instant.parse("2026-09-28T17:00:00Z"), Instant.parse("2026-09-28T19:00:00Z"));

        var event = new GoogleCalendarEventMapper(ZoneId.of("America/Sao_Paulo")).toEvent(appointment);

        assertThat(event).containsEntry("summary", "Atendimento");
        assertThat(event).doesNotContainKey("description");
        assertThat(event.get("start")).isEqualTo(java.util.Map.of("dateTime", "2026-09-28T14:00:00-03:00", "timeZone", "America/Sao_Paulo"));
    }
}

package br.com.institutodor.agenda.appointment.gateway;

import br.com.institutodor.agenda.appointment.entity.Appointment;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

public class GoogleCalendarEventMapper {
    private final ZoneId timeZone;

    public GoogleCalendarEventMapper(ZoneId timeZone) {
        this.timeZone = timeZone;
    }

    public Map<String, Object> toEvent(Appointment appointment) {
        return Map.of(
            "summary", "Atendimento",
            "start", dateTime(appointment.getStartAt()),
            "end", dateTime(appointment.getEndAt())
        );
    }

    private Map<String, String> dateTime(Instant value) {
        ZonedDateTime localDateTime = value.atZone(timeZone);
        var formatted = localDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX"));
        return Map.of("dateTime", formatted, "timeZone", timeZone.getId());
    }
}

package br.com.institutodor.agenda.appointment.dto;

import br.com.institutodor.agenda.appointment.entity.Appointment;
import br.com.institutodor.agenda.appointment.entity.AppointmentStatus;
import java.time.Instant;
import java.util.UUID;

public record AppointmentResponse(UUID id, String patientName, Instant startAt, Instant endAt,
                                  String googleEventId, AppointmentStatus status) {
    public static AppointmentResponse from(Appointment appointment) {
        return new AppointmentResponse(appointment.getId(), appointment.getPatientName(), appointment.getStartAt(),
            appointment.getEndAt(), appointment.getGoogleEventId(), appointment.getStatus());
    }
}

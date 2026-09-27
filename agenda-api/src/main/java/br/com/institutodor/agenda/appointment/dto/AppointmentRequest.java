package br.com.institutodor.agenda.appointment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public record AppointmentRequest(
    @NotBlank(message = "O nome do paciente e obrigatorio") String patientName,
    @NotNull(message = "O inicio e obrigatorio") Instant startAt,
    @NotNull(message = "O termino e obrigatorio") Instant endAt
) { }

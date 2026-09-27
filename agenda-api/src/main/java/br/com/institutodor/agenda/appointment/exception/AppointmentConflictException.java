package br.com.institutodor.agenda.appointment.exception;

public class AppointmentConflictException extends RuntimeException {
    public AppointmentConflictException() { super("Ja existe um atendimento neste periodo."); }
}

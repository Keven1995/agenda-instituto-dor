package br.com.institutodor.agenda.appointment.exception;

public class InvalidAppointmentPeriodException extends RuntimeException {
    public InvalidAppointmentPeriodException() { super("O termino deve ser posterior ao inicio."); }
}

package br.com.institutodor.agenda.appointment.exception;

public class CalendarSynchronizationException extends RuntimeException {
    public CalendarSynchronizationException(String message, Throwable cause) {
        super(message, cause);
    }

    public CalendarSynchronizationException(String message) {
        super(message);
    }
}

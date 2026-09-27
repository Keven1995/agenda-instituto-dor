package br.com.institutodor.agenda.config;

import br.com.institutodor.agenda.appointment.exception.AppointmentConflictException;
import br.com.institutodor.agenda.appointment.exception.InvalidAppointmentPeriodException;
import br.com.institutodor.agenda.appointment.exception.ResourceNotFoundException;
import br.com.institutodor.agenda.appointment.exception.CalendarSynchronizationException;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(AppointmentConflictException.class)
    ResponseEntity<Map<String, String>> conflict(AppointmentConflictException exception) {
        return response(HttpStatus.CONFLICT, "APPOINTMENT_TIME_CONFLICT", exception.getMessage());
    }

    @ExceptionHandler(InvalidAppointmentPeriodException.class)
    ResponseEntity<Map<String, String>> invalidPeriod(InvalidAppointmentPeriodException exception) {
        return response(HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_APPOINTMENT_PERIOD", exception.getMessage());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    ResponseEntity<Map<String, String>> notFound(ResourceNotFoundException exception) {
        return response(HttpStatus.NOT_FOUND, "APPOINTMENT_NOT_FOUND", exception.getMessage());
    }

    @ExceptionHandler(CalendarSynchronizationException.class)
    ResponseEntity<Map<String, String>> calendarFailure(CalendarSynchronizationException exception) {
        return response(HttpStatus.BAD_GATEWAY, "CALENDAR_SYNCHRONIZATION_FAILED", exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<Map<String, String>> invalidRequest(MethodArgumentNotValidException exception) {
        var message = exception.getBindingResult().getFieldErrors().stream().findFirst()
            .map(error -> error.getDefaultMessage()).orElse("Dados invalidos.");
        return response(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", message);
    }

    private ResponseEntity<Map<String, String>> response(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status).body(Map.of("code", code, "message", message));
    }
}

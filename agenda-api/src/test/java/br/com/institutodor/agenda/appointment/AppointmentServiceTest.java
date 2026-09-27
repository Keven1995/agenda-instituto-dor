package br.com.institutodor.agenda.appointment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.institutodor.agenda.appointment.dto.AppointmentRequest;
import br.com.institutodor.agenda.appointment.entity.Appointment;
import br.com.institutodor.agenda.appointment.entity.AppointmentStatus;
import br.com.institutodor.agenda.appointment.exception.AppointmentConflictException;
import br.com.institutodor.agenda.appointment.exception.InvalidAppointmentPeriodException;
import br.com.institutodor.agenda.appointment.exception.ResourceNotFoundException;
import br.com.institutodor.agenda.appointment.gateway.CalendarGateway;
import br.com.institutodor.agenda.appointment.repository.AppointmentRepository;
import br.com.institutodor.agenda.user.entity.User;
import br.com.institutodor.agenda.user.repository.UserRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AppointmentServiceTest {
    private static final UUID USER_ID = UUID.randomUUID();
    private static final Instant START = Instant.parse("2026-09-28T14:00:00Z");
    private static final Instant END = Instant.parse("2026-09-28T16:00:00Z");

    @Mock AppointmentRepository appointmentRepository;
    @Mock UserRepository userRepository;
    @Mock CalendarGateway calendarGateway;

    private AppointmentService service;
    private User user;

    @BeforeEach
    void setUp() {
        service = new AppointmentService(appointmentRepository, userRepository, calendarGateway);
        user = new User(USER_ID, "google-1", "keven@example.com", "Keven");
    }

    @Test
    void createsAppointmentWhenPeriodIsAvailable() {
        var request = new AppointmentRequest("Joao da Silva", START, END);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(appointmentRepository.existsOverlapping(eq(USER_ID), eq(START), eq(END), eq(null))).thenReturn(false);
        when(calendarGateway.create(any())).thenReturn("google-event-1");
        when(appointmentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.create(USER_ID, request);

        assertThat(result.patientName()).isEqualTo("Joao da Silva");
        assertThat(result.status()).isEqualTo(AppointmentStatus.SCHEDULED);
        assertThat(result.googleEventId()).isEqualTo("google-event-1");
        verify(appointmentRepository).save(any(Appointment.class));
    }

    @Test
    void rejectsAnInvalidPeriod() {
        var request = new AppointmentRequest("Joao da Silva", END, START);

        assertThatThrownBy(() -> service.create(USER_ID, request))
            .isInstanceOf(InvalidAppointmentPeriodException.class);

        verify(appointmentRepository, never()).save(any());
    }

    @Test
    void rejectsAnOverlappingAppointment() {
        var request = new AppointmentRequest("Joao da Silva", START.plusSeconds(30 * 60), END.plusSeconds(30 * 60));
        when(appointmentRepository.existsOverlapping(eq(USER_ID), eq(request.startAt()), eq(request.endAt()), eq(null))).thenReturn(true);

        assertThatThrownBy(() -> service.create(USER_ID, request))
            .isInstanceOf(AppointmentConflictException.class);

        verify(appointmentRepository, never()).save(any());
    }

    @Test
    void doesNotAllowAccessToAnotherUsersAppointment() {
        var appointmentId = UUID.randomUUID();
        when(appointmentRepository.findByIdAndUserId(appointmentId, USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(USER_ID, appointmentId))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void cancelsAppointmentAndDeletesCalendarEvent() {
        var appointment = Appointment.scheduled(user, "Joao da Silva", START, END);
        when(appointmentRepository.findByIdAndUserId(appointment.getId(), USER_ID)).thenReturn(Optional.of(appointment));

        service.cancel(USER_ID, appointment.getId());

        assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.CANCELLED);
        verify(calendarGateway).delete(appointment);
        verify(appointmentRepository).save(appointment);
    }
}

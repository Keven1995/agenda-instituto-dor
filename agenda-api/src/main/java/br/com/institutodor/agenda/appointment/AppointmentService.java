package br.com.institutodor.agenda.appointment;

import br.com.institutodor.agenda.appointment.dto.AppointmentRequest;
import br.com.institutodor.agenda.appointment.dto.AppointmentResponse;
import br.com.institutodor.agenda.appointment.entity.Appointment;
import br.com.institutodor.agenda.appointment.entity.AppointmentStatus;
import br.com.institutodor.agenda.appointment.exception.AppointmentConflictException;
import br.com.institutodor.agenda.appointment.exception.InvalidAppointmentPeriodException;
import br.com.institutodor.agenda.appointment.exception.ResourceNotFoundException;
import br.com.institutodor.agenda.appointment.gateway.CalendarGateway;
import br.com.institutodor.agenda.appointment.repository.AppointmentRepository;
import br.com.institutodor.agenda.user.repository.UserRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AppointmentService {
    private final AppointmentRepository appointmentRepository;
    private final UserRepository userRepository;
    private final CalendarGateway calendarGateway;

    public AppointmentService(AppointmentRepository appointmentRepository, UserRepository userRepository,
                              CalendarGateway calendarGateway) {
        this.appointmentRepository = appointmentRepository;
        this.userRepository = userRepository;
        this.calendarGateway = calendarGateway;
    }

    @Transactional
    public AppointmentResponse create(UUID userId, AppointmentRequest request) {
        validatePeriod(request.startAt(), request.endAt());
        if (appointmentRepository.existsOverlapping(userId, request.startAt(), request.endAt(), null)) {
            throw new AppointmentConflictException();
        }
        var user = userRepository.findById(userId).orElseThrow(ResourceNotFoundException::new);
        var appointment = Appointment.scheduled(user, request.patientName().trim(), request.startAt(), request.endAt());
        appointmentRepository.save(appointment);
        appointment.setGoogleEventId(calendarGateway.create(appointment));
        return AppointmentResponse.from(appointment);
    }

    @Transactional(readOnly = true)
    public List<AppointmentResponse> list(UUID userId, Instant start, Instant end) {
        return appointmentRepository.findByUserIdAndStatusAndStartAtLessThanAndEndAtGreaterThanOrderByStartAtAsc(
            userId, AppointmentStatus.SCHEDULED, end, start).stream().map(AppointmentResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public AppointmentResponse getById(UUID userId, UUID appointmentId) {
        return AppointmentResponse.from(findOwned(userId, appointmentId));
    }

    @Transactional
    public AppointmentResponse update(UUID userId, UUID appointmentId, AppointmentRequest request) {
        validatePeriod(request.startAt(), request.endAt());
        var appointment = findOwned(userId, appointmentId);
        if (appointment.getStatus() != AppointmentStatus.SCHEDULED) throw new ResourceNotFoundException();
        if (appointmentRepository.existsOverlapping(userId, request.startAt(), request.endAt(), appointmentId)) {
            throw new AppointmentConflictException();
        }
        appointment.update(request.patientName().trim(), request.startAt(), request.endAt());
        calendarGateway.update(appointment);
        return AppointmentResponse.from(appointmentRepository.save(appointment));
    }

    @Transactional
    public void cancel(UUID userId, UUID appointmentId) {
        var appointment = findOwned(userId, appointmentId);
        appointment.cancel();
        calendarGateway.delete(appointment);
        appointmentRepository.save(appointment);
    }

    private Appointment findOwned(UUID userId, UUID appointmentId) {
        return appointmentRepository.findByIdAndUserId(appointmentId, userId).orElseThrow(ResourceNotFoundException::new);
    }

    private void validatePeriod(Instant startAt, Instant endAt) {
        if (startAt == null || endAt == null || !endAt.isAfter(startAt)) throw new InvalidAppointmentPeriodException();
    }
}

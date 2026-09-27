package br.com.institutodor.agenda.appointment.repository;

import br.com.institutodor.agenda.appointment.entity.Appointment;
import br.com.institutodor.agenda.appointment.entity.AppointmentStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AppointmentRepository extends JpaRepository<Appointment, UUID> {
    Optional<Appointment> findByIdAndUserId(UUID id, UUID userId);

    @Query("""
        select case when count(a) > 0 then true else false end
        from Appointment a
        where a.user.id = :userId and a.status = 'SCHEDULED'
          and a.startAt < :endAt and a.endAt > :startAt
          and (:appointmentId is null or a.id <> :appointmentId)
        """)
    boolean existsOverlapping(@Param("userId") UUID userId, @Param("startAt") Instant startAt,
                              @Param("endAt") Instant endAt, @Param("appointmentId") UUID appointmentId);

    List<Appointment> findByUserIdAndStatusAndStartAtLessThanAndEndAtGreaterThanOrderByStartAtAsc(
        UUID userId, AppointmentStatus status, Instant endAt, Instant startAt);

    @Query("""
        select a from Appointment a
        where a.status = 'SCHEDULED' and a.notificationSentAt is null
          and a.startAt > :from and a.startAt <= :to
        """)
    List<Appointment> findDueForNotification(@Param("from") Instant from, @Param("to") Instant to);
}

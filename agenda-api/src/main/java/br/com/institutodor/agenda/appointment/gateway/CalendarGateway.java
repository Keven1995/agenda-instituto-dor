package br.com.institutodor.agenda.appointment.gateway;

import br.com.institutodor.agenda.appointment.entity.Appointment;

public interface CalendarGateway {
    String create(Appointment appointment);
    void update(Appointment appointment);
    void delete(Appointment appointment);
}

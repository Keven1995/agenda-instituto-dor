package br.com.institutodor.agenda.appointment.exception;

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException() { super("Agendamento nao encontrado."); }
}

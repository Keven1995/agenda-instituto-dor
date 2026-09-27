package br.com.institutodor.agenda.notification;

import br.com.institutodor.agenda.appointment.repository.AppointmentRepository;
import java.time.Duration;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AppointmentNotificationScheduler {
    private static final Logger log = LoggerFactory.getLogger(AppointmentNotificationScheduler.class);
    private final AppointmentRepository appointments;
    private final PushSubscriptionRepository subscriptions;
    private final WebPushSender sender;

    public AppointmentNotificationScheduler(AppointmentRepository appointments,
                                            PushSubscriptionRepository subscriptions,
                                            WebPushSender sender) {
        this.appointments = appointments;
        this.subscriptions = subscriptions;
        this.sender = sender;
    }

    @Scheduled(fixedDelayString = "${app.push.check-interval-ms:60000}")
    @Transactional
    public void sendDueNotifications() {
        if (!sender.isConfigured()) return;
        var now = Instant.now();
        var due = appointments.findDueForNotification(now.plus(Duration.ofMinutes(5)), now.plus(Duration.ofMinutes(6)));
        for (var appointment : due) {
            var sent = false;
            for (var subscription : subscriptions.findByUserId(appointment.getUser().getId())) {
                try {
                    sender.send(subscription, appointment);
                    sent = true;
                } catch (Exception exception) {
                    log.warn("Falha ao enviar notificacao Push para o endpoint do usuario {}", appointment.getUser().getId(), exception);
                }
            }
            if (sent) appointment.markNotificationSent();
        }
    }
}

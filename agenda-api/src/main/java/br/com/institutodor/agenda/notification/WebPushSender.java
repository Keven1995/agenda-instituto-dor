package br.com.institutodor.agenda.notification;

import br.com.institutodor.agenda.appointment.entity.Appointment;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class WebPushSender {
    private final String publicKey;
    private final String privateKey;
    private final String subject;
    private final ZoneId timeZone;

    public WebPushSender(@Value("${app.push.vapid-public-key:}") String publicKey,
                         @Value("${app.push.vapid-private-key:}") String privateKey,
                         @Value("${app.push.vapid-subject:mailto:admin@institutodor.com.br}") String subject,
                         @Value("${app.time-zone:America/Sao_Paulo}") String timeZone) {
        this.publicKey = publicKey;
        this.privateKey = privateKey;
        this.subject = subject;
        this.timeZone = ZoneId.of(timeZone);
    }

    public boolean isConfigured() { return !publicKey.isBlank() && !privateKey.isBlank(); }

    public void send(PushSubscription subscription, Appointment appointment) throws Exception {
        if (!isConfigured()) return;
        var time = appointment.getStartAt().atZone(timeZone)
            .format(DateTimeFormatter.ofPattern("HH:mm", Locale.ROOT));
        var payload = "{\"title\":\"Instituto da Dor\",\"body\":\"A consulta com "
            + escape(appointment.getPatientName()) + " está agendada para " + time + "\",\"url\":\"/\"}";
        var pushService = new PushService();
        pushService.setSubject(subject);
        pushService.setPublicKey(publicKey);
        pushService.setPrivateKey(privateKey);
        var notification = new Notification(subscription.getEndpoint(), subscription.getP256dh(), subscription.getAuth(), payload);
        pushService.send(notification);
    }

    private String escape(String value) { return value.replace("\\", "\\\\").replace("\"", "\\\""); }
}

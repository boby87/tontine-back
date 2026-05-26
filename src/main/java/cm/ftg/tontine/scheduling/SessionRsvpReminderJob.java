package cm.ftg.tontine.scheduling;

import cm.ftg.tontine.common.enums.MemberStatus;
import cm.ftg.tontine.member.entity.Member;
import cm.ftg.tontine.member.repository.MemberRepository;
import cm.ftg.tontine.notification.enums.NotificationCategory;
import cm.ftg.tontine.notification.enums.NotificationKind;
import cm.ftg.tontine.notification.service.NotificationService;
import cm.ftg.tontine.president.session.entity.Session;
import cm.ftg.tontine.president.session.enums.SessionStatus;
import cm.ftg.tontine.president.session.repository.SessionRepository;
import cm.ftg.tontine.secretary.rsvp.entity.SessionRsvp;
import cm.ftg.tontine.secretary.rsvp.enums.RsvpStatus;
import cm.ftg.tontine.secretary.rsvp.repository.SessionRsvpRepository;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnProperty(name = "app.scheduling.rsvp-reminders.enabled", havingValue = "true", matchIfMissing = true)
public class SessionRsvpReminderJob {

    private static final Logger log = LoggerFactory.getLogger(SessionRsvpReminderJob.class);
    private static final DateTimeFormatter HUMAN = DateTimeFormatter
            .ofPattern("EEEE d MMMM HH:mm", Locale.FRENCH);

    private final SessionRepository sessionRepository;
    private final SessionRsvpRepository rsvpRepository;
    private final MemberRepository memberRepository;
    private final NotificationService notificationService;

    public SessionRsvpReminderJob(SessionRepository sessionRepository,
                                  SessionRsvpRepository rsvpRepository,
                                  MemberRepository memberRepository,
                                  NotificationService notificationService) {
        this.sessionRepository = sessionRepository;
        this.rsvpRepository = rsvpRepository;
        this.memberRepository = memberRepository;
        this.notificationService = notificationService;
    }

    /** Tous les jours a 9h locale, notifie les membres sans RSVP pour les sessions a J-1 ou J-2. */
    @Scheduled(cron = "${app.scheduling.rsvp-reminders.cron:0 0 9 * * *}", zone = "Africa/Douala")
    @Transactional
    public void remindUpcoming() {
        Instant now = Instant.now();
        Instant windowStart = now.plus(Duration.ofHours(24));
        Instant windowEnd = now.plus(Duration.ofHours(48));
        List<Session> sessions = sessionRepository
                .findByStatusAndScheduledAtBetween(SessionStatus.SCHEDULED, windowStart, windowEnd);
        if (sessions.isEmpty()) {
            return;
        }
        int totalNotified = 0;
        for (Session s : sessions) {
            totalNotified += remindSession(s);
        }
        log.info("[SessionRsvpReminderJob] {} rappels envoyes pour {} sessions",
                totalNotified, sessions.size());
    }

    private int remindSession(Session s) {
        List<SessionRsvp> existing = rsvpRepository.findBySessionId(s.getId());
        Set<UUID> answered = new HashSet<>();
        for (SessionRsvp r : existing) {
            if (r.getStatus() != RsvpStatus.PENDING) {
                answered.add(r.getMemberId());
            }
        }
        List<Member> members = memberRepository.findByTontineId(s.getTontineId());
        int count = 0;
        String when = HUMAN.format(s.getScheduledAt().atZone(ZoneId.of("Africa/Douala")));
        String title = "Rappel : confirmez votre presence";
        String message = "Session #" + s.getNumber() + " prevue " + when
                + ". Merci de repondre via l'application.";
        String link = "/sessions/" + s.getId();
        for (Member m : members) {
            if (m.getUserId() == null || m.getStatus() != MemberStatus.ACTIVE) {
                continue;
            }
            if (answered.contains(m.getId())) {
                continue;
            }
            notificationService.publish(m.getUserId(), s.getTontineId(),
                    NotificationKind.INFO, NotificationCategory.SESSION,
                    title, message, link);
            count++;
        }
        return count;
    }
}

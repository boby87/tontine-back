package cm.ftg.tontine.scheduling;

import cm.ftg.tontine.member.entity.Member;
import cm.ftg.tontine.member.repository.MemberRepository;
import cm.ftg.tontine.notification.enums.NotificationCategory;
import cm.ftg.tontine.notification.enums.NotificationKind;
import cm.ftg.tontine.notification.service.NotificationService;
import cm.ftg.tontine.president.session.entity.Session;
import cm.ftg.tontine.president.session.enums.SessionStatus;
import cm.ftg.tontine.president.session.repository.SessionRepository;
import cm.ftg.tontine.treasurer.contribution.entity.Contribution;
import cm.ftg.tontine.treasurer.contribution.enums.ContributionStatus;
import cm.ftg.tontine.treasurer.contribution.repository.ContributionRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnProperty(name = "app.scheduling.arrears.enabled", havingValue = "true", matchIfMissing = true)
public class ContributionArrearsJob {

    private static final Logger log = LoggerFactory.getLogger(ContributionArrearsJob.class);
    private static final Duration GRACE = Duration.ofHours(24);

    private final ContributionRepository contributionRepository;
    private final SessionRepository sessionRepository;
    private final MemberRepository memberRepository;
    private final NotificationService notificationService;

    public ContributionArrearsJob(ContributionRepository contributionRepository,
                                   SessionRepository sessionRepository,
                                   MemberRepository memberRepository,
                                   NotificationService notificationService) {
        this.contributionRepository = contributionRepository;
        this.sessionRepository = sessionRepository;
        this.memberRepository = memberRepository;
        this.notificationService = notificationService;
    }

    /** Tous les jours a 10h locale, notifie les membres en retard. */
    @Scheduled(cron = "${app.scheduling.arrears.cron:0 0 10 * * *}", zone = "Africa/Douala")
    @Transactional
    public void detectArrears() {
        Instant cutoff = Instant.now().minus(GRACE);
        List<Session> pastSessions = sessionRepository
                .findByStatusAndScheduledAtBefore(SessionStatus.COMPLETED, cutoff);
        int totalNotified = 0;
        for (Session s : pastSessions) {
            List<Contribution> pending = contributionRepository
                    .findBySessionIdAndStatus(s.getId(), ContributionStatus.PENDING);
            for (Contribution c : pending) {
                Optional<Member> mb = memberRepository.findById(c.getMemberId());
                if (mb.isEmpty() || mb.get().getUserId() == null) {
                    continue;
                }
                notificationService.publish(
                        mb.get().getUserId(),
                        c.getTontineId(),
                        NotificationKind.WARNING,
                        NotificationCategory.CONTRIBUTION,
                        "Cotisation en retard",
                        "Votre cotisation pour la session #" + s.getNumber()
                                + " est en attente. Merci de regulariser.",
                        "/contributions/" + c.getId());
                totalNotified++;
            }
        }
        if (totalNotified > 0) {
            log.info("[ContributionArrearsJob] {} membres notifies pour retard", totalNotified);
        }
    }
}

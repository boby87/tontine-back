-- Table des invitations de membres (module president.membership.invitation).
-- Token public unique servant de lien d'acceptation (secret one-shot court-vivant).
CREATE TABLE membership_invitations (
    id                    UUID         NOT NULL,
    tontine_id            UUID         NOT NULL,
    candidate_full_name   VARCHAR(160) NOT NULL,
    candidate_phone       VARCHAR(20)  NOT NULL,
    candidate_email       VARCHAR(160),
    proposed_role         VARCHAR(20)  NOT NULL,
    channels              VARCHAR(100) NOT NULL,
    message               VARCHAR(1000),
    status                VARCHAR(20)  NOT NULL,
    token                 VARCHAR(80)  NOT NULL,
    sent_at               TIMESTAMP,
    accepted_at           TIMESTAMP,
    accepted_user_id      UUID,
    expires_at            TIMESTAMP    NOT NULL,
    invited_by_user_id    UUID         NOT NULL,
    invited_by_full_name  VARCHAR(160) NOT NULL,
    invited_at            TIMESTAMP    NOT NULL,
    reminders_sent        INT          NOT NULL DEFAULT 0,
    cancelled_at          TIMESTAMP,
    cancelled_by_user_id  UUID,
    cancel_reason         VARCHAR(500),
    version               BIGINT,
    CONSTRAINT pk_membership_invitations PRIMARY KEY (id),
    CONSTRAINT uk_invitation_token UNIQUE (token)
);

CREATE INDEX idx_invitation_tontine ON membership_invitations (tontine_id);
CREATE INDEX idx_invitation_status  ON membership_invitations (status);
CREATE INDEX idx_invitation_phone   ON membership_invitations (candidate_phone);

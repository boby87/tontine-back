-- Table des transferts de presidence (module president.presidencytransfer).
-- Acceptation bilaterale ; au plus UN transfert PENDING par tontine (index unique partiel).
CREATE TABLE presidency_transfers (
    id                       UUID         NOT NULL,
    tontine_id               UUID         NOT NULL,
    initiated_by_user_id     UUID         NOT NULL,
    initiated_by_full_name   VARCHAR(160) NOT NULL,
    target_member_id         UUID         NOT NULL,
    target_user_id           UUID,
    target_member_full_name  VARCHAR(160) NOT NULL,
    reason                   VARCHAR(2000) NOT NULL,
    status                   VARCHAR(20)  NOT NULL,
    initiated_at             TIMESTAMP    NOT NULL,
    expires_at               TIMESTAMP    NOT NULL,
    accepted_at              TIMESTAMP,
    declined_at              TIMESTAMP,
    decline_reason           VARCHAR(1000),
    cancelled_at             TIMESTAMP,
    cancelled_by_user_id     UUID,
    cancel_reason            VARCHAR(1000),
    version                  BIGINT,
    CONSTRAINT pk_presidency_transfers PRIMARY KEY (id)
);

CREATE INDEX idx_pt_tontine ON presidency_transfers (tontine_id);
CREATE INDEX idx_pt_status  ON presidency_transfers (status);
CREATE INDEX idx_pt_target  ON presidency_transfers (target_member_id);

-- Garantit qu'au plus UN transfert est PENDING par tontine a un instant T.
CREATE UNIQUE INDEX uk_pt_one_pending_per_tontine
    ON presidency_transfers (tontine_id)
    WHERE status = 'PENDING';

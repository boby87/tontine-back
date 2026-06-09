-- Table des bulletins de vote (module member.vote).
-- Un bulletin par votant et par motion, garanti par la contrainte UNIQUE (vote_id, voter_key).
-- voter_key :
--   * vote non-anonyme : memberId
--   * vote anonyme     : SHA-256(voteId + memberId + serverSecret) en hex
-- member_id n'est renseigne que pour les votes non-anonymes (NULL sinon).
CREATE TABLE IF NOT EXISTS vote_ballots (
    id         UUID         NOT NULL,
    vote_id    UUID         NOT NULL,
    option_id  UUID         NOT NULL,
    voter_key  VARCHAR(80)  NOT NULL,
    member_id  UUID,
    cast_at    TIMESTAMP    NOT NULL,
    CONSTRAINT pk_vote_ballots PRIMARY KEY (id),
    CONSTRAINT uk_vote_ballot_member UNIQUE (vote_id, voter_key)
);

CREATE INDEX IF NOT EXISTS idx_vote_ballot_vote ON vote_ballots (vote_id);
CREATE INDEX IF NOT EXISTS idx_vote_ballot_option ON vote_ballots (option_id);

package cm.ftg.tontine.president.membership.invitation.enums;

public enum InvitationStatus {
    PENDING,    // creee, pas encore envoyee (entre 2 retries SMS echoues)
    SENT,       // SMS / e-mail envoyes au candidat
    ACCEPTED,   // candidat a accepte -> Member cree
    EXPIRED,    // expiresAt < now()
    CANCELLED   // annulee par le President
}

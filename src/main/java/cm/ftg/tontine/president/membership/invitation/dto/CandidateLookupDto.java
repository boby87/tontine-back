package cm.ftg.tontine.president.membership.invitation.dto;

import cm.ftg.tontine.common.enums.MemberStatus;
import java.util.UUID;

/**
 * Resultat de la recherche d'un utilisateur existant a inviter, par identifiant ou telephone.
 *
 * <p>{@code alreadyMember} indique que l'utilisateur appartient deja a la tontine courante
 * (statut actif, en attente ou suspendu) : le front peut alors avertir le President plutot que
 * de proposer une invitation redondante.
 */
public record CandidateLookupDto(
        UUID userId,
        String firstName,
        String lastName,
        String phone,
        String email,
        boolean alreadyMember,
        MemberStatus memberStatus
) {
}

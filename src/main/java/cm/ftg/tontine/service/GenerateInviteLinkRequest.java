package cm.ftg.tontine.service;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Requête pour générer un lien d'invitation à une tontine.
 */
public record GenerateInviteLinkRequest(

    @NotNull
    Long tontineId,

    /** ID de l'utilisateur qui génère le lien (doit être PRESIDENT) */
    @NotBlank
    String requestedByUserId,

    /** Nombre maximal d'utilisations du lien (null = illimité) */
    Integer maxUses
) {}


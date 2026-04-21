package cm.ftg.tontine.service;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Requête d'ajout d'un membre par référence (téléphone ou email).
 * Le président de la tontine invite un utilisateur existant.
 */
public record AddMemberByReferenceRequest(

    @NotNull
    Long tontineId,

    /** ID du président qui effectue l'ajout */
    @NotBlank
    String requestedByUserId,

    /** Référence de l'utilisateur à ajouter : email ou numéro de téléphone */
    @NotBlank
    String reference
) {}


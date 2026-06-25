package cm.ftg.tontine.treasurer.distribution.controller;

import cm.ftg.tontine.common.dto.ApiResponse;
import cm.ftg.tontine.president.common.TontineIdResolver;
import cm.ftg.tontine.security.AuthenticatedUser;
import cm.ftg.tontine.treasurer.distribution.dto.BeneficiarySelectionDto;
import cm.ftg.tontine.treasurer.distribution.dto.CagnotteDistributionDto;
import cm.ftg.tontine.treasurer.distribution.dto.CreateDistributionRequest;
import cm.ftg.tontine.treasurer.distribution.dto.RunAuctionRequest;
import cm.ftg.tontine.treasurer.distribution.dto.RunLotteryRequest;
import cm.ftg.tontine.treasurer.distribution.service.CagnotteDistributionService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/treasurer/distributions")
public class CagnotteDistributionController {

    private final CagnotteDistributionService service;
    private final TontineIdResolver tontineIdResolver;

    public CagnotteDistributionController(CagnotteDistributionService service, TontineIdResolver tontineIdResolver) {
        this.service = service;
        this.tontineIdResolver = tontineIdResolver;
    }

    @GetMapping
    public ApiResponse<List<CagnotteDistributionDto>> list(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestHeader(value = "X-Tontine-Id", required = false) UUID tontineId) {
        UUID t = tontineIdResolver.resolve(user.id(), tontineId);
        return ApiResponse.ok(service.list(t, user.id()));
    }

    /**
     * AUCTION mode — submit bids; returns the selected beneficiary and triggers their OTP.
     * Must be called before POST /treasurer/distributions.
     */
    @PostMapping("/auction")
    public ApiResponse<BeneficiarySelectionDto> runAuction(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestHeader(value = "X-Tontine-Id", required = false) UUID tontineId,
            @Valid @RequestBody RunAuctionRequest req) {
        UUID t = tontineIdResolver.resolve(user.id(), tontineId);
        return ApiResponse.ok(service.runAuction(t, user.id(), req), "Gagnant enchère sélectionné");
    }

    /**
     * LOTTERY mode — random draw from eligible members; triggers winner's OTP.
     * Must be called before POST /treasurer/distributions.
     */
    @PostMapping("/lottery")
    public ApiResponse<BeneficiarySelectionDto> runLottery(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestHeader(value = "X-Tontine-Id", required = false) UUID tontineId,
            @Valid @RequestBody RunLotteryRequest req) {
        UUID t = tontineIdResolver.resolve(user.id(), tontineId);
        return ApiResponse.ok(service.runLottery(t, user.id(), req), "Bénéficiaire tiré au sort");
    }

    @PostMapping
    public ApiResponse<CagnotteDistributionDto> create(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestHeader(value = "X-Tontine-Id", required = false) UUID tontineId,
            @Valid @RequestBody CreateDistributionRequest req) {
        UUID t = tontineIdResolver.resolve(user.id(), tontineId);
        return ApiResponse.ok(service.create(t, user.id(), req), "Versement cagnotte effectue");
    }
}

package cm.ftg.tontine.treasurer.mobilemoney.controller;

import cm.ftg.tontine.common.dto.ApiResponse;
import cm.ftg.tontine.president.common.TontineIdResolver;
import cm.ftg.tontine.security.AuthenticatedUser;
import cm.ftg.tontine.treasurer.mobilemoney.dto.ApproveMobileMoneyRequest;
import cm.ftg.tontine.treasurer.mobilemoney.dto.MobileMoneyReconciliationDto;
import cm.ftg.tontine.treasurer.mobilemoney.dto.MobileMoneySendRequest;
import cm.ftg.tontine.treasurer.mobilemoney.dto.MobileMoneyTransactionDto;
import cm.ftg.tontine.treasurer.mobilemoney.dto.RejectMobileMoneyRequest;
import cm.ftg.tontine.treasurer.mobilemoney.service.MobileMoneyService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/treasurer/mobile-money")
public class MobileMoneyController {

    private final MobileMoneyService service;
    private final TontineIdResolver tontineIdResolver;

    public MobileMoneyController(MobileMoneyService service, TontineIdResolver tontineIdResolver) {
        this.service = service;
        this.tontineIdResolver = tontineIdResolver;
    }

    @GetMapping
    public ApiResponse<List<MobileMoneyTransactionDto>> list(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestHeader(value = "X-Tontine-Id", required = false) UUID tontineId) {
        UUID t = tontineIdResolver.resolve(user.id(), tontineId);
        return ApiResponse.ok(service.list(t, user.id()));
    }

    @PostMapping("/{id}/approve")
    public ApiResponse<MobileMoneyTransactionDto> approve(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID id,
            @RequestHeader(value = "X-Tontine-Id", required = false) UUID tontineId,
            @RequestBody(required = false) ApproveMobileMoneyRequest req) {
        UUID t = tontineIdResolver.resolve(user.id(), tontineId);
        return ApiResponse.ok(service.approve(id, t, user.id(), req), "Transaction approuvee");
    }

    @PostMapping("/{id}/reject")
    public ApiResponse<MobileMoneyTransactionDto> reject(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID id,
            @RequestHeader(value = "X-Tontine-Id", required = false) UUID tontineId,
            @Valid @RequestBody RejectMobileMoneyRequest req) {
        UUID t = tontineIdResolver.resolve(user.id(), tontineId);
        return ApiResponse.ok(service.reject(id, t, user.id(), req), "Transaction rejetee");
    }

    @PostMapping("/send")
    public ApiResponse<MobileMoneyTransactionDto> send(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestHeader(value = "X-Tontine-Id", required = false) UUID tontineId,
            @Valid @RequestBody MobileMoneySendRequest req) {
        UUID t = tontineIdResolver.resolve(user.id(), tontineId);
        return ApiResponse.ok(service.send(t, user.id(), req), "Envoi Mobile Money enregistre");
    }

    @GetMapping("/reconciliation")
    public ApiResponse<MobileMoneyReconciliationDto> reconciliation(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestHeader(value = "X-Tontine-Id", required = false) UUID tontineId) {
        UUID t = tontineIdResolver.resolve(user.id(), tontineId);
        return ApiResponse.ok(service.reconciliation(t, user.id()));
    }
}

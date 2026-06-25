package cm.ftg.tontine.treasurer.loan.controller;

import cm.ftg.tontine.common.dto.ApiResponse;
import cm.ftg.tontine.president.common.TontineIdResolver;
import cm.ftg.tontine.security.AuthenticatedUser;
import cm.ftg.tontine.treasurer.loan.dto.DisburseLoanRequest;
import cm.ftg.tontine.treasurer.loan.dto.LoanDto;
import cm.ftg.tontine.treasurer.loan.dto.LoanRepaymentScheduleDto;
import cm.ftg.tontine.treasurer.loan.dto.RepayLoanRequest;
import cm.ftg.tontine.treasurer.loan.service.LoanRepaymentScheduleService;
import cm.ftg.tontine.treasurer.loan.service.LoanService;
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
@RequestMapping("/treasurer/loans")
public class LoanController {

    private final LoanService service;
    private final LoanRepaymentScheduleService scheduleService;
    private final TontineIdResolver tontineIdResolver;

    public LoanController(LoanService service,
                          LoanRepaymentScheduleService scheduleService,
                          TontineIdResolver tontineIdResolver) {
        this.service = service;
        this.scheduleService = scheduleService;
        this.tontineIdResolver = tontineIdResolver;
    }

    @GetMapping
    public ApiResponse<List<LoanDto>> list(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestHeader(value = "X-Tontine-Id", required = false) UUID tontineId) {
        UUID t = tontineIdResolver.resolve(user.id(), tontineId);
        return ApiResponse.ok(service.list(t, user.id()));
    }

    @PostMapping("/{id}/disburse")
    public ApiResponse<LoanDto> disburse(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID id,
            @RequestHeader(value = "X-Tontine-Id", required = false) UUID tontineId,
            @RequestBody(required = false) DisburseLoanRequest req) {
        UUID t = tontineIdResolver.resolve(user.id(), tontineId);
        return ApiResponse.ok(service.disburse(id, t, user.id(),
                req == null ? new DisburseLoanRequest(null) : req), "Pret decaisse");
    }

    @GetMapping("/{id}/schedule")
    public ApiResponse<List<LoanRepaymentScheduleDto>> schedule(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID id,
            @RequestHeader(value = "X-Tontine-Id", required = false) UUID tontineId) {
        UUID t = tontineIdResolver.resolve(user.id(), tontineId);
        return ApiResponse.ok(scheduleService.listByLoan(id, t, user.id()));
    }

    @PostMapping("/{id}/repay")
    public ApiResponse<LoanDto> repay(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID id,
            @RequestHeader(value = "X-Tontine-Id", required = false) UUID tontineId,
            @Valid @RequestBody RepayLoanRequest req) {
        UUID t = tontineIdResolver.resolve(user.id(), tontineId);
        return ApiResponse.ok(service.repay(id, t, user.id(), req), "Remboursement enregistre");
    }
}

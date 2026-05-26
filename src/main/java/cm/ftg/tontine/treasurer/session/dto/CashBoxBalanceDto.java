package cm.ftg.tontine.treasurer.session.dto;

import java.math.BigDecimal;

public record CashBoxBalanceDto(String name, BigDecimal balance) {
}

package cm.ftg.tontine.president.presidencytransfer.dto;

import jakarta.validation.constraints.Size;

public record CancelPresidencyTransferRequest(
        @Size(max = 1000) String reason
) {
}

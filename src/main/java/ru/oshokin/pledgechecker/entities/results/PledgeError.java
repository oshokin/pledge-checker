package ru.oshokin.pledgechecker.entities.results;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import ru.oshokin.pledgechecker.utils.ErrorCode;

@JsonPropertyOrder({"requestIndex", "status", "code", "title", "detail"})
@AllArgsConstructor
public class PledgeError implements PledgeSearchResult {

    @Getter
    @Setter
    private long requestIndex;

    @Getter
    @Setter
    private String status;

    @Getter
    @Setter
    private String code;

    @Getter
    @Setter
    private String title;

    @Getter
    @Setter
    private String detail;

    public PledgeError() {
    }

    public PledgeError(long requestIndex, ErrorCode errorCode, String detail) {
        this.requestIndex = requestIndex;
        this.status = errorCode.getStatus();
        this.code = errorCode.getCode();
        this.title = errorCode.getTitle();
        this.detail = detail;
    }

}
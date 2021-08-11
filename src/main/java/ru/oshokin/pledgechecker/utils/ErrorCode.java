package ru.oshokin.pledgechecker.utils;

import lombok.Getter;

public enum ErrorCode {

    CLIENT_ERROR("400", "1001", "Client error"),
    SERVER_ERROR("500", "1002", "Server error");

    @Getter
    private final String status;

    @Getter
    private final String code;

    @Getter
    private final String title;

    ErrorCode(String status, String code, String title) {
        this.status = status;
        this.code = code;
        this.title = title;
    }

}




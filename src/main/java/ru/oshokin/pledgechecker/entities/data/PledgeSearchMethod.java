package ru.oshokin.pledgechecker.entities.data;

import lombok.Getter;

public enum PledgeSearchMethod {
    BY_NOTIFICATION_NUMBER("По регистрационному номеру уведомления"),
    BY_PLEDGOR_INFO("По информации о залогодателе"),
    BY_PLEDGE_SUBJECT_INFO("По информации о предмете залога");

    @Getter
    private final String searchText;

    PledgeSearchMethod(String searchText) {
        this.searchText = searchText;
    }

}



package ru.oshokin.pledgechecker.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class Settings {

    @Value("${server.workers.amount:0}")
    @Getter
    @Setter
    private int workersAmount;

    @Value("${server.headless.mode:false}")
    @Getter
    @Setter
    private boolean isInHeadlessMode;

    @Value("${server.delay.timeout:60}")
    @Getter
    @Setter
    private int defaultTimeOutInSeconds;

    @Value("${max.notifications.per.request:100}")
    @Getter
    @Setter
    private int notificationsPerRequest;

    @Value("${server.history.needed:true}")
    @Getter
    @Setter
    private boolean isHistoryNeeded;

    @Value("${server.last.history.enties.amount:10}")
    @Getter
    @Setter
    private int lastHistoryEntriesAmount;

}

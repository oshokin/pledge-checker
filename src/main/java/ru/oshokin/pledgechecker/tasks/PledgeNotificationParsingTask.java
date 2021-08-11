package ru.oshokin.pledgechecker.tasks;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import ru.oshokin.pledgechecker.entities.results.PledgeNotification;
import ru.oshokin.pledgechecker.services.PledgeParser;
import ru.oshokin.pledgechecker.services.PledgeParserBatch;

import java.util.concurrent.Callable;

@AllArgsConstructor
@Slf4j
public class PledgeNotificationParsingTask implements Callable<PledgeNotification> {

    @Getter
    @Setter
    private PledgeParser parser;

    @Getter
    @Setter
    private PledgeParserBatch parserBatch;

    @Override
    public PledgeNotification call() throws Exception {
        return parser.parsePledgeNotification(parserBatch);
    }

}

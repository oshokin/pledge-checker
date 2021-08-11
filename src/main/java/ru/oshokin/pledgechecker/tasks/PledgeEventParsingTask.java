package ru.oshokin.pledgechecker.tasks;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import ru.oshokin.pledgechecker.entities.events.PledgeEvent;
import ru.oshokin.pledgechecker.services.PledgeParser;
import ru.oshokin.pledgechecker.services.PledgeParserBatch;

import java.util.concurrent.Callable;

@AllArgsConstructor
@Slf4j
public class PledgeEventParsingTask implements Callable<PledgeEvent> {

    @Getter
    @Setter
    private PledgeParser parser;

    @Getter
    @Setter
    private PledgeParserBatch parserBatch;

    @Override
    public PledgeEvent call() throws Exception {
        return parser.parsePledgeEvent(parserBatch);
    }

}

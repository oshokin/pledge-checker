package ru.oshokin.pledgechecker.tasks;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import ru.oshokin.pledgechecker.entities.request.PledgeSearchRequest;
import ru.oshokin.pledgechecker.entities.results.PledgeSearchResult;
import ru.oshokin.pledgechecker.services.PledgeBrowser;

import java.util.List;
import java.util.concurrent.Callable;

@AllArgsConstructor
@Slf4j
public class ProcessRequestsTask implements Callable<List<PledgeSearchResult>> {

    @Getter
    @Setter
    private PledgeBrowser browser;

    @Getter
    @Setter
    private List<PledgeSearchRequest> requests;

    @Override
    public List<PledgeSearchResult> call() {
        return browser.processRequests(requests);
    }

}

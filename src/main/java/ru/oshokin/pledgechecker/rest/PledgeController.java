package ru.oshokin.pledgechecker.rest;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientException;
import ru.oshokin.pledgechecker.entities.request.PledgeSearchServiceRequest;
import ru.oshokin.pledgechecker.entities.results.PledgeSearchServiceResponse;
import ru.oshokin.pledgechecker.services.PledgeService;

@RestController
@Slf4j
public class PledgeController {

    private PledgeService pledgeService;

    @Autowired
    public void setPledgeService(PledgeService pledgeService) {
        this.pledgeService = pledgeService;
    }

    @PostMapping("pledge-checker/search")
    public PledgeSearchServiceResponse pledgeSearch(@RequestBody PledgeSearchServiceRequest request) {
        //TODO
        //@Valid
        //@Validate
        if (request.getRequests() == null || request.getRequests().isEmpty()) {
            throw new RestClientException("Request has no data");
        }

        return pledgeService.processRequest(request);
    }

}

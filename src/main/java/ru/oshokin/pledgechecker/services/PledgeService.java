package ru.oshokin.pledgechecker.services;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.stereotype.Service;
import ru.oshokin.pledgechecker.config.Settings;
import ru.oshokin.pledgechecker.entities.request.PledgeSearchRequest;
import ru.oshokin.pledgechecker.entities.request.PledgeSearchServiceRequest;
import ru.oshokin.pledgechecker.entities.results.PledgeSearchResult;
import ru.oshokin.pledgechecker.entities.results.PledgeSearchServiceResponse;
import ru.oshokin.pledgechecker.tasks.ProcessRequestsTask;
import ru.oshokin.pledgechecker.utils.Chunk;

import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Future;

@Service
@Slf4j
public class PledgeService {

    @Autowired
    @Getter
    @Setter
    private ApplicationContext context;

    @Autowired
    @Getter
    @Setter
    private AsyncTaskExecutor taskPool;

    @Autowired
    @Getter
    @Setter
    private Settings settings;

    @Getter
    @Setter
    private List<PledgeBrowser> browsers;

    public void startBrowsers() {
        log.info("Запускаю браузеры");
        if (settings.getWorkersAmount() == 0) settings.setWorkersAmount(Runtime.getRuntime().availableProcessors() / 2);
        log.info("Максимальное количество браузеров: {}", settings.getWorkersAmount());
        browsers = new ArrayList<>(settings.getWorkersAmount());
        for (int i = 0; i < settings.getWorkersAmount(); i++) {
            try {
                browsers.add(context.getBean(PledgeBrowser.class));
            } catch (Exception e) {
                log.error("Ошибка при запуске браузера: {}", e.getMessage());
            }
        }
        log.info("Запущено браузеров: {}", browsers.size());
        if (browsers.isEmpty()) throw new IllegalStateException("Нет доступных браузеров");
    }

    @PreDestroy
    public void stopBrowsers() {
        log.info("Останавливаю браузеры");
        if (browsers != null) browsers.forEach(PledgeBrowser::stopDriver);
    }

    public PledgeSearchServiceResponse processRequest(PledgeSearchServiceRequest request) {
        request.enumerateRequests();
        PledgeSearchServiceResponse funcResult = new PledgeSearchServiceResponse();
        List<List<PledgeSearchRequest>> chunks;
        List<Future<List<PledgeSearchResult>>> browserFutures = new ArrayList<>();
        if (browsers.size() > 1) {
            int chunkSize = request.getRequests().size() / browsers.size();
            if (chunkSize == 0) chunkSize = 1;
            chunks = Chunk.ofSize(request.getRequests(), chunkSize);
        }
        else chunks = Arrays.asList(request.getRequests());
        for (int i = 0; i < browsers.size(); i++) {
            if (i > chunks.size() - 1) continue;
            List<PledgeSearchRequest> chunk = chunks.get(i);
            if (chunk.size() > 0) browserFutures.add(taskPool.submit(new ProcessRequestsTask(browsers.get(i), chunk)));
        }
        Iterator<Future<List<PledgeSearchResult>>> it = browserFutures.iterator();
        while (it.hasNext()) {
            try {
                funcResult.addSearchResults(it.next().get());
            } catch (Exception e) {
            }
            it.remove();
        }

        return funcResult;
    }

}
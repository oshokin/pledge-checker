package ru.oshokin.pledgechecker.services;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.*;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.stereotype.Component;
import ru.oshokin.pledgechecker.config.Settings;
import ru.oshokin.pledgechecker.entities.data.PledgeSearchMethod;
import ru.oshokin.pledgechecker.entities.events.PledgeEvent;
import ru.oshokin.pledgechecker.entities.request.PledgeSearchRequest;
import ru.oshokin.pledgechecker.entities.results.PledgeError;
import ru.oshokin.pledgechecker.entities.results.PledgeNotification;
import ru.oshokin.pledgechecker.entities.results.PledgeSearchResult;
import ru.oshokin.pledgechecker.tasks.PledgeEventParsingTask;
import ru.oshokin.pledgechecker.tasks.PledgeNotificationParsingTask;
import ru.oshokin.pledgechecker.utils.CommonUtils;
import ru.oshokin.pledgechecker.utils.ErrorCode;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Component
@Scope("prototype")
@Slf4j
public class PledgeBrowser {

    private final Random randGen = new Random();
    private final String url = new String(Base64.getDecoder().decode("aHR0cHM6Ly93d3cucmVlc3RyLXphbG9nb3YucnUvc2VhcmNoL2luZGV4"));
    private final String findInRegistryLinkXPath = "//a[contains(., \"Найти в реестре\")]";
    private final String navigationBarXPath = "//ul[@class=\"nav nav-pills\"]/li/a[text()=\"%s\"]";
    private final String searchButtonXPath = "//button[@id=\"find-btn\"]";
    private final String backButtonXPath = "//button[@id=\"back-btn\"]";
    private final String closeHistoryButtonXPath = "//button[@class=\"swal2-confirm btn btn-default\"]";
    private final PledgeParser pledgeParser;
    private WebDriver driver;
    private Path downloadsDirectory;
    private long requestIndex;
    private Map<String, Future<PledgeNotification>> notificationFutures;
    private Map<String, List<Future<PledgeEvent>>> eventFutures;
    private Map<Future<?>, Path> filesFromFutures;

    @Autowired
    @Getter
    @Setter
    private Settings settings;

    @Autowired
    @Getter
    @Setter
    private AsyncTaskExecutor taskPool;

    public PledgeBrowser(@Autowired Settings settings) throws Exception {
        setSettings(settings);
        pledgeParser = new PledgeParser();
        notificationFutures = new ConcurrentHashMap<>();
        eventFutures = new ConcurrentHashMap<>();
        filesFromFutures = new ConcurrentHashMap<>();
        startDriver();
    }

    public void startDriver() throws IOException, WebDriverException {
        downloadsDirectory = Files.createTempDirectory("ru_oshokin_pledgechecker_");
        String workDir = System.getProperty("user.dir");
        System.setProperty("webdriver.gecko.driver", Paths.get(workDir, "geckodriver.exe").toString());

        FirefoxProfile profile = new FirefoxProfile();
        profile.setPreference("security.fileuri.strict_origin_policy", false);

        profile.setPreference("browser.download.folderList", 2);
        profile.setPreference("browser.download.dir", downloadsDirectory.toString());
        profile.setPreference("browser.helperApps.neverAsk.saveToDisk", "application/pdf");
        profile.setPreference("pdfjs.disabled", true);

        FirefoxOptions options = new FirefoxOptions();
        options.setProfile(profile);
        options.setCapability("marionette", true);
        options.setHeadless(settings.isInHeadlessMode());

        driver = new FirefoxDriver(options);
        driver.get(url);
    }

    public void stopDriver() {
        try {
            if (driver != null) driver.quit();
        } catch (Exception e) {
            log.error("Ошибка при остановке парсера: {}", e.getMessage());
        }
        try {
            if (downloadsDirectory != null && Files.exists(downloadsDirectory)) {
                Files.walk(downloadsDirectory)
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            }
        } catch (Exception e) {
            log.error("Ошибка при остановке парсера: {}", e.getMessage());
        }
    }

    public List<PledgeSearchResult> processRequests(List<PledgeSearchRequest> requests) {
        List<PledgeSearchResult> results = new ArrayList<>(requests.size());
        for (PledgeSearchRequest e : requests) {
            results.addAll(processRequest(e));
        }
        return results;
    }

    private List<PledgeSearchResult> processRequest(PledgeSearchRequest request) {
        requestIndex = request.getId();
        notificationFutures.clear();
        filesFromFutures.clear();

        List<PledgeSearchResult> funcResult = new ArrayList<>();
        PledgeSearchMethod searchMethod = request.getSearchMethod();
        if (searchMethod == null) {
            funcResult.add(getParserError(ErrorCode.CLIENT_ERROR, "Неясно по какому полю производить поиск"));
        }
        funcResult = openSearchTab(searchMethod);
        if (!funcResult.isEmpty()) return funcResult;
        funcResult = fillOutSearchForm(request, searchMethod);
        if (!funcResult.isEmpty()) return funcResult;
        funcResult = waitForSearchResultsToLoad();
        if (!funcResult.isEmpty()) return funcResult;
        funcResult = analyzeSearchResultsForErrors();
        //"//div[@class=\"error-code-panel\"][1]|//div[@class=\"error-text-panel\"][1]/div[1]"
        if (!funcResult.isEmpty()) return funcResult;
        funcResult = parseSearchResults();
        return funcResult;
    }

    private List<PledgeSearchResult> openSearchTab(PledgeSearchMethod searchMethod) {
        List<PledgeSearchResult> funcResult = new ArrayList<>();
        try {
            WebDriverWait wait = new WebDriverWait(driver, settings.getDefaultTimeOutInSeconds());
            if (!driver.getCurrentUrl().equals(url)) driver.get(url);
            simulateHumanClick(By.xpath(findInRegistryLinkXPath));
            CommonUtils.delay(TimeUnit.SECONDS, 1);
            String currentXPath = String.format(navigationBarXPath, searchMethod.getSearchText());
            simulateHumanClick(By.xpath(currentXPath));
            wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath(searchButtonXPath)));
            wait.until(ExpectedConditions.elementToBeClickable(By.xpath(searchButtonXPath)));
        } catch (Exception e) {
            String ed = getErrorDescription(e.getMessage());
            log.error(ed);
            Optional.of(getPageBody()).ifPresent(log::debug);
            funcResult.add(getParserError(ErrorCode.SERVER_ERROR, ed));
        }
        return funcResult;
    }

    private String getPageBody() {
        String funcResult;
        try {
            funcResult = driver.findElement(By.tagName("html")).getAttribute("outerHTML");
        } catch (NoSuchElementException e) {
            funcResult = null;
        }
        return funcResult;
    }

    private List<PledgeSearchResult> fillOutSearchForm(PledgeSearchRequest request, PledgeSearchMethod searchMethod) {
        List<PledgeSearchResult> funcResult = new ArrayList<>();
        if (searchMethod == PledgeSearchMethod.BY_NOTIFICATION_NUMBER) {
            try {
                inputValue("Номер уведомления", request.getNotificationsPackageNumber());
            } catch (Exception e) {
                String ed = getErrorDescription(e.getMessage());
                log.error(ed);
                Optional.of(getPageBody()).ifPresent(log::debug);
                funcResult.add(getParserError(ErrorCode.SERVER_ERROR, ed));
            }
        } else if (searchMethod == PledgeSearchMethod.BY_PLEDGOR_INFO) {
            try {
                if (request.getSurname() != null) inputValueSendKeys("Фамилия", CommonUtils.strLeft(request.getSurname(), 60));
                if (request.getName() != null) inputValueSendKeys("Имя", CommonUtils.strLeft(request.getName(), 60));
                if (request.getPatronymic() != null) inputValueSendKeys("Отчество", CommonUtils.strLeft(request.getPatronymic(), 60));
                if (request.getBirthDate() != null) inputValueSendKeys("Дата рождения", request.getBirthDate().toString());
                if (request.getIdentityDocumentNumber() != null) inputValueSendKeys("Паспорт", request.getIdentityDocumentNumber());
            } catch (Exception e) {
                String ed = getErrorDescription(e.getMessage());
                log.error(ed);
                Optional.of(getPageBody()).ifPresent(log::debug);
                funcResult.add(getParserError(ErrorCode.SERVER_ERROR, ed));
            }
        } else if (searchMethod == PledgeSearchMethod.BY_PLEDGE_SUBJECT_INFO) {
            try {
                if (request.getVIN() != null) inputValueSendKeys("VIN", CommonUtils.strLeft(request.getVIN(), 17));
                if (request.getPIN() != null) inputValueSendKeys("PIN", CommonUtils.strLeft(request.getPIN(), 255));
                if (request.getChassisNumber() != null) inputValueSendKeys("Номер шасси", CommonUtils.strLeft(request.getChassisNumber(), 25));
                if (request.getBodyNumber() != null) inputValueSendKeys("Номер кузова", CommonUtils.strLeft(request.getBodyNumber(), 20));
            } catch (Exception e) {
                String ed = getErrorDescription(e.getMessage());
                log.error(ed);
                Optional.of(getPageBody()).ifPresent(log::debug);
                funcResult.add(getParserError(ErrorCode.SERVER_ERROR, ed));
            }
        }
        return funcResult;
    }

    private void inputValue(String label, Object value) throws WebDriverException {
        WebElement input = driver.findElement(By.xpath(
                String.format("//form[@class=\"wr-form\"]/div[@class=\"form-row\"]/div[@class=\"label-panel\"]/label[contains(text(), \"%s\")]/parent::div/following-sibling::div[@class=\"input-panel\"]/input", label)));
        ((JavascriptExecutor) driver).executeScript(String.format("arguments[0].value='%s';", value), input);
        input.sendKeys(String.format("%n"));
    }

    private void inputValueSendKeys(String label, Object value) throws WebDriverException {
        WebElement input = driver.findElement(By.xpath(
                String.format("//form[@class=\"wr-form\"]/div[@class=\"form-row\"]/div[@class=\"label-panel\"]/label[contains(text(), \"%s\")]/parent::div/following-sibling::div[@class=\"input-panel\"]/input", label)));
        input.sendKeys(String.format("%s%n", value.toString()));
    }

    private List<PledgeSearchResult> waitForSearchResultsToLoad() {
        List<PledgeSearchResult> funcResult = new ArrayList<>();
        try {
            simulateHumanClick(By.xpath(searchButtonXPath));
            WebDriverWait wait = new WebDriverWait(driver, settings.getDefaultTimeOutInSeconds());
            wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath(backButtonXPath)));
            wait.until(ExpectedConditions.elementToBeClickable(By.xpath(backButtonXPath)));
        } catch (Exception e) {
            String ed = getErrorDescription(e.getMessage());
            log.error(ed);
            Optional.of(getPageBody()).ifPresent(log::debug);
            funcResult.add(getParserError(ErrorCode.SERVER_ERROR, ed));
        }
        return funcResult;
    }

    private List<PledgeSearchResult> analyzeSearchResultsForErrors() {
        List<PledgeSearchResult> funcResult = new ArrayList<>();
        WebElement errorLabel;

        try {
            errorLabel = driver.findElement(By.xpath("//div[@class=\"search-error-label\"]"));
        } catch (NoSuchElementException e) {
            errorLabel = null;
        }
        if (errorLabel != null) {
            funcResult.add(getParserError(ErrorCode.SERVER_ERROR, errorLabel.getText()));
        }

        return funcResult;
    }

    private List<PledgeSearchResult> parseSearchResults() {
        List<PledgeSearchResult> funcResult = new ArrayList<>();
        WebElement errorLabel;

        try {
            errorLabel = driver.findElement(By.xpath("//div[@class=\"search-error-label\"]"));
        } catch (NoSuchElementException e) {
            errorLabel = null;
        }
        if (errorLabel != null) {
            funcResult.add(getParserError(ErrorCode.SERVER_ERROR, errorLabel.getText()));
            return funcResult;
        }
        List<WebElement> notificationDataRows;
        try {
            notificationDataRows = driver.findElements(By.xpath("//table[@class=\"table table-extra table-search\"]/tbody/tr"));
        } catch (NoSuchElementException e) {
            notificationDataRows = null;
        }
        if (notificationDataRows == null) {
            Optional.of(getPageBody()).ifPresent(log::debug);
            funcResult.add(getParserError(ErrorCode.SERVER_ERROR, "Нет данных"));
            return funcResult;
        }
        for (WebElement notificationRow : notificationDataRows) {
            try {
                parseNotificationRow(notificationRow);
            } catch (Exception e) {
                String ed = getErrorDescription(e.getMessage());
                log.error(ed);
                Optional.of(getPageBody()).ifPresent(log::debug);
                funcResult.add(getParserError(ErrorCode.SERVER_ERROR, ed));
            }
        }
        waitForTasksToFinish();
        funcResult.addAll(getResultsFromNotificationsQueque());

        return funcResult;
    }

    private void parseNotificationRow(WebElement notificationRow) throws Exception {
        List<Future<PledgeEvent>> historyFutures = new ArrayList<>();

        WebDriverWait wait = new WebDriverWait(driver, settings.getDefaultTimeOutInSeconds());
        String currentStateXPath = ".//span[@class=\"notification\" and text()!=\"История изменений\"][1]";
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath(currentStateXPath)));
        WebElement currentStateColumn = notificationRow.findElement(By.xpath(currentStateXPath));
        String currentNotificationNumber = currentStateColumn.getText().trim();
        String parentWindowId = driver.getWindowHandle();
        simulateHumanClick(By.xpath(currentStateXPath));
        wait.until((ExpectedCondition<Boolean>) driver -> (driver.getWindowHandle().equalsIgnoreCase(parentWindowId)));
        String downloadedFileName = currentNotificationNumber + ".pdf";
        Path downloadedFilePath = waitForFileToDownload(downloadedFileName);
        Path notificationFilePath = downloadedFilePath.getParent().resolve(UUID.randomUUID() + ".pdf");
        Files.move(downloadedFilePath, notificationFilePath);
        waitForFileToChangeState(downloadedFilePath, false);
        waitForFileToChangeState(notificationFilePath, true);

        Future<PledgeNotification> currentStateFuture = taskPool.submit(
                new PledgeNotificationParsingTask(pledgeParser,
                        new PledgeParserBatch(requestIndex, currentNotificationNumber, notificationFilePath)));
        notificationFutures.put(currentNotificationNumber, currentStateFuture);
        filesFromFutures.put(currentStateFuture, notificationFilePath);

        int lastHistoryEntriesAmount = getSettings().getLastHistoryEntriesAmount();

        if (settings.isHistoryNeeded()) {
            simulateHumanClick(By.xpath(".//span[@class=\"notification\" and text()=\"История изменений\"][1]"));
            wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath(closeHistoryButtonXPath)));
            wait.until(ExpectedConditions.elementToBeClickable(By.xpath(closeHistoryButtonXPath)));
            List<WebElement> historyRows = driver.findElements(By.xpath(
                    "//div[@class=\"swal2-content\"]//table/tbody/tr/td/span[@class=\"notification\"]"));
            if (historyRows.size() > 0) {
                int firstIndex = 0;
                int lastIndex = historyRows.size() - 1;
                if (lastHistoryEntriesAmount < 0) {
                    lastIndex = Math.min(-lastHistoryEntriesAmount, historyRows.size()) - 1;
                } else if (lastHistoryEntriesAmount > 0) {
                    firstIndex = Math.max(historyRows.size() - lastHistoryEntriesAmount, 0);
                    lastIndex = historyRows.size() - 1;
                }
                for (int i = firstIndex; i <= lastIndex; i++) {
                    WebElement historyEntryColumn = historyRows.get(i);
                    simulateHumanClick(historyEntryColumn);
                    wait.until((ExpectedCondition<Boolean>) driver -> (driver.getWindowHandle().equalsIgnoreCase(parentWindowId)));
                    downloadedFileName = String.valueOf(i).concat(".pdf");
                    downloadedFilePath = waitForFileToDownload(downloadedFileName);
                    Path historyFilePath = downloadedFilePath.getParent().resolve(UUID.randomUUID().toString().concat(".pdf"));
                    Files.move(downloadedFilePath, historyFilePath);
                    waitForFileToChangeState(downloadedFilePath, false);
                    waitForFileToChangeState(historyFilePath, true);

                    Future<PledgeEvent> historyFuture = taskPool.submit(new PledgeEventParsingTask(pledgeParser,
                            new PledgeParserBatch(requestIndex, currentNotificationNumber, historyFilePath)));
                    historyFutures.add(historyFuture);
                    filesFromFutures.put(historyFuture, historyFilePath);
                }
            }
            WebElement closeHistoryButton = notificationRow.findElement(By.xpath(closeHistoryButtonXPath));
            simulateHumanClick(closeHistoryButton);
        }

        eventFutures.put(currentNotificationNumber, historyFutures);
    }

    private void waitForTasksToFinish() {
        boolean notFinished;
        long millisToSleep = 100;

        Collection<Future<PledgeNotification>> notifications = notificationFutures.values();
        notFinished = notifications.stream().anyMatch(e -> !e.isDone());
        while (notFinished) {
            try {
                Thread.sleep(millisToSleep);
            } catch (InterruptedException e) {
            }
            notFinished = notifications.stream().anyMatch(e -> !e.isDone());
        }
        Collection<Future<PledgeEvent>> events = eventFutures.values().stream().flatMap(List::stream).collect(Collectors.toList());
        notFinished = events.stream().anyMatch(e -> !e.isDone());
        while (notFinished) {
            try {
                Thread.sleep(millisToSleep);
            } catch (InterruptedException e) {
            }
            notFinished = events.stream().anyMatch(e -> !e.isDone());
        }
    }

    private List<PledgeSearchResult> getResultsFromNotificationsQueque() {
        List<PledgeSearchResult> results = new ArrayList<>();

        for (Map.Entry<String, Future<PledgeNotification>> entry : notificationFutures.entrySet()) {
            String notificationNumber = entry.getKey();
            Future<PledgeNotification> pledgeNotificationFuture = entry.getValue();
            if (!pledgeNotificationFuture.isDone()) continue;
            Path fileToDelete = filesFromFutures.get(pledgeNotificationFuture);
            try {
                Files.deleteIfExists(fileToDelete);
            } catch (IOException e) {
            }
            List<Future<PledgeEvent>> historyFutures = eventFutures.get(notificationNumber);
            Optional<Future<PledgeEvent>> optionalActiveHistoryFuture =
                    historyFutures.stream().filter(hf -> !(hf.isDone())).findFirst();
            if (optionalActiveHistoryFuture.isPresent()) continue;
            PledgeNotification pledgeNotification = null;
            boolean isErrorOccured = false;
            try {
                pledgeNotification = pledgeNotificationFuture.get();
            } catch (Exception e) {
                isErrorOccured = true;
                String ed = getErrorDescription(e.getMessage());
                log.error(ed);
                results.add(getParserError(ErrorCode.SERVER_ERROR, ed));
            }
            if (!isErrorOccured) {
                for (Future<PledgeEvent> historyFuture : historyFutures) {
                    try {
                        Files.deleteIfExists(filesFromFutures.get(historyFuture));
                    } catch (IOException e) {
                    }
                    try {
                        pledgeNotification.addEventToHistory(historyFuture.get());
                    } catch (Exception e) {
                        isErrorOccured = true;
                        String ed = getErrorDescription(e.getMessage());
                        log.error(ed);
                        results.add(getParserError(ErrorCode.SERVER_ERROR, ed));
                        break;
                    }
                }
            }
            if (!isErrorOccured && pledgeNotification != null) results.add(pledgeNotification);

            notificationFutures.remove(notificationNumber);
            eventFutures.remove(notificationNumber);
        }

        return results;
    }

    private void simulateHumanClick(WebElement element) {
        int OffsetX = randGen.nextInt(16);
        int OffsetY = randGen.nextInt(16);
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true)", element);
        Actions action = new Actions(driver);
        action.moveToElement(element).perform();
        action.moveByOffset(OffsetX, OffsetY).perform();
        action.moveByOffset(-OffsetX, -OffsetY).perform();
        action.click().perform();
    }

    private void simulateHumanClick(By locator) {
        WebDriverWait wait = new WebDriverWait(driver, settings.getDefaultTimeOutInSeconds());
        wait.until(ExpectedConditions.presenceOfElementLocated(locator));
        wait.until(ExpectedConditions.elementToBeClickable(locator));
        int OffsetX = randGen.nextInt(16);
        int OffsetY = randGen.nextInt(16);
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true)", driver.findElement(locator));
        Actions action = new Actions(driver);
        action.moveToElement(driver.findElement(locator)).perform();
        action.moveByOffset(OffsetX, OffsetY).perform();
        action.moveByOffset(-OffsetX, -OffsetY).perform();
        action.click().perform();
    }

    private PledgeError getParserError(ErrorCode ec, String detail) {
        return new PledgeError(requestIndex, ec, detail);
    }

    private String getErrorDescription(String input) {
        return String.format("Ошибка парсера: %s", input);
    }

    private Path waitForFileToDownload(String fileName) {
        Path downloadedFilePath = downloadsDirectory.resolve(fileName);
        Path firefoxTempFilePath = downloadsDirectory.resolve(fileName + ".part");
        log.info("Expecting file {} to appear", downloadedFilePath);
        waitForFileToChangeState(downloadedFilePath, true);
        log.info("Expecting file {} to disappear", firefoxTempFilePath);
        waitForFileToChangeState(firefoxTempFilePath, false);
        return downloadedFilePath;
    }

    private void waitForFileToChangeState(int waitTimeOutInMillis, int pollTimeOutInMillis, Path path, boolean exists) {
        FluentWait<WebDriver> wait =
                new FluentWait<>(driver).
                        withTimeout(Duration.ofMillis(waitTimeOutInMillis)).
                        pollingEvery(Duration.ofMillis(pollTimeOutInMillis));

        wait.until(new ExpectedCondition<Boolean>() {
            long previousSize = 0;
            public Boolean apply(WebDriver driver) {
                boolean funcResult;
                try {
                    boolean fileExists = Files.exists(path);
                    if (exists) {
                        long currentSize = Files.size(path);
                        funcResult = (fileExists && currentSize > 0 && currentSize == previousSize);
                        previousSize = currentSize;
                    } else funcResult = (!fileExists);
                } catch (IOException e) {
                    funcResult = false;
                }
                return funcResult;
            }
        });
    }

    private void waitForFileToChangeState(Path path, boolean exists) {
        waitForFileToChangeState(settings.getDefaultTimeOutInSeconds() * 1000, 250, path, exists);
    }

}
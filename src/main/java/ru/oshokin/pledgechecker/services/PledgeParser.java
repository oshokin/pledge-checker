package ru.oshokin.pledgechecker.services;

import io.github.jonathanlink.PDFLayoutTextStripper;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.cos.COSDocument;
import org.apache.pdfbox.io.RandomAccessFile;
import org.apache.pdfbox.pdfparser.PDFParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import ru.oshokin.pledgechecker.entities.assets.Asset;
import ru.oshokin.pledgechecker.entities.assets.Property;
import ru.oshokin.pledgechecker.entities.assets.Vehicle;
import ru.oshokin.pledgechecker.entities.data.PledgeContract;
import ru.oshokin.pledgechecker.entities.events.PledgeEvent;
import ru.oshokin.pledgechecker.entities.events.PledgeModification;
import ru.oshokin.pledgechecker.entities.events.PledgeRegistration;
import ru.oshokin.pledgechecker.entities.events.PledgeRemoval;
import ru.oshokin.pledgechecker.entities.legal.ForeignLegalEntity;
import ru.oshokin.pledgechecker.entities.legal.LegalEntity;
import ru.oshokin.pledgechecker.entities.legal.PhysicalLegalEntity;
import ru.oshokin.pledgechecker.entities.legal.RussianLegalEntity;
import ru.oshokin.pledgechecker.entities.results.PledgeNotification;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Scope("prototype")
@Slf4j
public class PledgeParser {

    private final DateTimeFormatter defaultDatePattern = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private final DateTimeFormatter defaultDateTimePattern = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");

    public PledgeParser() {
    }

    public PledgeNotification parsePledgeNotification(PledgeParserBatch parserBatch) throws Exception {
        String input = getTextFromPDF(parserBatch.getFilePath());

        StringBuilder text = new StringBuilder(removeDoubleSpaces(input));
        Map<String, String> notificationHeader = parseNotificationHeader(text);

        String notificationTypeText = notificationHeader.getOrDefault("type", "").toLowerCase();
        boolean isNotificationTypeSet =
                ((notificationTypeText.contains("возникновении"))
                        || (notificationTypeText.contains("изменении"))
                        || (notificationTypeText.contains("исключении")));
        if (!isNotificationTypeSet) throw new RuntimeException("Не удалось определить тип уведомления");

        PledgeNotification funcResult = new PledgeNotification();
        funcResult.setRequestIndex(parserBatch.getRequestIndex());

        funcResult.setNumber(notificationHeader.getOrDefault("number", ""));
        funcResult.setDateTime(LocalDateTime.parse(
                notificationHeader.getOrDefault("dateTime", ""), defaultDateTimePattern));
        funcResult.setActive((notificationHeader.getOrDefault("state", "").
                toLowerCase().contains("актуальное")));

        StringBuilder notificationPropertiesText = getNotificationPropertiesText(text);
        List<Property> assets = new ArrayList<>();

        assets.addAll(parseNotificationVehicles(notificationPropertiesText));
        assets.addAll(parseNotificationOtherAssets(notificationPropertiesText));

        funcResult.setAssets(assets);

        StringBuilder notificationPledgorsText = getNotificationPledgorsText(text);
        List<LegalEntity> pledgors = new ArrayList<>();

        pledgors.addAll(parseNotificationPhysicalEntities(notificationPledgorsText));
        pledgors.addAll(parseNotificationRussianLegalEntities(notificationPledgorsText));
        pledgors.addAll(parseNotificationForeignLegalEntities(notificationPledgorsText));

        funcResult.setPledgors(pledgors);

        StringBuilder notificationPledgeesText = getNotificationPledgeesText(text);
        List<LegalEntity> pledgees = new ArrayList<>();

        pledgees.addAll(parseNotificationPhysicalEntities(notificationPledgeesText));
        pledgees.addAll(parseNotificationRussianLegalEntities(notificationPledgeesText));
        pledgees.addAll(parseNotificationForeignLegalEntities(notificationPledgeesText));

        funcResult.setPledgees(pledgees);

        StringBuilder notificationContractText = getNotificationContractText(text);
        funcResult.setContract(parseNotificationContract(notificationContractText));

        return funcResult;
    }

    public PledgeEvent parsePledgeEvent(PledgeParserBatch parserBatch) throws Exception {
        String input = getTextFromPDF(parserBatch.getFilePath());

        StringBuilder text = new StringBuilder(removeDoubleSpaces(input));
        Map<String, String> notificationHeader = parseNotificationHeader(text);

        String notificationTypeText = notificationHeader.getOrDefault("type", "").toLowerCase();
        PledgeEvent funcResult = null;

        if (notificationTypeText.contains("возникновении")) funcResult = new PledgeRegistration();
        else if (notificationTypeText.contains("изменении")) funcResult = new PledgeModification();
        else if (notificationTypeText.contains("исключении")) funcResult = new PledgeRemoval();

        if (funcResult == null) throw new RuntimeException("Не удалось определить тип уведомления");

        funcResult.setNumber(notificationHeader.getOrDefault("number", ""));
        funcResult.setDateTime(LocalDateTime.parse(
                notificationHeader.getOrDefault("dateTime", ""), defaultDateTimePattern));

        if (!(funcResult instanceof PledgeRemoval)) {
            StringBuilder notificationPropertiesText = getNotificationPropertiesText(text);
            List<Property> assets = new ArrayList<>();

            assets.addAll(parseNotificationVehicles(notificationPropertiesText));
            assets.addAll(parseNotificationOtherAssets(notificationPropertiesText));

            if (funcResult instanceof PledgeRegistration) ((PledgeRegistration) funcResult).setAssets(assets);
            else if (funcResult instanceof PledgeModification) ((PledgeModification) funcResult).setAssets(assets);

            StringBuilder notificationPledgorsText = getNotificationPledgorsText(text);
            List<LegalEntity> pledgors = new ArrayList<>();

            pledgors.addAll(parseNotificationPhysicalEntities(notificationPledgorsText));
            pledgors.addAll(parseNotificationRussianLegalEntities(notificationPledgorsText));
            pledgors.addAll(parseNotificationForeignLegalEntities(notificationPledgorsText));

            if (funcResult instanceof PledgeRegistration) ((PledgeRegistration) funcResult).setPledgors(pledgors);
            else if (funcResult instanceof PledgeModification) ((PledgeModification) funcResult).setPledgors(pledgors);

            StringBuilder notificationPledgeesText = getNotificationPledgeesText(text);
            List<LegalEntity> pledgees = new ArrayList<>();

            pledgees.addAll(parseNotificationPhysicalEntities(notificationPledgeesText));
            pledgees.addAll(parseNotificationRussianLegalEntities(notificationPledgeesText));
            pledgees.addAll(parseNotificationForeignLegalEntities(notificationPledgeesText));

            if (funcResult instanceof PledgeRegistration) ((PledgeRegistration) funcResult).setPledgees(pledgees);
            else if (funcResult instanceof PledgeModification) ((PledgeModification) funcResult).setPledgees(pledgees);

            StringBuilder notificationContractText = getNotificationContractText(text);

            if (funcResult instanceof PledgeRegistration) ((PledgeRegistration) funcResult).setContract(parseNotificationContract(notificationContractText));
            else if (funcResult instanceof PledgeModification) ((PledgeModification) funcResult).setContract(parseNotificationContract(notificationContractText));
        }

        return funcResult;
    }

    private String getTextFromPDF(Path filePath) throws IOException {
        RandomAccessFile file = new RandomAccessFile(filePath.toFile(), "r");
        PDFParser pdfParser = new PDFParser(file);
        pdfParser.parse();
        COSDocument externalDocument = pdfParser.getDocument();
        PDDocument innerDocument = new PDDocument(externalDocument);
        PDFTextStripper pdfTextStripper = new PDFLayoutTextStripper();
        String funcResult = pdfTextStripper.getText(innerDocument);
        innerDocument.close();
        if (externalDocument != null && !externalDocument.isClosed()) externalDocument.close();
        if (!file.isClosed()) file.close();

        return funcResult;
    }

    private String replaceAllInSingleLineMode(String regex, String input, String replacement) {
        return Pattern.compile(regex, Pattern.DOTALL).matcher(input).replaceAll(replacement);
    }

    private String removeDoubleSpaces(String string) {
        String funcResult = string;
        while (funcResult.contains("  ")) {
            funcResult = funcResult.replace("  ", " ");
        }
        return funcResult;
    }

    private StringBuilder getNotificationPropertiesText(StringBuilder text) {
        Matcher matcher = Pattern.compile(
                "\\s*1\\.\\s*Движимое\\s*имущество,\\s*переданное\\s*в\\s*залог.*?(?="+
                        "2\\.\\s*Сведения\\s*о\\s*залогодателе\\(ях\\)|" +
                        "3\\.\\s*Сведения\\s*о\\s*залогодержателе\\(ях\\)|" +
                        "4\\.\\s*Сведения\\s*о\\s*договоре\\s*залога,\\s*иной\\s*сделке,\\s*" +
                        "на\\s*основании\\s*которой\\s*возникает\\s*залог\\s*в\\s*силу\\s*закона|\\z)", Pattern.DOTALL).matcher(text);

        StringBuffer newText = new StringBuffer(text.capacity());
        StringBuilder propertiesText = new StringBuilder(text.capacity());

        while (matcher.find()) {
            propertiesText.append(String.format("%s%n", matcher.group(0)));
            matcher.appendReplacement(newText, "");
        }

        matcher.appendTail(newText);

        text.setLength(0);
        text.append(newText);
        text.trimToSize();

        newText.setLength(0);

        return new StringBuilder(replaceAllInSingleLineMode(
                "(1\\.\\s*Движимое\\s*имущество,\\s*переданное\\s*в\\s*залог)|" +
                        "(1\\.\\d+\\s*Транспортное\\s*средство)|"+
                        "(1.\\d+\\s*Иное\\s*имущество)", propertiesText.toString(), ""));
    }

    private StringBuilder getNotificationPledgorsText(StringBuilder text) {
        Matcher matcher = Pattern.compile(
                "\\s*2\\.\\s*Сведения\\s*о\\s*залогодателе\\(ях\\).*?(?=" +
                        "3\\.\\s*Сведения\\s*о\\s*залогодержателе\\(ях\\)|" +
                        "4\\.\\s*Сведения\\s*о\\s*договоре\\s*залога,\\s*иной\\s*сделке,\\s*" +
                        "на\\s*основании\\s*которой\\s*возникает\\s*залог\\s*в\\s*силу\\s*закона|\\z)",
                Pattern.DOTALL).matcher(text);

        StringBuffer newText = new StringBuffer(text.capacity());
        StringBuilder notificationPledgorsText = new StringBuilder(text.capacity());

        while (matcher.find()) {
            notificationPledgorsText.append(String.format("%s%n", matcher.group(0)));
            matcher.appendReplacement(newText, "");
        }

        matcher.appendTail(newText);

        text.setLength(0);
        text.append(newText);
        text.trimToSize();

        newText.setLength(0);

        return new StringBuilder(replaceAllInSingleLineMode(
                "(2\\.\\s*Сведения\\s*о\\s*залогодателе\\(ях\\))|" +
                        "\\s[23]\\.\\d+\\s*(((Российское|Иностранное)\\s*юридическое)|Физическое)\\s*лицо",
                notificationPledgorsText.toString(), ""));
    }

    private StringBuilder getNotificationPledgeesText(StringBuilder text) {
        Matcher matcher = Pattern.compile(
                "\\s*3\\.\\s*Сведения\\s*о\\s*залогодержателе\\(ях\\).*?(?=" +
                        "4\\.\\s*Сведения\\s*о\\s*договоре\\s*залога,\\s*иной\\s*сделке,\\s*" +
                        "на\\s*основании\\s*которой\\s*возникает\\s*залог\\s*в\\s*силу\\s*закона|\\z)",
                Pattern.DOTALL).matcher(text);

        StringBuffer newText = new StringBuffer(text.capacity());
        StringBuilder notificationPledgeesText = new StringBuilder(text.capacity());

        while (matcher.find()) {
            notificationPledgeesText.append(String.format("%s%n", matcher.group(0)));
            matcher.appendReplacement(newText, "");
        }

        matcher.appendTail(newText);

        text.setLength(0);
        text.append(newText);
        text.trimToSize();

        newText.setLength(0);

        return new StringBuilder(replaceAllInSingleLineMode(
                "(3\\.\\s*Сведения\\s*о\\s*залогодержателе\\(ях\\))|" +
                        "\\s[23]\\.\\d+\\s*(((Российское|Иностранное)\\s*юридическое)|Физическое)\\s*лицо",
                notificationPledgeesText.toString(), ""));
    }

    private StringBuilder getNotificationContractText(StringBuilder text) {
        Matcher matcher = Pattern.compile(
                "\\s*4\\.\\s*Сведения\\s*о\\s*договоре\\s*залога,\\s*иной\\s*сделке,\\s*" +
                        "на\\s*основании\\s*которой\\s*возникает\\s*залог\\s*в\\s*силу\\s*закона.*",
                Pattern.DOTALL).matcher(text);

        StringBuffer newText = new StringBuffer(text.capacity());
        StringBuilder notificationPledgeesText = new StringBuilder(text.capacity());

        while (matcher.find()) {
            notificationPledgeesText.append(String.format("%s%n", matcher.group(0)));
            matcher.appendReplacement(newText, "");
        }

        matcher.appendTail(newText);

        text.setLength(0);
        text.append(newText);
        text.trimToSize();

        newText.setLength(0);

        return new StringBuilder(replaceAllInSingleLineMode(
                "\\s*4\\.\\s*Сведения\\s*о\\s*договоре\\s*залога,\\s*иной\\s*сделке,\\s*на\\s*основании" +
                        "\\s*которой\\s*возникает\\s*залог\\s*в\\s*силу\\s*закона\\s*",
                notificationPledgeesText.toString(), ""));
    }

    private Map<String, String> parseNotificationHeader(StringBuilder input) {
        Map<String, String> funcResult = new HashMap<>(4);

        funcResult.put("type", "");
        funcResult.put("number", "");
        funcResult.put("dateTime", "");
        funcResult.put("state", "");

        if (stringContainsInOrder(input.toString(), "Уведомление", "номер", "от")) {
            Matcher matcher = Pattern.compile(
                    "\\s*Уведомление\\s*(о\\s*возникновении|об\\s*изменении|об\\s*исключении)" +
                            "(\\s*залога\\s*движимого\\s*имущества)*\\s*" +
                            "номер\\s*(\\d{4}-\\d{3}-\\d{6}-\\d{3}(\\/\\d*)?)\\s*" +
                            "от\\s*(\\d{2}\\.\\d{2}\\.\\d{4}\\s*\\d{2}:\\d{2}:\\d{2})(\\s*" +
                            "\\(время\\s*московское\\))*", Pattern.DOTALL).matcher(input);

            StringBuffer newText = new StringBuffer(input.capacity());

            while (matcher.find()) {
                funcResult.put("type", matcher.group(1));
                funcResult.put("number", matcher.group(3));
                funcResult.put("dateTime", matcher.group(5));
                matcher.appendReplacement(newText, "");
            }

            matcher.appendTail(newText);

            input.setLength(0);
            input.append(newText);
            input.trimToSize();

            newText.setLength(0);
            newText.setLength(input.capacity());

            if (input.indexOf("Состояние:") > -1) {
                matcher = Pattern.compile(
                        "\\s*Состояние:\\s*(Сведения\\s*исключены|Актуальное)\\s*", Pattern.DOTALL).matcher(input);

                while (matcher.find()) {
                    funcResult.put("state", matcher.group(1));
                    matcher.appendReplacement(newText, "");
                }

                matcher.appendTail(newText);

                input.setLength(0);
                input.append(newText);
                input.trimToSize();

                newText.setLength(0);
            }
        }

        return funcResult;
    }

    private List<Vehicle> parseNotificationVehicles(StringBuilder text) {
        List<Vehicle> notificationVehicles = new ArrayList<>();

        if (stringContainsInOrder(text.toString(), "Описание", "транспортного", "средства")) {
            Matcher matcher = Pattern.compile(
                    "\\s*(\\d+)\\s*VIN\\s*(.*?(?=\\s*PIN))\\s*" +
                            "PIN\\s*(.*?(?=\\s*Описание))\\s*" +
                            "Описание\\s*([^\\n]*)\\s*" +
                            "транспортного\\s*([^\\n]*)\\s*" +
                            "средства\\s*(.*?(?=\\s*" +
                            "Номер\\s*шасси\\s*\\(рамы\\)))\\s*" +
                            "Номер\\s*шасси\\s*\\(рамы\\)\\s*(.*?(?=\\s*Номер\\s*кузова))\\s*" +
                            "Номер\\s*кузова([^\\n]*|\\s*)", Pattern.DOTALL).matcher(text);

            StringBuffer newText = new StringBuffer(text.length());

            while (matcher.find()) {
                Vehicle vehicle = new Vehicle();

                vehicle.setVIN(matcher.group(2));
                vehicle.setPIN(matcher.group(3));
                vehicle.setDescription(String.format("%s %s %s",
                        matcher.group(4).trim(),
                        matcher.group(5).trim(),
                        matcher.group(6).trim()).trim());
                vehicle.setChassisNumber(matcher.group(7));
                vehicle.setBodyNumber(matcher.group(8).trim());

                notificationVehicles.add(vehicle);
                matcher.appendReplacement(newText, "");
            }

            matcher.appendTail(newText);

            text.setLength(0);
            text.append(newText);
            text.trimToSize();

            newText.setLength(0);
        }

        return notificationVehicles;
    }

    private List<Asset> parseNotificationOtherAssets(StringBuilder text) {
        List<Asset> notificationOtherAssets = new ArrayList<>();
        if (stringContainsInOrder(text.toString(), "Описание", "транспортного", "средства")) {
            Matcher matcher = Pattern.compile(
                    "\\s+(\\d+)\\s+ID.*?(?=\\s*Описание\\s*иного)\\s*" +
                            "Описание\\s*иного\\s*([^\\n]*)\\n\\s*имущества(.*?(?=\\s+\\d+\\s+ID|\\z))",
                    Pattern.DOTALL).matcher(text);

            while (matcher.find()) {
                Asset asset = new Asset();
                asset.setDescription(String.format("%s %s", matcher.group(2).trim(), matcher.group(3).trim()).trim());

                notificationOtherAssets.add(asset);
            }

            text.setLength(0);
        }

        return notificationOtherAssets;
    }

    private List<PhysicalLegalEntity> parseNotificationPhysicalEntities(StringBuilder text) {
        List<PhysicalLegalEntity> notificationPhysicalEntities = new ArrayList<>();

        if (stringContainsInOrder(text.toString(), "Фамилия", "Адрес", "электронной", "почты")) {
            Matcher matcher = Pattern.compile(
                    "\\s+(\\d+)\\s+Фамилия\\s*([^\\n]*)\\s*" +
                            "Имя\\s*([^\\n]*)\\s*" +
                            "Отчество\\s*([^\\n]*)\\s*" +
                            "Фамилия\\s*([^\\n]*)\\s*\\(латинскими\\s*буквами\\)\\s*(.*?(?=\\s*Имя))\\s*" +
                            "Имя\\s*([^\\n]*)\\s*\\(латинскими\\s*буквами\\)\\s*(.*?(?=\\s*Отчество))\\s*" +
                            "Отчество\\s*([^\\n]*)\\s*\\(латинскими\\s*буквами\\)\\s*(.*?(?=\\s*Дата\\s*рождения))\\s*" +
                            "Дата\\s*рождения\\s*(\\d{2}\\.\\d{2}\\.\\d{4})*\\s*" +
                            "Документ,\\s*([^\\n]*)\\s*удостоверяющий\\s*([^\\n]*)\\s*личность\\s*(.*?(?=\\s*" +
                            "Адрес\\s*фактического))\\s*Адрес\\s*фактического\\s*([^\\n]*)\\s*" +
                            "места\\s*жительства\\s*в\\s*([^\\n]*)\\s*Российской\\s*([^\\n]*)\\s*" +
                            "Федерации\\s*(.*?(?=\\s*Адрес\\s*электронной))\\s*" +
                            "Адрес\\s*электронной\\s*([^\\n]*)\\s*почты\\s*(.*?(?=(\\s+(\\d+)\\s+(Фамилия|Полное))|\\z))",
                    Pattern.DOTALL).matcher(text);

            StringBuffer newText = new StringBuffer(text.length());

            while (matcher.find()) {
                notificationPhysicalEntities.add(getNotificationPhysicalEntity(matcher, true));
                matcher.appendReplacement(newText, "");
            }

            matcher.appendTail(newText);

            text.setLength(0);
            text.append(newText);
            text.trimToSize();

            newText.setLength(0);
        }

        if (stringContainsInOrder(text.toString(), "Фамилия")) {
            Matcher matcher = Pattern.compile(
                    "\\s+(\\d+)\\s+Фамилия\\s*([^\\n]*)\\s*" +
                            "Имя\\s*([^\\n]*)\\s*" +
                            "Отчество\\s*([^\\n]*)\\s*" +
                            "Фамилия\\s*([^\\n]*)\\s*\\(латинскими\\s*буквами\\)\\s*(.*?(?=\\s*Имя))\\s*" +
                            "Имя\\s*([^\\n]*)\\s*\\(латинскими\\s*буквами\\)\\s*(.*?(?=\\s*Отчество))\\s*" +
                            "Отчество\\s*([^\\n]*)\\s*\\(латинскими\\s*буквами\\)\\s*(.*?(?=\\s*Дата\\s*рождения))\\s*" +
                            "Дата\\s*рождения\\s*(\\d{2}\\.\\d{2}\\.\\d{4})*\\s*" +
                            "Документ,\\s*([^\\n]*)\\s*удостоверяющий\\s*([^\\n]*)\\s*личность\\s*(.*?(?=\\s*" +
                            "Адрес\\s*фактического))\\s*Адрес\\s*фактического\\s*([^\\n]*)\\s*" +
                            "места\\s*жительства\\s*в\\s*([^\\n]*)\\s*Российской\\s*([^\\n]*)\\s*" +
                            "Федерации\\s*(.*?(?=(\\s+(\\d+)\\s+(Фамилия|Полное))|\\z))",
                    Pattern.DOTALL).matcher(text);

            StringBuffer newText = new StringBuffer(text.length());

            while (matcher.find()) {
                notificationPhysicalEntities.add(getNotificationPhysicalEntity(matcher, false));
                matcher.appendReplacement(newText, "");
            }

            matcher.appendTail(newText);

            text.setLength(0);
            text.append(newText);
            text.trimToSize();

            newText.setLength(0);
        }

        return notificationPhysicalEntities;
    }

    private List<RussianLegalEntity> parseNotificationRussianLegalEntities(StringBuilder text) {
        List<RussianLegalEntity> notificationRussianLegalEntities = new ArrayList<>();

        if (stringContainsInOrder(text.toString(),
                "Полное", "наименование", "ИНН", "ОГРН", "Место", "нахождения")) {
            Matcher matcher = Pattern.compile(
                    "\\s+(\\d+)\\s+Полное\\s*([^\\n]*)\\s*наименование\\s*(.*?(?=\\s*ИНН))\\s*" +
                            "ИНН\\s*(.*?(?=\\s*ОГРН))\\s*" +
                            "ОГРН\\s*(.*?(?=\\s*Место))\\s*" +
                            "Место\\s*нахождения\\s*?(.*?(?=\\s+\\d+\\s+Полное|\\z))",
                    Pattern.DOTALL).matcher(text);

            StringBuffer newText = new StringBuffer(text.length());

            while (matcher.find()) {
                RussianLegalEntity legalEntity = new RussianLegalEntity();

                legalEntity.setFullName(String.format("%s %s", matcher.group(2).trim(), matcher.group(3).trim()).trim());
                legalEntity.setINN(matcher.group(4).trim());
                legalEntity.setOGRN(matcher.group(5).trim());
                legalEntity.setLocation(matcher.group(6).trim());

                notificationRussianLegalEntities.add(legalEntity);
                matcher.appendReplacement(newText, "");
            }

            matcher.appendTail(newText);

            text.setLength(0);
            text.append(newText);
            text.trimToSize();

            newText.setLength(0);
        }

        return notificationRussianLegalEntities;
    }

    private List<ForeignLegalEntity> parseNotificationForeignLegalEntities(StringBuilder text) {
        List<ForeignLegalEntity> notificationRussianLegalEntities = new ArrayList<>();

        if (stringContainsInOrder(text.toString(),
                "Полное", "наименование", "ИНН", "Регистрационный", "номер", "Место", "нахождения")) {
            Matcher matcher = Pattern.compile(
                    "\\s+(\\d+)\\s+Полное\\s*([^\\n]*)\\s*наименование\\s*(.*?(?=\\s*ИНН))\\s*" +
                            "ИНН\\s*(.*?(?=\\s*Регистрационный))\\s*" +
                            "Регистрационный\\s*([^\\n]*)\\s*номер\\s*(.*?(?=\\s*Место))\\s*" +
                            "Место\\s*нахождения\\s*?(.*?(?=\\s+\\d+\\s+Полное|\\z))",
                    Pattern.DOTALL).matcher(text);

            StringBuffer newText = new StringBuffer(text.length());

            while (matcher.find()) {
                ForeignLegalEntity legalEntity = new ForeignLegalEntity();

                legalEntity.setFullName(String.format("%s %s",
                        matcher.group(2).trim(), matcher.group(3).trim()).trim());
                legalEntity.setINN(matcher.group(4).trim());
                legalEntity.setRegistrationNumber(String.format("%s %s",
                        matcher.group(5).trim(), matcher.group(6).trim()).trim());
                legalEntity.setLocation(matcher.group(7).trim());

                notificationRussianLegalEntities.add(legalEntity);
                matcher.appendReplacement(newText, "");
            }

            matcher.appendTail(newText);

            text.setLength(0);
            text.append(newText);
            text.trimToSize();

            newText.setLength(0);
        }

        return notificationRussianLegalEntities;
    }

    private PhysicalLegalEntity getNotificationPhysicalEntity(Matcher matcher, boolean isEmailPresent) {
        PhysicalLegalEntity funcResult = new PhysicalLegalEntity();

        funcResult.setSurname(matcher.group(2).trim());
        funcResult.setName(matcher.group(3).trim());
        funcResult.setPatronymic(matcher.group(4).trim());
        funcResult.setSurnameInLatinLetters(String.format("%s %s",
                matcher.group(5).trim(), matcher.group(6).trim()).trim());
        funcResult.setNameInLatinLetters(String.format("%s %s",
                matcher.group(7).trim(), matcher.group(8).trim()).trim());
        funcResult.setPatronymicInLatinLetters(String.format("%s %s",
                matcher.group(9).trim(), matcher.group(10).trim()).trim());
        funcResult.setBirthDate(LocalDate.parse(matcher.group(11), defaultDatePattern));
        funcResult.setIdentityDocument(String.format("%s %s %s",
                matcher.group(12).trim(), matcher.group(13).trim(), matcher.group(14).trim()).trim());
        funcResult.setActualResidenceAddressInRF(String.format("%s %s %s %s",
                matcher.group(15).trim(), matcher.group(16).trim(), matcher.group(17).trim(),
                matcher.group(18).trim()).trim());
        funcResult.setEmail("");
        if (isEmailPresent) {
            funcResult.setEmail(String.format("%s %s",
                    matcher.group(19).trim(), matcher.group(20).trim()).trim());
        }

        return funcResult;
    }

    private PledgeContract parseNotificationContract(StringBuilder text) {
        PledgeContract funcResult = new PledgeContract();

        if (stringContainsInOrder(text.toString(),
                "Наименование", "Дата", "договора", "Номер", "договора", "Срок", "исполнения")) {

            Matcher matcher = Pattern.compile(
                    "\\s*Наименование\\s*(.*?(?=\\s*Дата\\s*договора))\\s*" +
                            "Дата\\s*договора\\s*(.*?(?=\\s*Номер\\s*договора))\\s*" +
                            "Номер\\s*договора\\s*(.*?(?=\\s*Срок))\\s*" +
                            "Срок\\s*исполнения\\s*([^\\n]*)\\s*обязательства,\\s*([^\\n]*)\\s*\\s*" +
                            "обеспеченного\\s*залогом\\s*([^\\n]*)\\s*" +
                            "движимого\\s*имущества\\s*(.*\\z)", Pattern.DOTALL).matcher(text);

            StringBuffer newText = new StringBuffer(text.capacity());

            while (matcher.find()) {
                funcResult.setName(matcher.group(1));
                funcResult.setDate(LocalDate.parse(matcher.group(2), defaultDatePattern));
                funcResult.setNumber(matcher.group(3));
                funcResult.setTerm(String.format("%s %s %s %s",
                        matcher.group(4).trim(), matcher.group(5).trim(), matcher.group(6).trim(),
                        matcher.group(7).trim()).trim());

                matcher.appendReplacement(newText, "");
            }

            matcher.appendTail(newText);

            text.setLength(0);
            text.append(newText);
            text.trimToSize();

            newText.setLength(0);
        }

        return funcResult;
    }

    private boolean stringContainsInOrder(String string, Iterable<String> substrings) {
        int fromIndex = 0;

        for (String substring : substrings) {
            fromIndex = string.indexOf(substring, fromIndex);
            if (fromIndex == -1) return false;
            fromIndex++;
        }
        return true;
    }

    private boolean stringContainsInOrder(String string, String... substrings) {
        return stringContainsInOrder(string, Arrays.asList(substrings));
    }

}

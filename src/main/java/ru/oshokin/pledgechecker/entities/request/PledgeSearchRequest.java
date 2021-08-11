package ru.oshokin.pledgechecker.entities.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import ru.oshokin.pledgechecker.entities.data.PledgeSearchMethod;
import ru.oshokin.pledgechecker.utils.CommonUtils;

import java.time.LocalDate;

@AllArgsConstructor
public class PledgeSearchRequest {

    @Getter
    @Setter
    private long id;

    @JsonProperty("number")
    @Getter
    @Setter
    private String notificationsPackageNumber;

    @Getter
    @Setter
    private String surname;

    @Getter
    @Setter
    private String name;

    @Getter
    @Setter
    private String patronymic;

    @Getter
    @Setter
    private String regionCode;

    @Getter
    @Setter
    private LocalDate birthDate;

    @JsonProperty("docNumber")
    @Getter
    @Setter
    private String identityDocumentNumber;

    @JsonProperty("vin")
    @Getter
    @Setter
    private String VIN;

    @JsonProperty("pin")
    @Getter
    @Setter
    private String PIN;

    @Getter
    @Setter
    private String chassisNumber;

    @Getter
    @Setter
    private String bodyNumber;

    public PledgeSearchRequest() {
    }

    public PledgeSearchMethod getSearchMethod() {
        PledgeSearchMethod funcResult = null;
        String number = getNotificationsPackageNumber();
        if (!CommonUtils.isNullOrEmpty(number))
            funcResult = PledgeSearchMethod.BY_NOTIFICATION_NUMBER;
        else {
            if (!CommonUtils.isNullOrEmpty(getVIN())
                    || !CommonUtils.isNullOrEmpty(getPIN())
                    || !CommonUtils.isNullOrEmpty(getChassisNumber())
                    || !CommonUtils.isNullOrEmpty(getBodyNumber()))
                funcResult = PledgeSearchMethod.BY_PLEDGE_SUBJECT_INFO;
            else {
                if (!CommonUtils.isNullOrEmpty(getSurname()) && !CommonUtils.isNullOrEmpty(getName()))
                    funcResult = PledgeSearchMethod.BY_PLEDGOR_INFO;
            }
        }

        return funcResult;
    }

}
package com.evolveum.midpoint.web.page.admin.certification.dto;

import java.io.Serializable;
import java.util.List;

/**
 * Created by Kate on 15.12.2015.
 */
public class StageDefinitionDto implements Serializable {
    public final static String F_NUMBER = "number";
    public final static String F_NAME = "name";
    public final static String F_DESCRIPTION = "description";
    public final static String F_DAYS = "days";
    public final static String F_NOTIFY_BEFORE_DEADLINE = "notifyBeforeDeadline";
    public final static String F_NOTIFY_ONLY_WHEN_NO_DECISION = "notifyOnlyWhenNoDecision";
    public final static String F_REVIEWER_SPECIFICATION = "reviewerSpecification";
    public final static String F_REVIEWER_DTO = "reviewerDto";

    private int number;
    private String name;
    private String description;
    private int days;
    private String notifyBeforeDeadline;
    private boolean notifyOnlyWhenNoDecision;
    private AccessCertificationReviewerDto reviewerDto;


    public int getNumber() {
        return number;
    }

    public void setNumber(int number) {
        this.number = number;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getDays() {
        return days;
    }

    public void setDays(int days) {
        this.days = days;
    }

    public String getNotifyBeforeDeadline() {
        return notifyBeforeDeadline;
    }

    public void setNotifyBeforeDeadline(String notifyBeforeDeadline) {
        this.notifyBeforeDeadline = notifyBeforeDeadline;
    }

    public boolean isNotifyOnlyWhenNoDecision() {
        return notifyOnlyWhenNoDecision;
    }

    public void setNotifyOnlyWhenNoDecision(boolean notifyOnlyWhenNoDecision) {
        this.notifyOnlyWhenNoDecision = notifyOnlyWhenNoDecision;
    }

    public AccessCertificationReviewerDto getReviewerDto() {
        return reviewerDto;
    }

    public void setReviewerDto(AccessCertificationReviewerDto reviewerDto) {
        this.reviewerDto = reviewerDto;
    }

}

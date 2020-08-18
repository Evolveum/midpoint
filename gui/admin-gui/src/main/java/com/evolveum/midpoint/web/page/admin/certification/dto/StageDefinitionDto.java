/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.certification.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import javax.xml.datatype.Duration;

import org.apache.commons.lang3.StringUtils;

import com.evolveum.midpoint.gui.api.util.ModelServiceLocator;
import com.evolveum.midpoint.schema.util.CertCampaignTypeUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

public class StageDefinitionDto implements Serializable {

    public static final String F_NUMBER = "number";
    public static final String F_NAME = "name";
    public static final String F_DESCRIPTION = "description";
    public static final String F_DURATION = "duration";
    public static final String F_NOTIFY_BEFORE_DEADLINE = "notifyBeforeDeadline";
    public static final String F_NOTIFY_ONLY_WHEN_NO_DECISION = "notifyOnlyWhenNoDecision";
    public static final String F_REVIEWER_SPECIFICATION = "reviewerSpecification";
    public static final String F_REVIEWER_DTO = "reviewerDto";
    public static final String F_OUTCOME_STRATEGY = "outcomeStrategy";
    public static final String F_OUTCOME_IF_NO_REVIEWERS = "outcomeIfNoReviewers";

    private int number;
    private String name;
    private String description;
    private String duration;
    private DeadlineRoundingType deadlineRounding;
    private String notifyBeforeDeadline;
    private boolean notifyOnlyWhenNoDecision;
    private AccessCertificationReviewerDto reviewerDto;
    private AccessCertificationCaseOutcomeStrategyType outcomeStrategy;
    private AccessCertificationResponseType outcomeIfNoReviewers;
    private List<AccessCertificationResponseType> stopReviewOnRaw;
    private List<AccessCertificationResponseType> advanceToNextStageOnRaw;
    private List<WorkItemTimedActionsType> timedActionsTypes;

    public StageDefinitionDto(AccessCertificationStageDefinitionType stageDefObj, ModelServiceLocator modelServiceLocator) throws SchemaException {
        if (stageDefObj != null) {
            setNumber(stageDefObj.getNumber());
            setName(stageDefObj.getName());
            setDescription(stageDefObj.getDescription());
            if (stageDefObj.getDuration() != null) {
                setDuration(stageDefObj.getDuration().toString());
            }
            setDeadlineRounding(stageDefObj.getDeadlineRounding());
            setNotifyBeforeDeadline(convertDurationListToString(stageDefObj.getNotifyBeforeDeadline()));
            setNotifyOnlyWhenNoDecision(Boolean.TRUE.equals(stageDefObj.isNotifyOnlyWhenNoDecision()));
            setReviewerDto(new AccessCertificationReviewerDto(stageDefObj.getReviewerSpecification(), modelServiceLocator));
            setOutcomeStrategy(stageDefObj.getOutcomeStrategy());
            setOutcomeIfNoReviewers(stageDefObj.getOutcomeIfNoReviewers());
            setStopReviewOnRaw(new ArrayList<>(stageDefObj.getStopReviewOn()));
            setAdvanceToNextStageOnRaw(new ArrayList<>(stageDefObj.getAdvanceToNextStageOn()));
            setTimedActionsTypes(new ArrayList<>(stageDefObj.getTimedActions()));
        } else {
            setReviewerDto(new AccessCertificationReviewerDto(null, modelServiceLocator));
        }
    }

    private String convertDurationListToString(List<Duration> list) {
        return StringUtils.join(list, ", ");
    }

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

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public DeadlineRoundingType getDeadlineRounding() {
        return deadlineRounding;
    }

    public void setDeadlineRounding(DeadlineRoundingType deadlineRounding) {
        this.deadlineRounding = deadlineRounding;
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

    public AccessCertificationCaseOutcomeStrategyType getOutcomeStrategy() {
        return outcomeStrategy;
    }

    public void setOutcomeStrategy(AccessCertificationCaseOutcomeStrategyType outcomeStrategy) {
        this.outcomeStrategy = outcomeStrategy;
    }

    public AccessCertificationResponseType getOutcomeIfNoReviewers() {
        return outcomeIfNoReviewers;
    }

    public void setOutcomeIfNoReviewers(AccessCertificationResponseType outcomeIfNoReviewers) {
        this.outcomeIfNoReviewers = outcomeIfNoReviewers;
    }

    public List<AccessCertificationResponseType> getStopReviewOn() {
        if (stopReviewOnRaw.isEmpty() && advanceToNextStageOnRaw.isEmpty()) {
            return null;
        }
        return CertCampaignTypeUtil.getOutcomesToStopOn(stopReviewOnRaw, advanceToNextStageOnRaw);
    }

    public List<AccessCertificationResponseType> getStopReviewOnRaw() {
        return stopReviewOnRaw;
    }

    public void setStopReviewOnRaw(List<AccessCertificationResponseType> stopReviewOnRaw) {
        this.stopReviewOnRaw = stopReviewOnRaw;
    }

    public List<AccessCertificationResponseType> getAdvanceToNextStageOnRaw() {
        return advanceToNextStageOnRaw;
    }

    public void setAdvanceToNextStageOnRaw(List<AccessCertificationResponseType> advanceToNextStageOnRaw) {
        this.advanceToNextStageOnRaw = advanceToNextStageOnRaw;
    }

    public List<WorkItemTimedActionsType> getTimedActionsTypes() {
        return timedActionsTypes;
    }

    public void setTimedActionsTypes(List<WorkItemTimedActionsType> timedActionsTypes) {
        this.timedActionsTypes = timedActionsTypes;
    }
}

package com.evolveum.midpoint.web.page.admin.certification.dto;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.schema.util.CertCampaignTypeUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCaseOutcomeStrategyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationResponseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationStageDefinitionType;
import org.apache.commons.lang3.StringUtils;

import javax.xml.datatype.Duration;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Kate on 15.12.2015.
 */
public class StageDefinitionDto implements Serializable {
    public final static String F_NUMBER = "number";
    public final static String F_NAME = "name";
    public final static String F_DESCRIPTION = "description";
    public final static String F_DURATION = "duration";
    public final static String F_NOTIFY_BEFORE_DEADLINE = "notifyBeforeDeadline";
    public final static String F_NOTIFY_ONLY_WHEN_NO_DECISION = "notifyOnlyWhenNoDecision";
    public final static String F_REVIEWER_SPECIFICATION = "reviewerSpecification";
    public final static String F_REVIEWER_DTO = "reviewerDto";
    public final static String F_OUTCOME_STRATEGY = "outcomeStrategy";
    public final static String F_OUTCOME_IF_NO_REVIEWERS = "outcomeIfNoReviewers";

    private int number;
    private String name;
    private String description;
    private String duration;
    private String notifyBeforeDeadline;
    private boolean notifyOnlyWhenNoDecision;
    private AccessCertificationReviewerDto reviewerDto;
    private AccessCertificationCaseOutcomeStrategyType outcomeStrategy;
    private AccessCertificationResponseType outcomeIfNoReviewers;
    private List<AccessCertificationResponseType> stopReviewOnRaw;
    private List<AccessCertificationResponseType> advanceToNextStageOnRaw;
	
	public StageDefinitionDto(AccessCertificationStageDefinitionType stageDefObj, PrismContext prismContext) throws SchemaException {
		if (stageDefObj != null) {
			setNumber(stageDefObj.getNumber());
			setName(stageDefObj.getName());
			setDescription(stageDefObj.getDescription());
			if (stageDefObj.getDuration() != null) {
				setDuration(stageDefObj.getDuration().toString());
			}
			setNotifyBeforeDeadline(convertDurationListToString(stageDefObj.getNotifyBeforeDeadline()));
			setNotifyOnlyWhenNoDecision(Boolean.TRUE.equals(stageDefObj.isNotifyOnlyWhenNoDecision()));
			setReviewerDto(new AccessCertificationReviewerDto(stageDefObj.getReviewerSpecification(), prismContext));
			setOutcomeStrategy(stageDefObj.getOutcomeStrategy());
			setOutcomeIfNoReviewers(stageDefObj.getOutcomeIfNoReviewers());
			setStopReviewOnRaw(new ArrayList<>(stageDefObj.getStopReviewOn()));
			setAdvanceToNextStageOnRaw(new ArrayList<>(stageDefObj.getAdvanceToNextStageOn()));
		} else {
			setReviewerDto(new AccessCertificationReviewerDto(null, prismContext));
		}
	}

	private String convertDurationListToString(List<Duration> list){
		String result = StringUtils.join(list, ", ");
		return result;
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
}

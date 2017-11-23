/*
 * Copyright (c) 2010-2017 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.web.component.wf;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.schema.util.WfContextUtil;
import com.evolveum.midpoint.web.component.data.column.ImagePanel;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.page.admin.server.dto.ApprovalOutcomeIcon;
import com.evolveum.midpoint.web.page.admin.workflow.dto.ApprovalProcessExecutionInformationDto;
import com.evolveum.midpoint.web.page.admin.workflow.dto.ApprovalStageExecutionInformationDto;
import com.evolveum.midpoint.web.page.admin.workflow.dto.ApproverEngagementDto;
import com.evolveum.midpoint.wf.util.ApprovalUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ApprovalLevelOutcomeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WorkItemOutcomeType;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;

import java.util.Objects;

/**
 * TEMPORARY IMPLEMENTATION. Replace with something graphically nice.
 *
 * @author mederly
 */
public class ApprovalProcessExecutionInformationPanel extends BasePanel<ApprovalProcessExecutionInformationDto> {

    private static final String ID_STAGES = "stages";
    private static final String ID_CURRENT_STAGE_MARKER = "currentStageMarker";
    private static final String ID_AUTOMATED_OUTCOME = "automatedOutcome";
    private static final String ID_APPROVERS = "approvers";
    private static final String ID_APPROVER_NAME = "approverName";
    private static final String ID_OUTCOME = "outcome";
    private static final String ID_PERFORMER_NAME = "performerName";
    private static final String ID_JUNCTION = "junction";
    private static final String ID_APPROVAL_BOX_CONTENT = "approvalBoxContent";
    private static final String ID_STAGE_NAME = "stageName";
    private static final String ID_STAGE_OUTCOME = "stageOutcome";
    private static final String ID_ARROW = "arrow";

    public ApprovalProcessExecutionInformationPanel(String id, IModel<ApprovalProcessExecutionInformationDto> model) {
        super(id, model);
        initLayout();
    }

    protected void initLayout() {

    	// TODO clean this code up!!!

        ListView<ApprovalStageExecutionInformationDto> stagesList = new ListView<ApprovalStageExecutionInformationDto>(ID_STAGES,
                new PropertyModel<>(getModel(), ApprovalProcessExecutionInformationDto.F_STAGES)) {
            @Override
            protected void populateItem(ListItem<ApprovalStageExecutionInformationDto> stagesListItem) {
                ApprovalProcessExecutionInformationDto process = ApprovalProcessExecutionInformationPanel.this.getModelObject();
                ApprovalStageExecutionInformationDto stage = stagesListItem.getModelObject();
                int stageNumber = stage.getStageNumber();
                int numberOfStages = process.getNumberOfStages();
                int currentStageNumber = process.getCurrentStageNumber();

	            WebMarkupContainer arrow = new WebMarkupContainer(ID_ARROW);
	            arrow.add(new VisibleBehaviour(() -> stageNumber > 1));
	            stagesListItem.add(arrow);

	            WebMarkupContainer currentStageMarker = new WebMarkupContainer(ID_CURRENT_STAGE_MARKER);
                currentStageMarker.add(new VisibleBehaviour(() -> stageNumber == currentStageNumber && process.isRunning()));
                stagesListItem.add(currentStageMarker);

                ListView<ApproverEngagementDto> approversList = new ListView<ApproverEngagementDto>(ID_APPROVERS,
                        new PropertyModel<>(stagesListItem.getModel(), ApprovalStageExecutionInformationDto.F_APPROVER_ENGAGEMENTS)) {
                    @Override
                    protected void populateItem(ListItem<ApproverEngagementDto> approversListItem) {
                        ApproverEngagementDto ae = approversListItem.getModelObject();

                        // original approver name
                        approversListItem.add(new Label(ID_APPROVER_NAME, 
                        		getApproverLabel("ApprovalProcessExecutionInformationPanel.approver", ae.getApproverRef())));

                        // outcome
                        WorkItemOutcomeType outcome = ae.getOutput() != null
                                ? ApprovalUtils.fromUri(ae.getOutput().getOutcome())
                                : null;
                        ApprovalOutcomeIcon outcomeIcon;
                        if (outcome != null) {
                            switch (outcome) {
                                case APPROVE: outcomeIcon = ApprovalOutcomeIcon.APPROVED; break;
                                case REJECT: outcomeIcon = ApprovalOutcomeIcon.REJECTED; break;
                                default: outcomeIcon = ApprovalOutcomeIcon.UNKNOWN; break;          // perhaps should throw AssertionError instead
                            }
                        } else {
                            if (stageNumber < currentStageNumber) {
                                outcomeIcon = ApprovalOutcomeIcon.EMPTY;         // history: do not show anything for work items with no outcome
                            } else if (stageNumber == currentStageNumber) {
                                outcomeIcon = process.isRunning() && stage.isReachable() ?
		                                ApprovalOutcomeIcon.IN_PROGRESS : ApprovalOutcomeIcon.CANCELLED;      // currently open
                            } else {
                                outcomeIcon = process.isRunning() && stage.isReachable()
		                                ? ApprovalOutcomeIcon.FUTURE : ApprovalOutcomeIcon.CANCELLED;
                            }
                        }
                        ImagePanel outcomePanel = new ImagePanel(ID_OUTCOME,
                                Model.of(outcomeIcon.getIcon()),
                                Model.of(getString(outcomeIcon.getTitle())));
                        outcomePanel.add(new VisibleBehaviour(() -> outcomeIcon != ApprovalOutcomeIcon.EMPTY));
                        approversListItem.add(outcomePanel);

	                    // content (incl. performer)
	                    WebMarkupContainer approvalBoxContent = new WebMarkupContainer(ID_APPROVAL_BOX_CONTENT);
	                    approversListItem.add(approvalBoxContent);
	                    approvalBoxContent.add(new VisibleBehaviour(() -> ae.getCompletedBy() != null &&
			                    !Objects.equals(ae.getCompletedBy().getOid(), ae.getApproverRef().getOid())));
	                    approvalBoxContent.add(new Label(ID_PERFORMER_NAME,
                        		getApproverLabel("ApprovalProcessExecutionInformationPanel.performer", ae.getCompletedBy())));

                        // junction
	                    Label junctionLabel = new Label(ID_JUNCTION, stage.isFirstDecides() ? "" : " & ");      // or "+" for first decides? probably not
	                    junctionLabel.setVisible(!stage.isFirstDecides() && !ae.isLast());                       // not showing "" to save space (if aligned vertically)
	                    approversListItem.add(junctionLabel);
                    }
                };
                approversList.setVisible(stage.getAutomatedCompletionReason() == null);
                stagesListItem.add(approversList);

                String autoCompletionKey;
                if (stage.getAutomatedCompletionReason() != null) {
                	switch (stage.getAutomatedCompletionReason()) {
		                case AUTO_COMPLETION_CONDITION: autoCompletionKey = "DecisionDto.AUTO_COMPLETION_CONDITION"; break;
		                case NO_ASSIGNEES_FOUND: autoCompletionKey = "DecisionDto.NO_ASSIGNEES_FOUND"; break;
		                default: autoCompletionKey = null;      // or throw an exception?
	                }
                } else {
                	autoCompletionKey = null;
                }
	            Label automatedOutcomeLabel = new Label(ID_AUTOMATED_OUTCOME, autoCompletionKey != null ? getString(autoCompletionKey) : "");
                automatedOutcomeLabel.setVisible(stage.getAutomatedCompletionReason() != null);
                stagesListItem.add(automatedOutcomeLabel);

                stagesListItem.add(new Label(ID_STAGE_NAME, getStageNameLabel(stage, stageNumber, numberOfStages)));

	            ApprovalLevelOutcomeType stageOutcome = stage.getOutcome();
	            ApprovalOutcomeIcon stageOutcomeIcon;
	            if (stageOutcome != null) {
		            switch (stageOutcome) {
			            case APPROVE: stageOutcomeIcon = ApprovalOutcomeIcon.APPROVED; break;
			            case REJECT: stageOutcomeIcon = ApprovalOutcomeIcon.REJECTED; break;
			            case SKIP: stageOutcomeIcon = ApprovalOutcomeIcon.SKIPPED; break;
			            default: stageOutcomeIcon = ApprovalOutcomeIcon.UNKNOWN; break;          // perhaps should throw AssertionError instead
		            }
	            } else {
		            if (stageNumber < currentStageNumber) {
			            stageOutcomeIcon = ApprovalOutcomeIcon.EMPTY;         // history: do not show anything (shouldn't occur, as historical stages are filled in)
		            } else if (stageNumber == currentStageNumber) {
			            stageOutcomeIcon = process.isRunning() && stage.isReachable() ?
					            ApprovalOutcomeIcon.IN_PROGRESS : ApprovalOutcomeIcon.CANCELLED;      // currently open
		            } else {
			            stageOutcomeIcon = process.isRunning() && stage.isReachable() ?
					            ApprovalOutcomeIcon.FUTURE : ApprovalOutcomeIcon.CANCELLED;
		            }
	            }
	            ImagePanel stageOutcomePanel = new ImagePanel(ID_STAGE_OUTCOME,
			            Model.of(stageOutcomeIcon.getIcon()),
			            Model.of(getString(stageOutcomeIcon.getTitle())));
	            stageOutcomePanel.add(new VisibleBehaviour(() -> stageOutcomeIcon != ApprovalOutcomeIcon.EMPTY));
	            stagesListItem.add(stageOutcomePanel);
            }

        };
        add(stagesList);
    }

	private String getStageNameLabel(ApprovalStageExecutionInformationDto stage, int stageNumber, int numberOfStages) {
		StringBuilder sb = new StringBuilder();
		sb.append(getString("ApprovalProcessExecutionInformationPanel.stage"));
		if (stage.getStageName() != null || stage.getStageDisplayName() != null) {
			sb.append(": ");
			sb.append(WfContextUtil.getStageInfo(stageNumber, numberOfStages, stage.getStageName(), stage.getStageDisplayName()));
			return sb.toString();
		} else {
			sb.append(stageNumber).append("/").append(numberOfStages);
			return sb.toString();
		}
	}

	private String getApproverLabel(String labelKey, ObjectReferenceType ref) {
		if (ref == null) {
			return "";
		}
		StringBuilder sb = new StringBuilder();
		sb.append(getString(labelKey));
		sb.append(": ");
		sb.append(WebComponentUtil.getName(ref));
		return sb.toString();	
	}
    
}

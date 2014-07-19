/*
 * Copyright (c) 2010-2013 Evolveum
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

package com.evolveum.midpoint.web.component.wf.processes.itemApproval;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.component.wf.DecisionsPanel;
import com.evolveum.midpoint.web.component.wf.WorkItemsPanel;
import com.evolveum.midpoint.web.page.admin.workflow.dto.DecisionDto;
import com.evolveum.midpoint.web.page.admin.workflow.dto.ProcessInstanceDto;
import com.evolveum.midpoint.web.page.admin.workflow.dto.WorkItemDto;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ApprovalLevelType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ApprovalSchemaType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DecisionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns.model.workflow.process_instance_state_3.ItemApprovalProcessState;
import com.evolveum.midpoint.xml.ns.model.workflow.process_instance_state_3.ItemApprovalRequestType;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.Validate;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;

import java.util.ArrayList;
import java.util.List;

/**
 * @author mederly
 */
public class ItemApprovalPanel extends Panel {

    private static final Trace LOGGER = TraceManager.getTrace(ItemApprovalPanel.class);

    private static final String ID_ITEM_TO_BE_APPROVED_LABEL = "itemToBeApprovedLabel";
    private static final String ID_ITEM_TO_BE_APPROVED = "itemToBeApproved";
    //private static final String ID_RESULT = "result";

    private static final String ID_APPROVAL_SCHEMA = "approvalSchema";

    private static final String ID_DECISIONS_DONE_LABEL = "decisionsDoneLabel";
    private static final String ID_DECISIONS_DONE = "decisionsDone";

    private static final String ID_CURRENT_WORK_ITEMS = "currentWorkItems";
    private static final String ID_CURRENT_WORK_ITEMS_LABEL = "currentWorkItemsLabel";

    private IModel<ProcessInstanceDto> model;

    public ItemApprovalPanel(String id, IModel<ProcessInstanceDto> model) {
        super(id);
        Validate.notNull(model);
        this.model = model;

        initLayout();
    }

    private void initLayout() {

        Label itemToBeApprovedLabel = new Label(ID_ITEM_TO_BE_APPROVED_LABEL, new StringResourceModel("${}", new AbstractReadOnlyModel<String>() {
            @Override
            public String getObject() {
                if (!model.getObject().isAnswered()) {
                    return "ItemApprovalPanel.itemToBeApproved";
                } else {
                    Boolean result = model.getObject().getAnswerAsBoolean();
                    if (result == null) {
                        return "ItemApprovalPanel.itemThatWasCompleted";        // actually, this should not happen, if the process is ItemApproval
                    } else if (result) {
                        return "ItemApprovalPanel.itemThatWasApproved";
                    } else {
                        return "ItemApprovalPanel.itemThatWasRejected";
                    }
                }
            }
        }));
        itemToBeApprovedLabel.add(new AttributeModifier("color", new AbstractReadOnlyModel<String>() {
            @Override
            public String getObject() {
                if (!model.getObject().isAnswered()) {
                    return "black";         // should not be visible, anyway
                } else {
                    Boolean result = model.getObject().getAnswerAsBoolean();
                    if (result == null) {
                        return "black";        // actually, this should not happen, if the process is ItemApproval
                    } else if (result) {
                        return "green";
                    } else {
                        return "red";
                    }
                }
            }
        }));
        add(itemToBeApprovedLabel);

        Label itemToBeApproved = new Label(ID_ITEM_TO_BE_APPROVED, new AbstractReadOnlyModel<String>() {

            @Override
            public String getObject() {

                ItemApprovalProcessState instanceState = (ItemApprovalProcessState) model.getObject().getInstanceState().getProcessSpecificState();
                ItemApprovalRequestType approvalRequestType = instanceState.getApprovalRequest();

                if (approvalRequestType == null) {
                    return "?";
                } else {
                    Object item = approvalRequestType.getItemToApprove();
                    if (item instanceof AssignmentType) {
                        AssignmentType assignmentType = (AssignmentType) item;
                        if (assignmentType.getTarget() != null) {
                            return assignmentType.getTarget().toString();
                        } else if (assignmentType.getTargetRef() != null) {
                            return assignmentType.getTargetRef().getOid() + " (" + assignmentType.getTargetRef().getType() + ")";
                        } else {
                            return "?";
                        }
                    } else {
                        return item != null ? item.toString() : "(none)";
                    }
                }
            }
        });
        add(itemToBeApproved);

        // todo i18n
        Label approvalSchema = new Label(ID_APPROVAL_SCHEMA, new AbstractReadOnlyModel() {
            @Override
            public Object getObject() {
                StringBuilder retval = new StringBuilder();

                ItemApprovalProcessState instanceState = (ItemApprovalProcessState) model.getObject().getInstanceState().getProcessSpecificState();
                ItemApprovalRequestType approvalRequestType = instanceState.getApprovalRequest();

                if (approvalRequestType == null) {
                    return "?";
                } else {
                    ApprovalSchemaType approvalSchema = approvalRequestType.getApprovalSchema();
                    if (approvalSchema != null) {
                        if (approvalSchema.getName() != null) {
                            retval.append("<b>");
                            retval.append(StringEscapeUtils.escapeHtml(approvalSchema.getName()));
                            retval.append("</b>");
                        }
                        if (approvalSchema.getDescription() != null) {
                            retval.append(" (");
                            retval.append(StringEscapeUtils.escapeHtml(approvalSchema.getDescription()));
                            retval.append(")");
                        }
                        if (approvalSchema.getName() != null || approvalSchema.getDescription() != null) {
                            retval.append("<br/>");
                        }
                        retval.append("Levels:<p/><ol>");
                        for (ApprovalLevelType level : approvalSchema.getLevel()) {
                            retval.append("<li>");
                            if (level.getName() != null) {
                                retval.append(StringEscapeUtils.escapeHtml(level.getName()));
                            } else {
                                retval.append("unnamed level");
                            }
                            if (level.getDescription() != null) {
                                retval.append(" (");
                                retval.append(StringEscapeUtils.escapeHtml(level.getDescription()));
                                retval.append(")");
                            }
                            if (level.getEvaluationStrategy() != null) {
                                retval.append(" [" + level.getEvaluationStrategy() + "]");
                            }
                            if (level.getAutomaticallyApproved() != null) {
                                String desc = level.getAutomaticallyApproved().getDescription();
                                if (desc != null) {
                                    retval.append(" (auto-approval condition: " + StringEscapeUtils.escapeHtml(desc) + ")");
                                } else {
                                    retval.append(" (auto-approval condition present)");
                                }
                            }
                            retval.append("<br/>Approvers:<ul>");
                            for (ObjectReferenceType approverRef : level.getApproverRef()) {
                                retval.append("<li>");
                                retval.append(approverRef.getOid());
                                if (approverRef.getType() != null) {
                                    retval.append(" (" + approverRef.getType().getLocalPart() + ")");
                                }
                                if (approverRef.getDescription() != null) {
                                    retval.append (" - " + approverRef.getDescription());
                                }
                                retval.append("</li>");
                            }
                            for (ExpressionType expression : level.getApproverExpression()) {
                                retval.append("<li>Expression: ");
                                // todo display the expression
                                if (expression.getDescription() != null) {
                                    retval.append(StringEscapeUtils.escapeHtml(expression.getDescription()));
                                } else {
                                    retval.append("(...)");
                                }
                                retval.append("</li>");
                            }
                        }

                        retval.append("</ul>");     // ends the list of approvers
                        retval.append("</ol>");         // ends the list of levels
                    }
                }
                return retval.toString();
            }
        });
        approvalSchema.setEscapeModelStrings(false);
        add(approvalSchema);

        add(new Label(ID_DECISIONS_DONE_LABEL, new StringResourceModel("ItemApprovalPanel.decisionsDoneWhenFinishedIs_${finished}", model)));

        add(new DecisionsPanel(ID_DECISIONS_DONE, new AbstractReadOnlyModel<List<DecisionDto>>() {
            @Override
            public List<DecisionDto> getObject() {
                List<DecisionDto> retval = new ArrayList<>();
                ProcessInstanceDto processInstanceDto = model.getObject();
                processInstanceDto.reviveIfNeeded(ItemApprovalPanel.this);
                ItemApprovalProcessState instanceState = (ItemApprovalProcessState) processInstanceDto.getInstanceState().getProcessSpecificState();
                List<DecisionType> allDecisions = instanceState.getDecisions();
                if (allDecisions != null) {
                    for (DecisionType decision : allDecisions) {
                        retval.add(new DecisionDto(decision));
                    }
                }
                return retval;
            }
        }));

        VisibleEnableBehaviour visibleIfRunning = new VisibleEnableBehaviour() {
            @Override
            public boolean isVisible() {
                return !model.getObject().isFinished();
            }
        };

        Label workItemsPanelLabel = new Label(ID_CURRENT_WORK_ITEMS_LABEL, new ResourceModel("ItemApprovalPanel.currentWorkItems"));
        workItemsPanelLabel.add(visibleIfRunning);
        add(workItemsPanelLabel);

        WorkItemsPanel workItemsPanel = new WorkItemsPanel(ID_CURRENT_WORK_ITEMS, new PropertyModel<List<WorkItemDto>>(model, "workItems"));
        workItemsPanel.add(visibleIfRunning);
        add(workItemsPanel);
    }
}

/*
 * Copyright (c) 2012 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2013 [name of copyright owner]
 */

package com.evolveum.midpoint.web.component.wf.processes.itemApproval;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.util.PrismUtil;
import com.evolveum.midpoint.prism.xml.PrismJaxbProcessor;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.JAXBUtil;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.component.wf.decisions.DecisionsPanel;
import com.evolveum.midpoint.web.component.wf.workItems.WorkItemsPanel;
import com.evolveum.midpoint.web.page.admin.workflow.dto.DecisionDto;
import com.evolveum.midpoint.web.page.admin.workflow.dto.ProcessInstanceDto;
import com.evolveum.midpoint.web.page.admin.workflow.dto.WorkItemDto;
import com.evolveum.midpoint.wf.processes.general.ApprovalRequest;
import com.evolveum.midpoint.wf.processes.general.Decision;
import com.evolveum.midpoint.wf.processes.general.ProcessVariableNames;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.*;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.Validate;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.*;

import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;
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
                Boolean result = model.getObject().getAnswer();
                if (result == Boolean.TRUE) {
                    return "ItemApprovalPanel.itemThatWasApproved";
                } else if (result == Boolean.FALSE) {
                    return "ItemApprovalPanel.itemThatWasRejected";
                } else {
                    return "ItemApprovalPanel.itemToBeApproved";
                }
            }
        }));
        itemToBeApprovedLabel.add(new AttributeModifier("color", new AbstractReadOnlyModel<String>() {
            @Override
            public String getObject() {
                Boolean result = model.getObject().getAnswer();
                if (result == Boolean.TRUE) {
                    return "green";
                } else if (result == Boolean.FALSE) {
                    return "red";
                } else {
                    return "black";          // should not be visible, anyway
                }
            }
        }));
        add(itemToBeApprovedLabel);

        Label itemToBeApproved = new Label(ID_ITEM_TO_BE_APPROVED, new AbstractReadOnlyModel<String>() {

            @Override
            public String getObject() {

                ApprovalRequest<?> approvalRequest = (ApprovalRequest) model.getObject().getVariable(ProcessVariableNames.APPROVAL_REQUEST);

                // todo delegate to process wrapper instead
                if (approvalRequest == null) {
                    return "?";
                } else {
                    Object item = approvalRequest.getItemToApprove();
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

                ApprovalRequest<?> approvalRequest = (ApprovalRequest) model.getObject().getVariable(ProcessVariableNames.APPROVAL_REQUEST);
                if (approvalRequest == null) {
                    return "?";
                } else {
                    ApprovalSchemaType approvalSchemaType = approvalRequest.getApprovalSchema();
                    if (approvalSchemaType.getName() != null) {
                        retval.append("<b>");
                        retval.append(StringEscapeUtils.escapeHtml(approvalSchemaType.getName()));
                        retval.append("</b>");
                    }
                    if (approvalSchemaType.getDescription() != null) {
                        retval.append(" (");
                        retval.append(StringEscapeUtils.escapeHtml(approvalSchemaType.getDescription()));
                        retval.append(")");
                    }
                    if (approvalSchemaType.getName() != null || approvalSchemaType.getDescription() != null) {
                        retval.append("<br/>");
                    }
                    retval.append("Levels:<p/><ol>");
                    for (ApprovalLevelType levelType : approvalSchemaType.getLevel()) {
                        retval.append("<li>");
                        if (levelType.getName() != null) {
                            retval.append(StringEscapeUtils.escapeHtml(levelType.getName()));
                        } else {
                            retval.append("unnamed level");
                        }
                        if (levelType.getDescription() != null) {
                            retval.append(" (");
                            retval.append(StringEscapeUtils.escapeHtml(levelType.getDescription()));
                            retval.append(")");
                        }
                        if (levelType.getEvaluationStrategy() != null) {
                            retval.append(" [" + levelType.getEvaluationStrategy() + "]");
                        }
                        if (levelType.getAutomaticallyApproved() != null) {
                            String desc = levelType.getAutomaticallyApproved().getDescription();
                            if (desc != null) {
                                retval.append(" (auto-approval condition: " + StringEscapeUtils.escapeHtml(desc) + ")");
                            } else {
                                retval.append(" (auto-approval condition present)");
                            }
                        }
                        retval.append("<br/>Approvers:<ul>");
                        for (ObjectReferenceType approverRef : levelType.getApproverRef()) {
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
                        for (ExpressionType expression : levelType.getApproverExpression()) {
                            retval.append("<li>Expression: ");
                            // todo display the expression
                            if (expression.getDescription() != null) {
                                retval.append(StringEscapeUtils.escapeHtml(expression.getDescription()));
                            } else {
                                retval.append("(...)");
                            }
//                            PrismJaxbProcessor p = prismContext.getPrismJaxbProcessor();
//                            try {
//                                retval.append(StringEscapeUtils.escapeHtml(p.marshalElementToString(expression, new QName("", "expression"))));
//                            } catch (JAXBException e) {
//                                LoggingUtils.logException(LOGGER, "Cannot display expression", e);
//                                retval.append(StringEscapeUtils.escapeHtml("Cannot display expression: " + e.getMessage()));
//                            }
                            retval.append("</li>");
                        }

                        retval.append("</ul>");     // ends the list of approvers
                    }
                    retval.append("</ol>");         // ends the list of levels
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
                List<DecisionDto> retval = new ArrayList<DecisionDto>();
                List<Decision> allDecisions = (List<Decision>) model.getObject().getVariable(ProcessVariableNames.ALL_DECISIONS);
                if (allDecisions != null) {
                    for (Decision decision : allDecisions) {
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

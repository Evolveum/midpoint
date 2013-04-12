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
 * Portions Copyrighted 2012 [name of copyright owner]
 */

package com.evolveum.midpoint.wf.processes.general;

import com.evolveum.midpoint.common.security.MidPointPrincipal;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.WfConstants;
import com.evolveum.midpoint.wf.activiti.SpringApplicationContextHolder;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ApprovalLevelType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.LevelEvaluationStrategyType;
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.JavaDelegate;
import org.apache.commons.lang.Validate;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Date;
import java.util.List;

/**
 * @author mederly
 */
public class RecordIndividualDecision implements JavaDelegate {

    private static final Trace LOGGER = TraceManager.getTrace(RecordIndividualDecision.class);

    public void execute(DelegateExecution execution) {

        ApprovalRequest approvalRequest = (ApprovalRequest) execution.getVariable(ProcessVariableNames.APPROVAL_REQUEST);
        Validate.notNull(approvalRequest, "approvalRequest is null");
        DecisionList decisionList = (DecisionList) execution.getVariable(ProcessVariableNames.DECISION_LIST);
        Validate.notNull(decisionList, "decisionList is null");
        List<Decision> allDecisions = (List<Decision>) execution.getVariable(ProcessVariableNames.ALL_DECISIONS);
        Validate.notNull(allDecisions, "allDecisions is null");
        ApprovalLevelType level = (ApprovalLevelType) execution.getVariable(ProcessVariableNames.LEVEL);
        Validate.notNull(level, "level is null");

        Boolean yesOrNo = (Boolean) execution.getVariable(WfConstants.FORM_FIELD_DECISION);
        String comment = (String) execution.getVariable(WfConstants.FORM_FIELD_COMMENT);

        Decision decision = new Decision();

        MidPointPrincipal user = getPrincipalUser();
        if (user != null) {
            decision.setUser(user.getName().getOrig());  //TODO: probably not correct setting
        } else {
            decision.setUser("?");    // todo
        }

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("======================================== Recording individual decision of " + user);
        }

        decision.setApproved(yesOrNo == null ? false : yesOrNo);
        decision.setComment(comment == null ? "" : comment);
        decision.setApprovalRequest(approvalRequest);
        decision.setDate(new Date());
        decisionList.addDecision(decision);

        allDecisions.add(decision);

        // here we carry out level evaluation strategy

        LevelEvaluationStrategyType levelEvaluationStrategyType = level.getEvaluationStrategy();

        Boolean setLoopApprovesInLevelStop = null;
        if (levelEvaluationStrategyType == LevelEvaluationStrategyType.FIRST_DECIDES) {
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Setting " + ProcessVariableNames.LOOP_APPROVERS_IN_LEVEL_STOP + " to true, because the level evaluation strategy is 'firstDecides'.");
            }
            setLoopApprovesInLevelStop = Boolean.TRUE;
        } else if (levelEvaluationStrategyType == LevelEvaluationStrategyType.ALL_MUST_AGREE && !decision.isApproved()) {
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Setting " + ProcessVariableNames.LOOP_APPROVERS_IN_LEVEL_STOP + " to true, because the level eval strategy is 'allMustApprove' and the decision was 'reject'.");
            }
            setLoopApprovesInLevelStop = Boolean.TRUE;
        }

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Logged decision '" + yesOrNo + "' for " + approvalRequest);
            LOGGER.trace("Resulting decision list = " + decisionList);
            LOGGER.trace("All decisions = " + allDecisions);
        }

        execution.setVariable(ProcessVariableNames.DECISION_LIST, decisionList);
        execution.setVariable(ProcessVariableNames.ALL_DECISIONS, allDecisions);
        if (setLoopApprovesInLevelStop != null) {
            execution.setVariable(ProcessVariableNames.LOOP_APPROVERS_IN_LEVEL_STOP, setLoopApprovesInLevelStop);
        }
        execution.setVariable(WfConstants.VARIABLE_MIDPOINT_STATE, "User " + decision.getUser() + " decided to " + (decision.isApproved() ? "approve" : "refuse") + " the request.");

        SpringApplicationContextHolder.getActivitiInterface().notifyMidpoint(execution);
    }

    // todo fixme: copied from web SecurityUtils
    public MidPointPrincipal getPrincipalUser() {
        SecurityContext ctx = SecurityContextHolder.getContext();
        if (ctx != null && ctx.getAuthentication() != null && ctx.getAuthentication().getPrincipal() != null) {
            Object principal = ctx.getAuthentication().getPrincipal();
            if (!(principal instanceof MidPointPrincipal)) {
                LOGGER.warn("Principal user in security context holder is {} but not type of {}",
                        new Object[]{principal, MidPointPrincipal.class.getName()});
                return null;
            }
            return (MidPointPrincipal) principal;
        } else {
            LOGGER.warn("No spring security context or authentication or principal.");
            return null;
        }
    }

}

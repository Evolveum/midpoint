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

package com.evolveum.midpoint.wf.processes.addroles;

import com.evolveum.midpoint.common.security.MidPointPrincipal;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.WfConstants;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ApprovalLevelType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.LevelEvaluationStrategyType;

import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.JavaDelegate;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Date;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: mederly
 * Date: 7.8.2012
 * Time: 17:56
 * To change this template use File | Settings | File Templates.
 */
public class RecordIndividualDecision implements JavaDelegate {

    private static final Trace LOGGER = TraceManager.getTrace(RecordIndividualDecision.class);

    public void execute(DelegateExecution execution) {

        AssignmentToApprove assignmentToApprove = (AssignmentToApprove) execution.getVariable(AddRoleAssignmentWrapper.ASSIGNMENT_TO_APPROVE);
        Boolean yesOrNo = (Boolean) execution.getVariable(WfConstants.FORM_FIELD_DECISION);
        String comment = (String) execution.getVariable(AddRoleAssignmentWrapper.FORM_FIELD_COMMENT);

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
        decision.setAssignmentToApprove(assignmentToApprove);
        decision.setDate(new Date());
        DecisionList decisionList = (DecisionList) execution.getVariable(AddRoleAssignmentWrapper.DECISION_LIST);
        decisionList.addDecision(decision);
        execution.setVariable(AddRoleAssignmentWrapper.DECISION_LIST, decisionList);

        List<Decision> allDecisions = (List<Decision>) execution.getVariable(AddRoleAssignmentWrapper.ALL_DECISIONS);
        allDecisions.add(decision);
        execution.setVariable(AddRoleAssignmentWrapper.ALL_DECISIONS, allDecisions);

        // here we carry out level evaluation strategy

        ApprovalLevelType level = (ApprovalLevelType) execution.getVariable(AddRoleAssignmentWrapper.LEVEL);
        if (level == null) {
            throw new SystemException("Process variable 'level' is not present; execution = " + execution);
        }
        LevelEvaluationStrategyType levelEvaluationStrategyType = level.getEvaluationStrategy();

        if (levelEvaluationStrategyType == LevelEvaluationStrategyType.FIRST_DECIDES) {
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Setting " + AddRoleAssignmentWrapper.LOOP_APPROVERS_IN_LEVEL_STOP + " to true, because the level evaluation strategy is 'firstDecides'.");
            }
            execution.setVariable(AddRoleAssignmentWrapper.LOOP_APPROVERS_IN_LEVEL_STOP, Boolean.TRUE);
        } else if (levelEvaluationStrategyType == LevelEvaluationStrategyType.ALL_MUST_AGREE && !decision.isApproved()) {
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Setting " + AddRoleAssignmentWrapper.LOOP_APPROVERS_IN_LEVEL_STOP + " to true, because the level eval strategy is 'allMustApprove' and the decision was 'reject'.");
            }
            execution.setVariable(AddRoleAssignmentWrapper.LOOP_APPROVERS_IN_LEVEL_STOP, Boolean.TRUE);
        }

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Logged decision '" + yesOrNo + "' for " + assignmentToApprove);
            LOGGER.trace("Resulting decision list = " + decisionList);
            LOGGER.trace("All decisions = " + allDecisions);
        }
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

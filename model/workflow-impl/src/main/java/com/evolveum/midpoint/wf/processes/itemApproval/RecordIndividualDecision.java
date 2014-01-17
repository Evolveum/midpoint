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

package com.evolveum.midpoint.wf.processes.itemApproval;

import com.evolveum.midpoint.common.security.MidPointPrincipal;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.activiti.SpringApplicationContextHolder;
import com.evolveum.midpoint.wf.processes.common.CommonProcessVariableNames;
import com.evolveum.midpoint.wf.util.ApprovalUtils;
import com.evolveum.midpoint.wf.util.MiscDataUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.LevelEvaluationStrategyType;
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.JavaDelegate;
import org.apache.commons.lang.Validate;

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
        approvalRequest.setPrismContext(SpringApplicationContextHolder.getPrismContext());

        List<Decision> decisionList = (List<Decision>) execution.getVariable(ProcessVariableNames.DECISIONS_IN_LEVEL);
        Validate.notNull(decisionList, "decisionList is null");

        List<Decision> allDecisions = (List<Decision>) execution.getVariable(ProcessVariableNames.ALL_DECISIONS);
        Validate.notNull(allDecisions, "allDecisions is null");

        ApprovalLevelImpl level = (ApprovalLevelImpl) execution.getVariable(ProcessVariableNames.LEVEL);
        Validate.notNull(level, "level is null");
        level.setPrismContext(SpringApplicationContextHolder.getPrismContext());

        boolean approved = ApprovalUtils.isApproved((String) execution.getVariable(CommonProcessVariableNames.FORM_FIELD_DECISION));
        String comment = (String) execution.getVariable(CommonProcessVariableNames.FORM_FIELD_COMMENT);

        Decision decision = new Decision();

        MidPointPrincipal user = MiscDataUtil.getPrincipalUser();
        if (user != null) {
            decision.setApproverName(user.getName().getOrig());  //TODO: probably not correct setting
            decision.setApproverOid(user.getOid());
        } else {
            decision.setApproverName("?");    // todo
            decision.setApproverOid("?");
        }

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("======================================== Recording individual decision of " + user);
        }

        decision.setApproved(approved);
        decision.setComment(comment == null ? "" : comment);
        decision.setDate(new Date());

        decisionList.add(decision);
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
            LOGGER.trace("Logged decision '" + approved + "' for " + approvalRequest);
            LOGGER.trace("Resulting decision list = " + decisionList);
            LOGGER.trace("All decisions = " + allDecisions);
        }

        execution.setVariable(ProcessVariableNames.DECISIONS_IN_LEVEL, decisionList);
        execution.setVariable(ProcessVariableNames.ALL_DECISIONS, allDecisions);
        if (setLoopApprovesInLevelStop != null) {
            execution.setVariable(ProcessVariableNames.LOOP_APPROVERS_IN_LEVEL_STOP, setLoopApprovesInLevelStop);
        }
        execution.setVariable(CommonProcessVariableNames.VARIABLE_MIDPOINT_STATE, "User " + decision.getApproverName() + " decided to " + (decision.isApproved() ? "approve" : "refuse") + " the request.");

        SpringApplicationContextHolder.getActivitiInterface().notifyMidpoint(execution);
    }

}

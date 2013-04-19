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

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.processes.CommonProcessVariableNames;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ApprovalLevelType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.LevelEvaluationStrategyType;
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.JavaDelegate;
import org.apache.commons.lang.Validate;

/**
 * @author  mederly
 */
public class SummarizeDecisionsInLevel implements JavaDelegate {

    private static final Trace LOGGER = TraceManager.getTrace(SummarizeDecisionsInLevel.class);

    public void execute(DelegateExecution execution) {

        DecisionList decisionList = (DecisionList) execution.getVariable(CommonProcessVariableNames.VARIABLE_DECISION_LIST);
        Validate.notNull(decisionList, "decisionList is null");
        ApprovalLevelType level = (ApprovalLevelType) execution.getVariable(ProcessVariableNames.LEVEL);
        Validate.notNull(level, "level is null");

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("****************************************** Summarizing decisions in level " + level.getName() + " (level evaluation strategy = " + level.getEvaluationStrategy() + "): ");
        }

        boolean allApproved = true;
        for (Decision decision : decisionList.getDecisionList()) {
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace(" - " + decision.toString());
            }
            allApproved &= decision.isApproved();
        }

        boolean approved;
        if (level.getEvaluationStrategy() == null || level.getEvaluationStrategy() == LevelEvaluationStrategyType.ALL_MUST_AGREE) {
            approved = allApproved;
        } else if (level.getEvaluationStrategy() == LevelEvaluationStrategyType.FIRST_DECIDES) {
            approved = decisionList.getDecisionList().get(0).isApproved();
        } else {
            throw new IllegalStateException("Unknown level evaluation strategy: " + level.getEvaluationStrategy());
        }
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("approved at this level = " + approved);
        }

        execution.setVariable(ProcessVariableNames.LOOP_LEVELS_STOP, !approved);
    }
}

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
import com.evolveum.midpoint.wf.activiti.SpringApplicationContextHolder;
import com.evolveum.midpoint.wf.processes.CommonProcessVariableNames;
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.JavaDelegate;
import org.apache.commons.lang.Validate;

public class PrepareResult implements JavaDelegate {

    private static final Trace LOGGER = TraceManager.getTrace(PrepareResult.class);

    public void execute(DelegateExecution execution) {

        Boolean loopLevelsStop = (Boolean) execution.getVariable(ProcessVariableNames.LOOP_LEVELS_STOP);
        Validate.notNull(loopLevelsStop, "loopLevels_stop is undefined");
        boolean approved = !loopLevelsStop;

        execution.setVariable(CommonProcessVariableNames.VARIABLE_WF_ANSWER, approved);
        execution.setVariable(CommonProcessVariableNames.VARIABLE_MIDPOINT_STATE, "Final decision is " + (approved ? "APPROVED" : "REFUSED"));

        SpringApplicationContextHolder.getActivitiInterface().notifyMidpointFinal(execution);
    }

}

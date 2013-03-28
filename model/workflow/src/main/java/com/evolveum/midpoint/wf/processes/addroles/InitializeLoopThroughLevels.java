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

import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.JavaDelegate;

public class InitializeLoopThroughLevels implements JavaDelegate {

    private static final Trace LOGGER = TraceManager.getTrace(InitializeLoopThroughLevels.class);

    public void execute(DelegateExecution execution) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Initialized loopLevels_stop; execution = " + execution);
        }
        execution.setVariableLocal(AddRoleAssignmentWrapper.LOOP_LEVELS_STOP, Boolean.FALSE);

        AssignmentToApprove assignmentToApprove =
                (AssignmentToApprove) execution.getVariable(AddRoleAssignmentWrapper.ASSIGNMENT_TO_APPROVE);
        if (assignmentToApprove == null) {
            throw new SystemException("assignmentToApprove process variable is null");
        }
        execution.setVariableLocal(AddRoleAssignmentWrapper.ROLE, assignmentToApprove.getRole());
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Initialized role to " + assignmentToApprove.getRole() + "; execution = " + execution);
        }
    }

}

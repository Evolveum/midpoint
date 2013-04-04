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
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.JavaDelegate;

/**
 * @author mederly
 */
public class DumpVariables implements JavaDelegate {

    private static final Trace LOGGER = TraceManager.getTrace(DumpVariables.class);

    public void execute(DelegateExecution execution) {

        if (LOGGER.isTraceEnabled()) {

            LOGGER.trace("--------------------------------- DumpVariables: " + execution.getCurrentActivityId());
            LOGGER.trace("Execution id=" + execution.getId() + ", parentId=" + execution.getParentId());
            LOGGER.trace("Variables:");
            for (String v : execution.getVariableNames()) {
                LOGGER.trace(" - " + v + " = " + execution.getVariable(v));
            }
            LOGGER.trace("Local variables:");
            for (String v : execution.getVariableNamesLocal()) {
                LOGGER.trace(" - " + v + " = " + execution.getVariableLocal(v));
            }
            LOGGER.trace("--------------------------------- DumpVariables: " + execution.getCurrentActivityId() + " END ------------");
        }

    }

}

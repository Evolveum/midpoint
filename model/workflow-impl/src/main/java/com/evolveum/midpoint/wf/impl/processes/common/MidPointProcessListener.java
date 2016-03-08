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

package com.evolveum.midpoint.wf.impl.processes.common;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.ExecutionListener;

import static com.evolveum.midpoint.wf.impl.processes.common.SpringApplicationContextHolder.getActivitiInterface;
import static java.lang.Boolean.TRUE;

/**
 * A listener that informs midPoint about process instance end for "smart" processes.
 * A process must register this listener in order for it to be invoked using a construction like this (at the level of process):
 *
 * <extensionElements>
 *   <activiti:executionListener event="end" class="com.evolveum.midpoint.wf.processes.common.MidPointProcessListener" />
 * </extensionElements>
 *
 * @author mederly
 */
public class MidPointProcessListener implements ExecutionListener {

    private static final Trace LOGGER = TraceManager.getTrace(MidPointProcessListener.class);

    @Override
    public void notify(DelegateExecution execution) {
        if (!ExecutionListener.EVENTNAME_END.equals(execution.getEventName())) {
            return;
        }
        LOGGER.trace("Signalling process end; execution id = {}, current activity id = {}, current activity name = {}, instance id = {}",
                execution.getId(), execution.getCurrentActivityId(), execution.getCurrentActivityName(), execution.getProcessInstanceId());
        try {
            getActivitiInterface().notifyMidpointAboutProcessFinishedEvent(execution);
        } catch (RuntimeException e) {
            LOGGER.trace("Got exception while processing process end event in midpoint", e);
            if (TRUE.equals(execution.getVariable(CommonProcessVariableNames.VARIABLE_PROCESS_INSTANCE_IS_STOPPING))) {
                LOGGER.trace("... the process is ending anyway, so we just ignore this exception");
            } else {
                throw e;
            }
        }
    }
}

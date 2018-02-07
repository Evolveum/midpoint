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

package com.evolveum.midpoint.wf.impl.processes.itemApproval;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.impl.processes.common.ActivitiUtil;
import com.evolveum.midpoint.wf.impl.processes.common.CommonProcessVariableNames;
import com.evolveum.midpoint.wf.util.ApprovalUtils;
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.JavaDelegate;

import static com.evolveum.midpoint.wf.impl.processes.common.SpringApplicationContextHolder.getActivitiInterface;

public class PrepareResult implements JavaDelegate {

    private static final Trace LOGGER = TraceManager.getTrace(PrepareResult.class);

    public void execute(DelegateExecution execution) {

        Boolean loopStagesStop = ActivitiUtil.getRequiredVariable(execution, ProcessVariableNames.LOOP_STAGES_STOP, Boolean.class, null);
        boolean approved = !loopStagesStop;

        execution.setVariable(CommonProcessVariableNames.VARIABLE_OUTCOME, ApprovalUtils.toUri(approved));

        getActivitiInterface().notifyMidpointAboutProcessFinishedEvent(execution);
    }

}

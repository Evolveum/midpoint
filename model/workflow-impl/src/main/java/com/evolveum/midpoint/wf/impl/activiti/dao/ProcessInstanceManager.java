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

package com.evolveum.midpoint.wf.impl.activiti.dao;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.api.WorkflowManager;
import com.evolveum.midpoint.wf.impl.WorkflowManagerImpl;
import com.evolveum.midpoint.wf.impl.activiti.ActivitiEngine;
import com.evolveum.midpoint.wf.impl.processes.common.CommonProcessVariableNames;

import org.activiti.engine.ActivitiException;
import org.activiti.engine.HistoryService;
import org.activiti.engine.RuntimeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author mederly
 */

@Component
public class ProcessInstanceManager {

    private static final transient Trace LOGGER = TraceManager.getTrace(ProcessInstanceManager.class);

    @Autowired
    private ActivitiEngine activitiEngine;

    private static final String DOT_CLASS = WorkflowManagerImpl.class.getName() + ".";
    private static final String DOT_INTERFACE = WorkflowManager.class.getName() + ".";

    private static final String OPERATION_STOP_PROCESS_INSTANCE = DOT_INTERFACE + "stopProcessInstance";
    private static final String OPERATION_DELETE_PROCESS_INSTANCE = DOT_INTERFACE + "deleteProcessInstance";

    public void stopProcessInstance(String instanceId, String username, OperationResult parentResult) {
        OperationResult result = parentResult.createSubresult(OPERATION_STOP_PROCESS_INSTANCE);
        result.addParam("instanceId", instanceId);

        RuntimeService rs = activitiEngine.getRuntimeService();
        try {
            LOGGER.trace("Stopping process instance {} on the request of {}", instanceId, username);
            String deletionMessage = "Process instance stopped on the request of " + username;
//            rs.setVariable(instanceId, CommonProcessVariableNames.VARIABLE_WF_STATE, deletionMessage);
            rs.setVariable(instanceId, CommonProcessVariableNames.VARIABLE_PROCESS_INSTANCE_IS_STOPPING, Boolean.TRUE);
            rs.deleteProcessInstance(instanceId, deletionMessage);
            result.recordSuccess();
        } catch (ActivitiException e) {
            result.recordFatalError("Process instance couldn't be stopped", e);
            LoggingUtils.logException(LOGGER, "Process instance {} couldn't be stopped", e, instanceId);
        }
    }

    public void deleteProcessInstance(String instanceId, OperationResult parentResult) {
        OperationResult result = parentResult.createSubresult(OPERATION_DELETE_PROCESS_INSTANCE);
        result.addParam("instanceId", instanceId);

        HistoryService hs = activitiEngine.getHistoryService();
        try {
            hs.deleteHistoricProcessInstance(instanceId);
            result.recordSuccess();
        } catch (ActivitiException e) {
            result.recordFatalError("Process instance couldn't be deleted", e);
            LoggingUtils.logException(LOGGER, "Process instance {} couldn't be deleted", e);
        }
    }

}

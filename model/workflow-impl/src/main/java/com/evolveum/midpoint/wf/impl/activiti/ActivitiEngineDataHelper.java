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

package com.evolveum.midpoint.wf.impl.activiti;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.api.WorkflowException;
import org.activiti.engine.ActivitiException;
import org.activiti.engine.HistoryService;
import org.activiti.engine.TaskService;
import org.activiti.engine.history.HistoricDetail;
import org.activiti.engine.history.HistoricDetailQuery;
import org.activiti.engine.history.HistoricVariableUpdate;
import org.activiti.engine.task.IdentityLink;
import org.activiti.engine.task.Task;
import org.activiti.engine.task.TaskQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Auxiliary methods for accessing data in Activiti.
 *
 * @author mederly
 */

@Component
public class ActivitiEngineDataHelper {

    private static final Trace LOGGER = TraceManager.getTrace(ActivitiEngineDataHelper.class);

    @Autowired
    private ActivitiEngine activitiEngine;

    public Map<String, Object> getHistoricVariables(String pid, OperationResult result) throws WorkflowException {

        Map<String, Object> retval = new HashMap<String, Object>();

        // copied from ActivitiInterface!
        HistoryService hs = activitiEngine.getHistoryService();

        try {

            HistoricDetailQuery hdq = hs.createHistoricDetailQuery()
                    .variableUpdates()
                    .processInstanceId(pid)
                    .orderByTime().desc();

            for (HistoricDetail hd : hdq.list())
            {
                HistoricVariableUpdate hvu = (HistoricVariableUpdate) hd;
                String name = hvu.getVariableName();
                Object value = hvu.getValue();
                if (!retval.containsKey(name)) {
                    retval.put(name, value);
                }
            }

            return retval;

        } catch (ActivitiException e) {
            String m = "Couldn't get variables for finished process instance " + pid;
            result.recordFatalError(m, e);
            throw new WorkflowException(m, e);
        }
    }

    public Map<String,Object> getProcessVariables(String taskId, OperationResult result) throws ObjectNotFoundException, WorkflowException {
        try {
            Task task = getTask(taskId);
            Map<String,Object> variables = activitiEngine.getProcessEngine().getRuntimeService().getVariables((task.getExecutionId()));
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Execution " + task.getExecutionId() + ", pid " + task.getProcessInstanceId() + ", variables = " + variables);
            }
            return variables;
        } catch (ActivitiException e) {
            String m = "Couldn't get variables for the process corresponding to task " + taskId;
            result.recordFatalError(m, e);
            throw new WorkflowException(m, e);
        }
    }

    // todo deduplicate this
    // todo: ObjectNotFoundException used in unusual way (not in connection with midPoint repository)
    private Task getTask(String taskId) throws ObjectNotFoundException {
        Task task = activitiEngine.getTaskService().createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            throw new ObjectNotFoundException("Task " + taskId + " could not be found.");
        }
        return task;
    }

    public Task getTaskById(String taskId, OperationResult result) throws ObjectNotFoundException {
        TaskService taskService = activitiEngine.getTaskService();
        TaskQuery tq = taskService.createTaskQuery()
                .taskId(taskId)
                .includeProcessVariables()
                .includeTaskLocalVariables();
        Task task = tq.singleResult();
        if (task == null) {
            result.recordFatalError("Task with ID " + taskId + " does not exist.");
            throw new ObjectNotFoundException("Task with ID " + taskId + " does not exist.");
        } else {
            return task;
        }
    }

}

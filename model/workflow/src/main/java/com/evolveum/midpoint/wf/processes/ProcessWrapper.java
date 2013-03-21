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

package com.evolveum.midpoint.wf.processes;

import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.wf.messages.ProcessEvent;
import org.activiti.engine.history.HistoricProcessInstance;
import org.activiti.engine.runtime.ProcessInstance;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * @author mederly
 */
public interface ProcessWrapper {

    /**
     * Determines whether to start a workflow process (and, if so, with what properties).
     *
     * @param context Context of the model operation.
     * @param task Enclosing task to which results of the process will be stored.
     * @param result
     * @return Instruction to start the process, or null if no process should be started.
     */
    StartProcessInstruction prepareStartCommandIfApplicable(ModelContext context, Task task, OperationResult result);

    /**
     * Does a specific processing at the end of the workflow. E.g. removes roles that were not approved from the model context.
     *
     * @param context Current context of the model operation.
     * @param event Message from the WfMS.
     * @param task Enclosing task to which results of the process were (and are) written.
     * @param result
     * @return TODO xxxxxx
     */
    void finishProcess(ModelContext context, ProcessEvent event, Task task, OperationResult result);

    String getProcessSpecificDetailsForTask(String instanceId, Map<String, Object> vars);
    String getProcessSpecificDetails(ProcessInstance instance, Map<String, Object> vars, List<org.activiti.engine.task.Task> tasks);
    String getProcessSpecificDetails(HistoricProcessInstance instance, Map<String, Object> vars);
}

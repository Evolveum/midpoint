/*
 * Copyright (c) 2010-2014 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.wf.processors;

import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.jobs.Job;
import com.evolveum.midpoint.wf.jobs.JobController;
import com.evolveum.midpoint.wf.jobs.JobCreationInstruction;
import com.evolveum.midpoint.wf.jobs.WfTaskUtil;
import com.evolveum.midpoint.wf.util.MiscDataUtil;
import com.evolveum.prism.xml.ns._public.types_2.PolyStringType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;

/**
 * @author mederly
 */
@Component
public class BaseModelInvocationProcessingHelper {

    private static final Trace LOGGER = TraceManager.getTrace(BaseModelInvocationProcessingHelper.class);

    @Autowired
    protected JobController jobController;

    @Autowired
    private WfTaskUtil wfTaskUtil;

    public Job createRootJob(ChangeProcessor changeProcessor, ModelContext context, Task taskFromModel, OperationResult result) throws SchemaException, ObjectNotFoundException {
        JobCreationInstruction rootInstruction = createInstructionForRoot(changeProcessor, context, taskFromModel);
        return jobController.createJob(rootInstruction, determineParentTaskForRoot(taskFromModel), result);
    }

    // to which object (e.g. user) is the task related?
    public PrismObject determineTaskObject(ModelContext context) {
        PrismObject taskObject = context.getFocusContext().getObjectNew();
        if (taskObject != null && taskObject.getOid() == null) {
            taskObject = null;
        }
        return taskObject;
    }

    public JobCreationInstruction createInstructionForRoot(ChangeProcessor changeProcessor, ModelContext modelContext, Task taskFromModel) throws SchemaException {
        return createInstructionForRoot(changeProcessor, modelContext, modelContext, taskFromModel);
    }

    public JobCreationInstruction createInstructionForRoot(ChangeProcessor changeProcessor, ModelContext contextForRoot, ModelContext modelContext, Task taskFromModel) throws SchemaException {

        String defaultTaskName = prepareRootTaskName(modelContext);
        PrismObject taskObject = determineTaskObject(modelContext);

        JobCreationInstruction instruction;
        if (contextForRoot != null) {
            instruction = JobCreationInstruction.createModelOperationRootJob(changeProcessor, contextForRoot);
        } else {
            instruction = JobCreationInstruction.createNoModelOperationRootJob(changeProcessor);
        }

        instruction.setTaskName(new PolyStringType(defaultTaskName));
        instruction.setTaskObject(taskObject);
        instruction.setTaskOwner(taskFromModel.getOwner());

        // At this moment, we HAVE NOT entered wait-for-tasks state, because we have no prerequisite tasks (in this case,
        // children) defined yet. Entering that state would result in immediate execution of this task. We have to
        // enter this state only after all children tasks are created.
        instruction.setCreateWaiting(true);

        return instruction;
    }

    protected String prepareRootTaskName(ModelContext context) {

        String operation;
        if (context.getFocusContext() != null && context.getFocusContext().getPrimaryDelta() != null) {
            operation = context.getFocusContext().getPrimaryDelta().getChangeType().toString().toLowerCase();
        } else {
            operation = "processing";
        }
        String name = MiscDataUtil.getObjectName(context);

        DateFormat dateFormat = DateFormat.getDateTimeInstance();
        String time = dateFormat.format(new Date());

        return "Workflow for " + operation + " " + name + " (started " + time + ")";
    }

    protected Task determineParentTaskForRoot(Task taskFromModel) {

        // this is important: if existing task which we have got from model is transient (this is usual case), we create our root task as a task without parent!
        // however, if the existing task is persistent (perhaps because the model operation executes already in the context of a workflow), we create a subtask
        // todo think heavily about this; there might be a problem if a transient task from model gets (in the future) persistent
        // -- in that case, it would not wait for its workflow-related children (but that's its problem, because children could finish even before
        // that task is switched to background)

        if (taskFromModel.isTransient()) {
            return null;
        } else {
            return taskFromModel;
        }
    }

    public Job createRootJob(JobCreationInstruction rootInstruction, Task taskFromModel, OperationResult result) throws SchemaException, ObjectNotFoundException {
        Job rootJob = jobController.createJob(rootInstruction, determineParentTaskForRoot(taskFromModel), result);
        wfTaskUtil.setRootTaskOidImmediate(taskFromModel, rootJob.getTask().getOid(), result);
        return rootJob;
    }

    public void logTasksBeforeStart(Job rootJob, OperationResult result) throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("============ Situation just before root task starts waiting for subtasks ============");
            LOGGER.trace("Root job = {}; task = {}", rootJob, rootJob.getTask().dump());
            if (rootJob.hasModelContext()) {
                LOGGER.trace("Context in root task = " + rootJob.retrieveModelContext(result).debugDump());
            }
            List<Job> children = rootJob.listChildren(result);
            for (int i = 0; i < children.size(); i++) {
                Job child = children.get(i);
                LOGGER.trace("Child job #" + i + " = {}, its task = {}", child, child.getTask().dump());
                if (child.hasModelContext()) {
                    LOGGER.trace("Context in child task = " + child.retrieveModelContext(result).debugDump());
                }
            }
            LOGGER.trace("Now the root task starts waiting for child tasks");
        }
    }

}

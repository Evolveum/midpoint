/*
 * Copyright (c) 2010-2017 Evolveum
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

package com.evolveum.midpoint.wf.impl.processors;

import com.evolveum.midpoint.common.LocalizationService;
import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.api.context.ModelProjectionContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.ObjectTreeDeltas;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.LocalizableMessage;
import com.evolveum.midpoint.util.LocalizableMessageBuilder;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.impl.tasks.WfTask;
import com.evolveum.midpoint.wf.impl.tasks.WfTaskController;
import com.evolveum.midpoint.wf.impl.tasks.WfTaskCreationInstruction;
import com.evolveum.midpoint.wf.impl.tasks.WfTaskUtil;
import com.evolveum.midpoint.wf.impl.util.MiscDataUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WfConfigurationType;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

import static com.evolveum.midpoint.prism.PrismObject.asPrismObject;

/**
 * Helper class intended to facilitate processing of model invocation.
 * (Currently deals mainly with root job creation.)
 *
 * Functionality provided here differs from the one in JobController mainly in
 * the fact that here we know about binding to model (ModelContext, model operation task),
 * and in JobController we do not.
 *
 * @author mederly
 */
@Component
public class BaseModelInvocationProcessingHelper {

    private static final Trace LOGGER = TraceManager.getTrace(BaseModelInvocationProcessingHelper.class);

    @Autowired protected WfTaskController wfTaskController;
	@Autowired private WfTaskUtil wfTaskUtil;
	@Autowired private LocalizationService localizationService;

	@Autowired
	@Qualifier("cacheRepositoryService")
	private RepositoryService repositoryService;

	private static final String APPROVING_AND_EXECUTING_KEY = "ApprovingAndExecuting.";
	private static final String CREATION_OF_KEY = "CreationOf";
	private static final String DELETION_OF_KEY = "DeletionOf";
	private static final String CHANGE_OF_KEY = "ChangeOf";

	/**
     * Creates a root job creation instruction.
     *
     * @param changeProcessor reference to the change processor responsible for the whole operation
     * @param modelContext model context in which the original model operation is carried out
     * @param taskFromModel task in which the original model operation is carried out
     * @param contextForRoot model context that should be put into the root task (might be different from the modelContext)
     * @return the job creation instruction
     * @throws SchemaException
     */
    public WfTaskCreationInstruction createInstructionForRoot(ChangeProcessor changeProcessor, ModelContext modelContext, Task taskFromModel, ModelContext contextForRoot, OperationResult result) throws SchemaException {

        WfTaskCreationInstruction<?, ?> instruction;
        if (contextForRoot != null) {
            instruction = WfTaskCreationInstruction.createModelOnly(changeProcessor, contextForRoot);
        } else {
            instruction = WfTaskCreationInstruction.createEmpty(changeProcessor);
        }

	    LocalizableMessage rootTaskName = determineRootTaskName(modelContext);
	    String rootTaskNameInDefaultLocale = localizationService.translate(rootTaskName, Locale.getDefault());
	    instruction.setLocalizableTaskName(rootTaskName);
	    instruction.setTaskName(rootTaskNameInDefaultLocale);
        instruction.setTaskObject(determineRootTaskObject(modelContext));
        instruction.setTaskOwner(taskFromModel.getOwner());
        instruction.setCreateTaskAsWaiting();

	    instruction.setObjectRef(modelContext, result);
		instruction.setRequesterRef(getRequester(taskFromModel, result));
        return instruction;
    }

    /**
     * More specific version of the previous method, having contextForRoot equals to modelContext.
     */
    public WfTaskCreationInstruction createInstructionForRoot(ChangeProcessor changeProcessor, ModelContext modelContext, Task taskFromModel, OperationResult result) throws SchemaException {
        return createInstructionForRoot(changeProcessor, modelContext, taskFromModel, modelContext, result);
    }

    /**
     * Determines the root task name (e.g. "Workflow for adding XYZ (started 1.2.2014 10:34)")
     * TODO allow change processor to influence this name
     */
    private LocalizableMessage determineRootTaskName(ModelContext<?> context) {
        String operationKey;
        if (context.getFocusContext() != null && context.getFocusContext().getPrimaryDelta() != null
				&& context.getFocusContext().getPrimaryDelta().getChangeType() != null) {
            switch (context.getFocusContext().getPrimaryDelta().getChangeType()) {
				case ADD: operationKey = CREATION_OF_KEY; break;
				case DELETE: operationKey = DELETION_OF_KEY; break;
				case MODIFY: operationKey = CHANGE_OF_KEY; break;
				default: throw new IllegalStateException();
			}
        } else {
            operationKey = CHANGE_OF_KEY;
        }
	    ObjectType focus = MiscDataUtil.getFocusObjectNewOrOld(context);
	    DateTimeFormatter formatter = DateTimeFormat.forStyle("MM").withLocale(Locale.getDefault());
	    String time = formatter.print(System.currentTimeMillis());

	    return new LocalizableMessageBuilder()
		        .key(APPROVING_AND_EXECUTING_KEY + operationKey)
		        .arg(ObjectTypeUtil.createDisplayInformation(asPrismObject(focus), false))
			    .arg(time)
		        .build();
    }

    /**
     * Determines where to "hang" workflow root task - whether as a subtask of model task, or nowhere (run it as a standalone task).
     */
    private Task determineParentTaskForRoot(Task taskFromModel) {

//        // this is important: if existing task which we have got from model is transient (this is usual case), we create our root task as a task without parent!
//        // however, if the existing task is persistent (perhaps because the model operation executes already in the context of a workflow), we create a subtask
//        // todo think heavily about this; there might be a problem if a transient task from model gets (in the future) persistent
//        // -- in that case, it would not wait for its workflow-related children (but that's its problem, because children could finish even before
//        // that task is switched to background)
//
//        if (taskFromModel.isTransient()) {
//            return null;
//        } else {
//            return taskFromModel;
//        }

	    /*
	     *  Let us create all approval tasks as independent ones. This might resolve more issues, for example:
	     *   - workflow tasks will be displayed on user Tasks tab (MID-4508): currently some of them are not, as they are technically subtasks
	     *   - background tasks that initiate approvals (e.g. live sync or reconciliation) could end up with lots of subtasks,
	     *     which makes their displaying take extraordinarily long
	     *
	     *  It is to be seen if this will have some negative consequences.
	     */
	    return null;
    }

    /**
     * To which object (e.g. user) is the task related?
     */
    private PrismObject determineRootTaskObject(ModelContext context) {
        PrismObject taskObject = context.getFocusContext().getObjectNew();
        if (taskObject != null && taskObject.getOid() == null) {
            taskObject = null;
        }
        return taskObject;
    }

    /**
     * Creates a root job, based on provided job start instruction.
     * Puts a reference to the workflow root task to the model task.
     *
     * @param rootInstruction instruction to use
     * @param taskFromModel (potential) parent task
     * @param wfConfigurationType
	 * @param result
	 * @return reference to a newly created job
     * @throws SchemaException
     * @throws ObjectNotFoundException
     */
    public WfTask submitRootTask(WfTaskCreationInstruction rootInstruction, Task taskFromModel, WfConfigurationType wfConfigurationType,
			OperationResult result)
            throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException {
        WfTask rootWfTask = wfTaskController.submitWfTask(rootInstruction, determineParentTaskForRoot(taskFromModel), wfConfigurationType, taskFromModel.getChannel(), result);
		result.setBackgroundTaskOid(rootWfTask.getTask().getOid());
		wfTaskUtil.setRootTaskOidImmediate(taskFromModel, rootWfTask.getTask().getOid(), result);
        return rootWfTask;
    }

    public void logJobsBeforeStart(WfTask rootWfTask, OperationResult result) throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException, ExpressionEvaluationException {
        if (!LOGGER.isTraceEnabled()) {
            return;
        }

        StringBuilder sb = new StringBuilder();

        sb.append("===[ Situation just before root task starts waiting for subtasks ]===\n");
        sb.append("Root job = ").append(rootWfTask).append("; task = ").append(rootWfTask.getTask().debugDump()).append("\n");
        if (rootWfTask.hasModelContext()) {
            sb.append("Context in root task: \n").append(rootWfTask.retrieveModelContext(result).debugDump(1)).append("\n");
        }
        List<WfTask> children = rootWfTask.listChildren(result);
        for (int i = 0; i < children.size(); i++) {
            WfTask child = children.get(i);
            sb.append("Child job #").append(i).append(" = ").append(child).append(", its task:\n").append(child.getTask().debugDump(1));
            if (child.hasModelContext()) {
                sb.append("Context in child task:\n").append(child.retrieveModelContext(result).debugDump(2));
            }
        }
        LOGGER.trace("\n{}", sb.toString());
        LOGGER.trace("Now the root task starts waiting for child tasks");
    }

	public PrismObject<UserType> getRequester(Task task, OperationResult result) {
		if (task.getOwner() == null) {
			LOGGER.warn("No requester in task {} -- continuing, but the situation is suspicious.", task);
			return null;
		}

		// let's get fresh data (not the ones read on user login)
		PrismObject<UserType> requester;
		try {
			requester = repositoryService.getObject(UserType.class, task.getOwner().getOid(), null, result);
		} catch (ObjectNotFoundException e) {
			LoggingUtils.logException(LOGGER, "Couldn't get data about task requester (" + task.getOwner() + "), because it does not exist in repository anymore. Using cached data.", e);
			requester = task.getOwner().clone();
		} catch (SchemaException e) {
			LoggingUtils.logUnexpectedException(LOGGER, "Couldn't get data about task requester (" + task.getOwner() + "), due to schema exception. Using cached data.", e);
			requester = task.getOwner().clone();
		}
		return requester;
	}

	public <O extends ObjectType> ObjectTreeDeltas<O> extractTreeDeltasFromModelContext(ModelContext<O> modelContext) {
		ObjectTreeDeltas<O> objectTreeDeltas = new ObjectTreeDeltas<>(modelContext.getPrismContext());
		if (modelContext.getFocusContext() != null && modelContext.getFocusContext().getPrimaryDelta() != null) {
			objectTreeDeltas.setFocusChange(modelContext.getFocusContext().getPrimaryDelta().clone());
		}

		for (ModelProjectionContext projectionContext : modelContext.getProjectionContexts()) {
			if (projectionContext.getPrimaryDelta() != null) {
				objectTreeDeltas.addProjectionChange(projectionContext.getResourceShadowDiscriminator(), projectionContext.getPrimaryDelta());
			}
		}
		return objectTreeDeltas;
	}


}

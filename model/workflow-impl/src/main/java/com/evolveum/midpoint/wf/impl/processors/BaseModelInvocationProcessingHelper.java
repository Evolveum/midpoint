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
import com.evolveum.midpoint.model.api.context.ModelElementContext;
import com.evolveum.midpoint.model.api.context.ModelProjectionContext;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.ObjectTreeDeltas;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.LocalizableMessage;
import com.evolveum.midpoint.util.LocalizableMessageBuilder;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.impl.engine.EngineInvocationContext;
import com.evolveum.midpoint.wf.impl.engine.WorkflowEngine;
import com.evolveum.midpoint.wf.impl.tasks.StartInstruction;
import com.evolveum.midpoint.wf.impl._temp.TemporaryHelper;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import org.jetbrains.annotations.NotNull;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

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

	@Autowired private TemporaryHelper temporaryHelper;
	@Autowired private WorkflowEngine workflowEngine;
	@Autowired private LocalizationService localizationService;
	@Autowired private PrismContext prismContext;

	@Autowired
	@Qualifier("cacheRepositoryService")
	private RepositoryService repositoryService;

	private static final String APPROVING_AND_EXECUTING_KEY = "ApprovingAndExecuting.";
	private static final String CREATION_OF_KEY = "CreationOf";
	private static final String DELETION_OF_KEY = "DeletionOf";
	private static final String CHANGE_OF_KEY = "ChangeOf";

	/**
	 * Retrieves focus object name from the model context.
	 */
	public static String getFocusObjectName(ModelContext<? extends ObjectType> modelContext) {
	    ObjectType object = getFocusObjectNewOrOld(modelContext);
	    return object.getName() != null ? object.getName().getOrig() : null;
	}

	public static String getFocusObjectOid(ModelContext<?> modelContext) {
	    ModelElementContext<?> fc = modelContext.getFocusContext();
	    if (fc.getObjectNew() != null && fc.getObjectNew().getOid() != null) {
	        return fc.getObjectNew().getOid();
	    } else if (fc.getObjectOld() != null && fc.getObjectOld().getOid() != null) {
	        return fc.getObjectOld().getOid();
	    } else {
	        return null;
	    }
	}

	public static ObjectType getFocusObjectNewOrOld(ModelContext<? extends ObjectType> modelContext) {
	    ModelElementContext<? extends ObjectType> fc = modelContext.getFocusContext();
	    PrismObject<? extends ObjectType> prism = fc.getObjectNew() != null ? fc.getObjectNew() : fc.getObjectOld();
	    if (prism == null) {
	        throw new IllegalStateException("No object (new or old) in model context");
	    }
	    return prism.asObjectable();
	}

	/**
     * Creates a root job creation instruction.
     *
     * @param changeProcessor reference to the change processor responsible for the whole operation
     * @param modelContext model context that should be put into the root task (might be different from the modelContext)
     * @param task task in which the original model operation is carried out
     * @return the job creation instruction
     * @throws SchemaException
     */
    public StartInstruction createInstructionForRoot(ChangeProcessor changeProcessor, @NotNull ModelContext<?> modelContext,
		    Task task, OperationResult result) throws SchemaException {
        StartInstruction instruction = StartInstruction.create(changeProcessor);
        instruction.setModelContext(modelContext);

	    LocalizableMessage rootCaseName = determineRootCaseName(modelContext);
	    String rootCaseNameInDefaultLocale = localizationService.translate(rootCaseName, Locale.getDefault());
	    instruction.setLocalizableName(rootCaseName);
	    instruction.setName(rootCaseNameInDefaultLocale);
	    instruction.setObjectRef(modelContext);
		instruction.setRequesterRef(getRequester(task, result));
        return instruction;
    }

    /**
     * Determines the root task name (e.g. "Workflow for adding XYZ (started 1.2.2014 10:34)")
     * TODO allow change processor to influence this name
     */
    private LocalizableMessage determineRootCaseName(ModelContext<?> context) {
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
	    ObjectType focus = getFocusObjectNewOrOld(context);
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
//        if (task.isTransient()) {
//            return null;
//        } else {
//            return task;
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
     * @param task (potential) parent task
	 * @param result
	 * @return reference to a newly created job
     * @throws SchemaException
     * @throws ObjectNotFoundException
     */
    public CaseType addRoot(StartInstruction rootInstruction, Task task,
		    OperationResult result) throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException {
        CaseType rootCase = addCase(rootInstruction, task, result);
		result.setCaseOid(rootCase.getOid());
		//wfTaskUtil.setRootTaskOidImmediate(task, rootCase.getOid(), result);
        return rootCase;
    }

    public void logJobsBeforeStart(CaseType rootCase, Task task, OperationResult result) throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException, ExpressionEvaluationException {
        if (!LOGGER.isTraceEnabled()) {
            return;
        }

        StringBuilder sb = new StringBuilder();

        sb.append("===[ Situation just after case tree creation ]===\n");
        sb.append("Root case:\n").append(rootCase.asPrismObject().debugDump(1)).append("\n");
        List<CaseType> children = listChildren(rootCase, result);
        for (int i = 0; i < children.size(); i++) {
            CaseType child = children.get(i);
            sb.append("Child job #").append(i).append(":\n").append(child.asPrismObject().debugDump(1));
        }
        LOGGER.trace("\n{}", sb.toString());
    }

	public List<CaseType> listChildren(CaseType aCase, OperationResult result) throws SchemaException {
		ObjectQuery query = prismContext.queryFor(CaseType.class)
				.item(CaseType.F_PARENT_REF).ref(aCase.getOid())
				.build();
		SearchResultList<PrismObject<CaseType>> objects = repositoryService
				.searchObjects(CaseType.class, query, null, result);
		return objects.stream().map(o -> o.asObjectable()).collect(Collectors.toList());
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

	/**
	 * TODO
	 * @param parentCase the task that will be the parent of the task of newly created wf-task; it may be null
	 * @param instruction the wf task creation instruction
	 * @param task
	 */
	public CaseType addCase(StartInstruction instruction, Task task, OperationResult result)
			throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException {
		LOGGER.trace("Processing start instruction:\n{}", instruction.debugDumpLazily());
		CaseType aCase = instruction.getCase();
		repositoryService.addObject(aCase.asPrismObject(), null, result);

		if (instruction.startsWorkflowProcess()) {
			EngineInvocationContext ctx = new EngineInvocationContext(aCase, task);
			workflowEngine.startProcessInstance(ctx, result);
		}
		return aCase;
	}

}

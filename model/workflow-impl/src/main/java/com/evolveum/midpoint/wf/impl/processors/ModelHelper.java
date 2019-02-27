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
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.LocalizableMessage;
import com.evolveum.midpoint.util.LocalizableMessageBuilder;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.impl.engine.EngineInvocationContext;
import com.evolveum.midpoint.wf.impl.engine.WorkflowEngine;
import com.evolveum.midpoint.wf.impl.util.MiscHelper;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import org.jetbrains.annotations.NotNull;
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
 */
@Component
public class ModelHelper {

    private static final Trace LOGGER = TraceManager.getTrace(ModelHelper.class);

	@Autowired private WorkflowEngine workflowEngine;
	@Autowired private LocalizationService localizationService;
	@Autowired private PrismContext prismContext;
	@Autowired private MiscHelper miscHelper;

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
     * @param modelContext model context that should be put into the root task (might be different from the modelContext)
     * @param task task in which the original model operation is carried out
     * @return the job creation instruction
     * @throws SchemaException
     */
    public StartInstruction createInstructionForRoot(ChangeProcessor changeProcessor, @NotNull ModelInvocationContext<?> ctx,
		    ModelContext<?> contextForRootCase, OperationResult result) throws SchemaException {
        StartInstruction instruction = StartInstruction.create(changeProcessor);
        instruction.setModelContext(contextForRootCase);

	    LocalizableMessage rootCaseName = determineRootCaseName(ctx);
	    String rootCaseNameInDefaultLocale = localizationService.translate(rootCaseName, Locale.getDefault());
	    instruction.setLocalizableName(rootCaseName);
	    instruction.setName(rootCaseNameInDefaultLocale);
	    instruction.setObjectRef(ctx);
		instruction.setRequesterRef(ctx.getRequestor(result));
        return instruction;
    }

    /**
     * Determines the root task name (e.g. "Workflow for adding XYZ (started 1.2.2014 10:34)")
     * TODO allow change processor to influence this name
     */
    private LocalizableMessage determineRootCaseName(ModelInvocationContext<?> ctx) {
        String operationKey;
	    ModelElementContext<?> focusContext = ctx.modelContext.getFocusContext();
	    if (focusContext != null && focusContext.getPrimaryDelta() != null
				&& focusContext.getPrimaryDelta().getChangeType() != null) {
            switch (focusContext.getPrimaryDelta().getChangeType()) {
				case ADD: operationKey = CREATION_OF_KEY; break;
				case DELETE: operationKey = DELETION_OF_KEY; break;
				case MODIFY: operationKey = CHANGE_OF_KEY; break;
				default: throw new IllegalStateException();
			}
        } else {
            operationKey = CHANGE_OF_KEY;
        }
	    ObjectType focus = ctx.getFocusObjectNewOrOld();
	    DateTimeFormatter formatter = DateTimeFormat.forStyle("MM").withLocale(Locale.getDefault());
	    String time = formatter.print(System.currentTimeMillis());

	    return new LocalizableMessageBuilder()
		        .key(APPROVING_AND_EXECUTING_KEY + operationKey)
		        .arg(ObjectTypeUtil.createDisplayInformation(asPrismObject(focus), false))
			    .arg(time)
		        .build();
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
        List<CaseType> children = miscHelper.getSubcases(rootCase, result);
        for (int i = 0; i < children.size(); i++) {
            CaseType child = children.get(i);
            sb.append("Child job #").append(i).append(":\n").append(child.asPrismObject().debugDump(1));
        }
        LOGGER.trace("\n{}", sb.toString());
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

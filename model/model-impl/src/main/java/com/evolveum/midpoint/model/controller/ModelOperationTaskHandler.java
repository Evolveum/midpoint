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

package com.evolveum.midpoint.model.controller;

import com.evolveum.midpoint.model.api.hooks.HookRegistry;
import com.evolveum.midpoint.model.lens.Clockwork;
import com.evolveum.midpoint.model.lens.LensContext;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.*;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.model.model_context_2.LensContextType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.xml.namespace.QName;
import java.util.List;

/**
 * Handles a "ModelOperation task" - executes a given model operation in a context
 * of the task (i.e., in most cases, asynchronously).
 * 
 * The operation and its state is described in ModelOperationState element. When sent to this handler,
 * the current state of operation is unwrapped and processed by the ModelController.
 *
 * ModelOperationState consists of:
 *  - ModelOperationKindType: add, modify, delete,
 *  - ModelOperationStageType: primary, secondary, execute,
 *  - OperationData (base64-encoded serialized data structure, specific to MOKT/MOST)
 *
 *  CURRENTLY NOT WORKING (will be revived in midpoint 2.2)
 *
 * @author mederly
 */

@Component
public class ModelOperationTaskHandler implements TaskHandler {

	public static final String MODEL_OPERATION_TASK_URI = "http://midpoint.evolveum.com/model/model-operation-handler-1";

    // copied from WfTaskUtil (todo eliminate duplicity)
    public static final String WORKFLOW_EXTENSION_NS = "http://midpoint.evolveum.com/model/workflow/extension-2";
    public static final QName WFMODEL_CONTEXT_PROPERTY_NAME = new QName(WORKFLOW_EXTENSION_NS, "wfModelContext");

    @Autowired(required = true)
	private TaskManager taskManager;

	@Autowired(required = true)
	private ModelController model;

    @Autowired(required = true)
    private PrismContext prismContext;

    @Autowired(required = true)
    private Clockwork clockwork;

    @Autowired(required = false)
    private HookRegistry hookRegistry;

	private static final Trace LOGGER = TraceManager.getTrace(ModelOperationTaskHandler.class);

	@Override
	public TaskRunResult run(Task task) {

		OperationResult result = task.getResult().createSubresult("ModelOperationTaskHandler.run");
		TaskRunResult runResult = new TaskRunResult();

		/*
		 * Call the model operation body.
		 */

        PrismProperty<LensContextType> contextTypeProperty = task.getExtension(WFMODEL_CONTEXT_PROPERTY_NAME);
        if (contextTypeProperty == null) {
            throw new IllegalStateException("There's no wfModelContext information in task " + task);   // todo error handling
        }

        LensContext context = null;
        try {
            context = LensContext.fromJaxb(contextTypeProperty.getRealValue(), prismContext);
        } catch (SchemaException e) {
            throw new SystemException("Cannot recover wfModelContext from task " + task);   // todo error handling
        }

        try {
            clockwork.run(context, task, result);
            if (result.isUnknown()) {
                result.computeStatus();
            }
            runResult.setRunResultStatus(TaskRunResult.TaskRunResultStatus.FINISHED);
        } catch (Exception e) { // todo
            String message = "An exception occurred within model operation, in task " + task;
            LoggingUtils.logException(LOGGER, message, e);
            result.recordPartialError(message, e);
            // TODO: here we do not know whether the error is temporary or permanent (in the future we could discriminate on the basis of particular exception caught)
            runResult.setRunResultStatus(TaskRunResult.TaskRunResultStatus.TEMPORARY_ERROR);
		}

        task.getResult().recomputeStatus();
		runResult.setOperationResult(task.getResult());
		return runResult;
	}

	@Override
	public Long heartbeat(Task task) {
		return null; // null - as *not* to record progress
	}

	@Override
	public void refreshStatus(Task task) {
	}

    @Override
    public String getCategoryName(Task task) {
        return TaskCategory.WORKFLOW;
    }

    @Override
    public List<String> getCategoryNames() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

	@PostConstruct
	private void initialize() throws Exception {
		LOGGER.info("Registering with taskManager as a handler for " + MODEL_OPERATION_TASK_URI);
		taskManager.registerHandler(MODEL_OPERATION_TASK_URI, this);
	}
}

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

import com.evolveum.midpoint.model.lens.Clockwork;
import com.evolveum.midpoint.model.lens.LensContext;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismProperty;
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
 * The context of the model operation (i.e., model context) is stored in task extension property
 * called "modelContext". When this handler is executed, the context is retrieved, unwrapped from
 * its XML representation, and the model operation is (re)started.
 *
 * @author mederly
 */

@Component
public class ModelOperationTaskHandler implements TaskHandler {

    private static final Trace LOGGER = TraceManager.getTrace(ModelOperationTaskHandler.class);

    private static final String DOT_CLASS = ModelOperationTaskHandler.class.getName() + ".";

    public static final String MODEL_OPERATION_TASK_URI = "http://midpoint.evolveum.com/model/model-operation-handler-2";
    public static final String MODEL_CONTEXT_NS = "http://midpoint.evolveum.com/xml/ns/public/model/model-context-2";
    public static final QName MODEL_CONTEXT_PROPERTY = new QName(MODEL_CONTEXT_NS, "modelContext");
    public static final QName SKIP_MODEL_CONTEXT_PROCESSING_PROPERTY = new QName(MODEL_CONTEXT_NS, "skipModelContextProcessing");

    @Autowired(required = true)
	private TaskManager taskManager;

    @Autowired(required = true)
    private PrismContext prismContext;

    @Autowired(required = true)
    private Clockwork clockwork;

	@Override
	public TaskRunResult run(Task task) {

		OperationResult result = task.getResult().createSubresult(DOT_CLASS + "run");
		TaskRunResult runResult = new TaskRunResult();

        PrismProperty<Boolean> skipProperty = task.getExtension(SKIP_MODEL_CONTEXT_PROCESSING_PROPERTY);

        if (skipProperty != null && skipProperty.getRealValue() == Boolean.TRUE) {

            LOGGER.trace("Found " + skipProperty + ", skipping the model operation execution.");
            if (result.isUnknown()) {
                result.computeStatus();
            }
            runResult.setRunResultStatus(TaskRunResult.TaskRunResultStatus.FINISHED);

        } else {

            PrismProperty<LensContextType> contextTypeProperty = task.getExtension(MODEL_CONTEXT_PROPERTY);
            if (contextTypeProperty == null) {
                throw new SystemException("There's no model context property in task " + task + " (" + MODEL_CONTEXT_PROPERTY + ")");
            }

            LensContext context = null;
            try {
                context = LensContext.fromJaxb(contextTypeProperty.getRealValue(), prismContext);
            } catch (SchemaException e) {
                throw new SystemException("Cannot recover model context from task " + task + " due to schema exception", e);
            }

            try {
                clockwork.run(context, task, result);

                contextTypeProperty.setRealValue(context.toJaxb());
                task.setExtensionPropertyImmediate(contextTypeProperty, result);

                if (result.isUnknown()) {
                    result.computeStatus();
                }
                runResult.setRunResultStatus(TaskRunResult.TaskRunResultStatus.FINISHED);
            } catch (Exception e) { // too many various exceptions; will be fixed with java7 :)
                String message = "An exception occurred within model operation, in task " + task;
                LoggingUtils.logException(LOGGER, message, e);
                result.recordPartialError(message, e);
                // TODO: here we do not know whether the error is temporary or permanent (in the future we could discriminate on the basis of particular exception caught)
                runResult.setRunResultStatus(TaskRunResult.TaskRunResultStatus.TEMPORARY_ERROR);
            }
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
        return null;
    }

	@PostConstruct
	private void initialize() {
        if (LOGGER.isTraceEnabled()) {
		    LOGGER.trace("Registering with taskManager as a handler for " + MODEL_OPERATION_TASK_URI);
        }
		taskManager.registerHandler(MODEL_OPERATION_TASK_URI, this);
	}
}

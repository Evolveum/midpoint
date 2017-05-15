/*
 * Copyright (c) 2010-2017 Evolveum
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
package com.evolveum.midpoint.wf.impl.processors.general;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.audit.api.AuditEventStage;
import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.api.hooks.HookOperationMode;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.api.WorkflowException;
import com.evolveum.midpoint.wf.impl.activiti.ActivitiEngine;
import com.evolveum.midpoint.wf.impl.tasks.WfTask;
import com.evolveum.midpoint.wf.impl.tasks.WfTaskController;
import com.evolveum.midpoint.wf.impl.tasks.WfTaskCreationInstruction;
import com.evolveum.midpoint.wf.impl.tasks.WfTaskUtil;
import com.evolveum.midpoint.wf.impl.messages.ProcessEvent;
import com.evolveum.midpoint.wf.impl.messages.TaskEvent;
import com.evolveum.midpoint.wf.impl.processors.*;
import com.evolveum.midpoint.wf.impl.processors.general.scenarios.DefaultGcpScenarioBean;
import com.evolveum.midpoint.wf.impl.processors.general.scenarios.GcpScenarioBean;
import com.evolveum.midpoint.wf.impl.util.SerializationSafeContainer;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Map;

/**
 * @author mederly
 */
@Component
public class GeneralChangeProcessor extends BaseChangeProcessor {

    private static final Trace LOGGER = TraceManager.getTrace(GeneralChangeProcessor.class);

    @Autowired
    private PrismContext prismContext;

    @Autowired
    private WfTaskUtil wfTaskUtil;

    @Autowired
    private WfTaskController wfTaskController;

    @Autowired
    private ActivitiEngine activitiEngine;

    @Autowired
    private BaseModelInvocationProcessingHelper baseModelInvocationProcessingHelper;

    @Autowired
    private BaseConfigurationHelper baseConfigurationHelper;

    @Autowired
    private BaseAuditHelper baseAuditHelper;

    @Autowired
    private GcpConfigurationHelper gcpConfigurationHelper;

    @Autowired
    private GcpExpressionHelper gcpExpressionHelper;

    @Autowired
    private GcpExternalizationHelper gcpExternalizationHelper;

    //region Initialization and Configuration
    @PostConstruct
    public void init() {
        baseConfigurationHelper.registerProcessor(this);
    }

    private GcpScenarioBean getScenarioBean(Map<String, Object> variables) {
        String beanName = (String) variables.get(GcpProcessVariableNames.VARIABLE_MIDPOINT_SCENARIO_BEAN_NAME);
        return findScenarioBean(beanName);
    }

    public GcpScenarioBean findScenarioBean(String name) {
        if (name == null) {
            name = lowerFirstChar(DefaultGcpScenarioBean.class.getSimpleName());
        }
        if (getBeanFactory().containsBean(name)) {
            return getBeanFactory().getBean(name, GcpScenarioBean.class);
        } else {
            throw new IllegalStateException("Scenario bean " + name + " couldn't be found.");
        }
    }

    private String lowerFirstChar(String name) {
        return Character.toLowerCase(name.charAt(0)) + name.substring(1);
    }

    //endregion

    //region Processing model invocation
    @Override
    public HookOperationMode processModelInvocation(@NotNull ModelContext context, WfConfigurationType wfConfigurationType, @NotNull Task taskFromModel, @NotNull OperationResult result) throws SchemaException {

        if (wfConfigurationType != null && wfConfigurationType.getGeneralChangeProcessor() != null && Boolean.FALSE.equals(wfConfigurationType.getGeneralChangeProcessor().isEnabled())) {
            LOGGER.trace("{} is disabled", getBeanName());
            return null;
        }

        if (wfConfigurationType == null || wfConfigurationType.getGeneralChangeProcessor() == null || wfConfigurationType.getGeneralChangeProcessor().getScenario().isEmpty()) {
            LOGGER.trace("No scenarios for {}", getBeanName());
            return null;
        }

        GeneralChangeProcessorConfigurationType processorConfigurationType = wfConfigurationType.getGeneralChangeProcessor();
        for (GeneralChangeProcessorScenarioType scenarioType : processorConfigurationType.getScenario()) {
            GcpScenarioBean scenarioBean = findScenarioBean(scenarioType.getBeanName());
            if (Boolean.FALSE.equals(scenarioType.isEnabled())) {
                LOGGER.trace("scenario {} is disabled, skipping", scenarioType.getName());
            } else if (!gcpExpressionHelper.evaluateActivationCondition(scenarioType, context, taskFromModel, result)) {
                LOGGER.trace("activationCondition was evaluated to FALSE for scenario named {}", scenarioType.getName());
            } else if (!scenarioBean.determineActivation(scenarioType, context, taskFromModel, result)) {
                LOGGER.trace("scenarioBean decided to skip scenario named {}", scenarioType.getName());
            } else {
                LOGGER.trace("Applying scenario {} (process name {})", scenarioType.getName(), scenarioType.getProcessName());
                return applyScenario(scenarioType, scenarioBean, context, taskFromModel, wfConfigurationType, result);
            }
        }
        LOGGER.trace("No scenario found to be applicable, exiting the change processor.");
        return null;
    }

    private HookOperationMode applyScenario(GeneralChangeProcessorScenarioType scenarioType, GcpScenarioBean scenarioBean, ModelContext context,
			Task taskFromModel, WfConfigurationType wfConfigurationType, OperationResult result) {

        try {
            // ========== preparing root task ===========

            WfTaskCreationInstruction rootInstruction = baseModelInvocationProcessingHelper.createInstructionForRoot(this, context, taskFromModel, result);
            WfTask rootWfTask = baseModelInvocationProcessingHelper.submitRootTask(rootInstruction, taskFromModel, wfConfigurationType, result);

            // ========== preparing child task, starting WF process ===========

            WfTaskCreationInstruction instruction = scenarioBean.prepareJobCreationInstruction(scenarioType, (LensContext<?>) context, rootWfTask, taskFromModel, result);
            wfTaskController.submitWfTask(instruction, rootWfTask, wfConfigurationType, result);

            // ========== complete the action ===========

            baseModelInvocationProcessingHelper.logJobsBeforeStart(rootWfTask, result);
            rootWfTask.startWaitingForSubtasks(result);

            return HookOperationMode.BACKGROUND;

        } catch (SchemaException|ObjectNotFoundException|CommunicationException|ConfigurationException|ObjectAlreadyExistsException|ExpressionEvaluationException|RuntimeException|Error e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Workflow process(es) could not be started", e);
            result.recordFatalError("Workflow process(es) could not be started: " + e, e);
            return HookOperationMode.ERROR;
            // todo rollback - at least close open tasks, maybe stop workflow process instances
        }
    }
    //endregion

    //region Finalizing the processing
    @Override
    public void onProcessEnd(ProcessEvent event, WfTask wfTask, OperationResult result) throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException {

        Task task = wfTask.getTask();
        // we simply put model context back into parent task
        // (or if it is null, we set the task to skip model context processing)

        // it is safe to directly access the parent, because (1) it is in waiting state, (2) we are its only child

        Task rootTask = task.getParentTask(result);

        SerializationSafeContainer<LensContextType> contextContainer = event.getVariable(GcpProcessVariableNames.VARIABLE_MODEL_CONTEXT, SerializationSafeContainer.class);
        LensContextType lensContextType = null;
        if (contextContainer != null) {
            contextContainer.setPrismContext(prismContext);
            lensContextType = contextContainer.getValue();
        }

        if (lensContextType == null) {
            LOGGER.debug(GcpProcessVariableNames.VARIABLE_MODEL_CONTEXT + " not present in process, this means we should stop processing. Task = {}", rootTask);
            wfTaskUtil.storeModelContext(rootTask, (ModelContext) null);
        } else {
            LOGGER.debug("Putting (changed or unchanged) value of {} into the task {}", GcpProcessVariableNames.VARIABLE_MODEL_CONTEXT, rootTask);
            wfTaskUtil.storeModelContext(rootTask, lensContextType);
        }

        rootTask.savePendingModifications(result);
        LOGGER.trace("onProcessEnd ending for task {}", task);
    }
    //endregion

    //region Auditing
    @Override
    public AuditEventRecord prepareProcessInstanceAuditRecord(WfTask wfTask, AuditEventStage stage, Map<String, Object> variables, OperationResult result) {
        return getScenarioBean(variables).prepareProcessInstanceAuditRecord(variables, wfTask, stage, result);
    }

    @Override
    public AuditEventRecord prepareWorkItemCreatedAuditRecord(WorkItemType workItem, TaskEvent taskEvent, WfTask wfTask,
            OperationResult result) throws WorkflowException {
        return getScenarioBean(taskEvent.getVariables()).prepareWorkItemCreatedAuditRecord(workItem, wfTask, taskEvent, result);
    }

    @Override
    public AuditEventRecord prepareWorkItemDeletedAuditRecord(WorkItemType workItem, WorkItemEventCauseInformationType cause,
            TaskEvent taskEvent, WfTask wfTask,
            OperationResult result) throws WorkflowException {
        return getScenarioBean(taskEvent.getVariables())
                .prepareWorkItemDeletedAuditRecord(workItem, cause, taskEvent, wfTask, result);
    }
    //endregion
}

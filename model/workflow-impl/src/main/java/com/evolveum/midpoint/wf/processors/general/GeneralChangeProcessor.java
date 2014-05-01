package com.evolveum.midpoint.wf.processors.general;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.audit.api.AuditEventStage;
import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.api.hooks.HookOperationMode;
import com.evolveum.midpoint.model.lens.LensContext;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.activiti.ActivitiEngine;
import com.evolveum.midpoint.wf.api.WorkflowException;
import com.evolveum.midpoint.wf.jobs.Job;
import com.evolveum.midpoint.wf.jobs.JobController;
import com.evolveum.midpoint.wf.jobs.JobCreationInstruction;
import com.evolveum.midpoint.wf.jobs.WfTaskUtil;
import com.evolveum.midpoint.wf.messages.ProcessEvent;
import com.evolveum.midpoint.wf.messages.TaskEvent;
import com.evolveum.midpoint.wf.processors.BaseAuditHelper;
import com.evolveum.midpoint.wf.processors.BaseChangeProcessor;
import com.evolveum.midpoint.wf.processors.BaseExternalizationHelper;
import com.evolveum.midpoint.wf.processors.BaseModelInvocationProcessingHelper;
import com.evolveum.midpoint.wf.processors.general.scenarios.DefaultGcpScenarioBean;
import com.evolveum.midpoint.wf.processors.general.scenarios.GcpScenarioBean;
import com.evolveum.midpoint.wf.util.JaxbValueContainer;
import com.evolveum.midpoint.wf.util.SerializationSafeContainer;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.GeneralChangeProcessorConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.GeneralChangeProcessorScenarioType;
import com.evolveum.midpoint.xml.ns._public.model.model_context_2.LensContextType;
import com.evolveum.midpoint.xml.ns.model.workflow.common_forms_2.WorkItemContents;
import com.evolveum.midpoint.xml.ns.model.workflow.process_instance_state_2.ProcessInstanceState;
import com.evolveum.midpoint.xml.ns.model.workflow.process_instance_state_2.ProcessSpecificState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.xml.bind.JAXBException;
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
    private JobController jobController;

    @Autowired
    private ActivitiEngine activitiEngine;

    @Autowired
    private BaseModelInvocationProcessingHelper baseModelInvocationProcessingHelper;

    @Autowired
    private BaseExternalizationHelper baseExternalizationHelper;

    @Autowired
    private BaseAuditHelper baseAuditHelper;

    @Autowired
    private GcpConfigurationHelper gcpConfigurationHelper;

    @Autowired
    private GcpExpressionHelper gcpExpressionHelper;

    @Autowired
    private GcpExternalizationHelper gcpExternalizationHelper;

    private GeneralChangeProcessorConfigurationType processorConfigurationType;

    //region Initialization and Configuration
    @PostConstruct
    public void init() {
        processorConfigurationType = gcpConfigurationHelper.configure(this);
        if (isEnabled()) {
            // print startup message
            int scenarios = processorConfigurationType.getScenario().size();
            if (scenarios > 0) {
                LOGGER.info(getBeanName() + " initialized correctly (number of scenarios: " + scenarios + ")");
            } else {
                LOGGER.warn(getBeanName() + " initialized correctly, but there are no scenarios - so it will never be invoked");
            }
        }
    }

    private GeneralChangeProcessorScenarioType findScenario(String scenarioName) {
        for (GeneralChangeProcessorScenarioType scenario : processorConfigurationType.getScenario()) {
            if (scenarioName.equals(scenario.getName())) {
                return scenario;
            }
        }
        throw new SystemException("Scenario named " + scenarioName + " couldn't be found");
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

    public void disableScenario(String scenarioName) {
        findScenario(scenarioName).setEnabled(false);
    }

    public void enableScenario(String scenarioName) {
        findScenario(scenarioName).setEnabled(true);
    }
    //endregion

    //region Processing model invocation
    @Override
    public HookOperationMode processModelInvocation(ModelContext context, Task taskFromModel, OperationResult result) throws SchemaException {

        if (processorConfigurationType.getScenario().isEmpty()) {
            LOGGER.warn("No scenarios for " + getBeanName());
        }

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
                return applyScenario(scenarioType, scenarioBean, context, taskFromModel, result);
            }
        }
        LOGGER.trace("No scenario found to be applicable, exiting the change processor.");
        return HookOperationMode.FOREGROUND;
    }

    private HookOperationMode applyScenario(GeneralChangeProcessorScenarioType scenarioType, GcpScenarioBean scenarioBean, ModelContext context, Task taskFromModel, OperationResult result) {

        try {
            // ========== preparing root task ===========

            JobCreationInstruction rootInstruction = baseModelInvocationProcessingHelper.createInstructionForRoot(this, context, taskFromModel);
            Job rootJob = baseModelInvocationProcessingHelper.createRootJob(rootInstruction, taskFromModel, result);

            // ========== preparing child task, starting WF process ===========

            JobCreationInstruction instruction = scenarioBean.prepareJobCreationInstruction(scenarioType, (LensContext<?>) context, rootJob, taskFromModel, result);
            jobController.createJob(instruction, rootJob, result);

            // ========== complete the action ===========

            baseModelInvocationProcessingHelper.logJobsBeforeStart(rootJob, result);
            rootJob.startWaitingForSubtasks(result);

            return HookOperationMode.BACKGROUND;

        } catch (SchemaException|ObjectNotFoundException|CommunicationException|ConfigurationException|RuntimeException e) {
            LoggingUtils.logException(LOGGER, "Workflow process(es) could not be started", e);
            result.recordFatalError("Workflow process(es) could not be started: " + e, e);
            return HookOperationMode.ERROR;
            // todo rollback - at least close open tasks, maybe stop workflow process instances
        }
    }
    //endregion

    //region Finalizing the processing
    @Override
    public void onProcessEnd(ProcessEvent event, Job job, OperationResult result) throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException {

        Task task = job.getTask();
        // we simply put model context back into parent task
        // (or if it is null, we set the task to skip model context processing)

        // it is safe to directly access the parent, because (1) it is in waiting state, (2) we are its only child

        Task rootTask = task.getParentTask(result);

        SerializationSafeContainer<LensContextType> contextContainer = (SerializationSafeContainer<LensContextType>) event.getVariable(GcpProcessVariableNames.VARIABLE_MODEL_CONTEXT);
        LensContextType lensContextType = null;
        if (contextContainer != null) {
            contextContainer.setPrismContext(prismContext);
            lensContextType = contextContainer.getValue();
        }

        if (lensContextType == null) {
            LOGGER.debug(GcpProcessVariableNames.VARIABLE_MODEL_CONTEXT + " not present in process, this means we should stop processing. Task = {}", rootTask);
            wfTaskUtil.setSkipModelContextProcessingProperty(rootTask, true, result);
        } else {
            LOGGER.debug("Putting (changed or unchanged) value of {} into the task {}", GcpProcessVariableNames.VARIABLE_MODEL_CONTEXT, rootTask);
            wfTaskUtil.storeModelContext(rootTask, lensContextType.asPrismContainerValue().getContainer());
        }

        rootTask.savePendingModifications(result);
        LOGGER.trace("onProcessEnd ending for task {}", task);
    }
    //endregion

    //region Externalization methods (including auditing)
    @Override
    public PrismObject<? extends WorkItemContents> externalizeWorkItemContents(org.activiti.engine.task.Task task, Map<String, Object> processInstanceVariables, OperationResult result) throws JAXBException, ObjectNotFoundException, SchemaException {
        return getScenarioBean(processInstanceVariables).externalizeWorkItemContents(task, processInstanceVariables, result);
    }

    @Override
    public PrismObject<? extends ProcessInstanceState> externalizeProcessInstanceState(Map<String, Object> variables) throws JAXBException, SchemaException {
        PrismObject<ProcessInstanceState> state = baseExternalizationHelper.externalizeState(variables);
        ProcessSpecificState processSpecificState = getScenarioBean(variables).externalizeInstanceState(variables);
        state.asObjectable().setProcessSpecificState(processSpecificState);
        return state;
    }

    @Override
    public AuditEventRecord prepareProcessInstanceAuditRecord(Map<String, Object> variables, Job job, AuditEventStage stage, OperationResult result) {
        return getScenarioBean(variables).prepareProcessInstanceAuditRecord(variables, job, stage, result);
    }

    @Override
    public AuditEventRecord prepareWorkItemAuditRecord(TaskEvent taskEvent, AuditEventStage stage, OperationResult result) throws WorkflowException {
        return getScenarioBean(taskEvent.getVariables()).prepareWorkItemAuditRecord(taskEvent, stage, result);
    }
    //endregion
}

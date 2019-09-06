/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
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
import com.evolveum.midpoint.wf.impl.engine.EngineInvocationContext;
import com.evolveum.midpoint.wf.impl.processors.StartInstruction;
import com.evolveum.midpoint.wf.impl.processors.*;
import com.evolveum.midpoint.wf.impl.processors.general.scenarios.DefaultGcpScenarioBean;
import com.evolveum.midpoint.wf.impl.processors.general.scenarios.GcpScenarioBean;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * @author mederly
 */
@Component
public class GeneralChangeProcessor extends BaseChangeProcessor {

    private static final Trace LOGGER = TraceManager.getTrace(GeneralChangeProcessor.class);

    @Autowired private PrismContext prismContext;
    @Autowired private ModelHelper modelHelper;
    @Autowired private ConfigurationHelper configurationHelper;
    @Autowired private GcpConfigurationHelper gcpConfigurationHelper;
    @Autowired private GcpExpressionHelper gcpExpressionHelper;
    @Autowired private GcpExternalizationHelper gcpExternalizationHelper;

    //region Initialization and Configuration
    @PostConstruct
    public void init() {
        configurationHelper.registerProcessor(this);
    }

    private GcpScenarioBean getScenarioBean(ApprovalContextType wfContext) {
        //String beanName = ((WfGeneralChangeProcessorStateType) wfContext.getProcessorSpecificState()).getScenario();
        //return findScenarioBean(beanName);
        throw new UnsupportedOperationException("TODO");
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
    public HookOperationMode processModelInvocation(@NotNull ModelInvocationContext<?> ctx, @NotNull OperationResult result) throws SchemaException {

        if (ctx.wfConfiguration != null && ctx.wfConfiguration.getGeneralChangeProcessor() != null &&
                Boolean.FALSE.equals(ctx.wfConfiguration.getGeneralChangeProcessor().isEnabled())) {
            LOGGER.trace("{} is disabled", getBeanName());
            return null;
        }

        if (ctx.wfConfiguration == null || ctx.wfConfiguration.getGeneralChangeProcessor() == null ||
                ctx.wfConfiguration.getGeneralChangeProcessor().getScenario().isEmpty()) {
            LOGGER.trace("No scenarios for {}", getBeanName());
            return null;
        }

        GeneralChangeProcessorConfigurationType processorConfigurationType = ctx.wfConfiguration.getGeneralChangeProcessor();
        for (GeneralChangeProcessorScenarioType scenarioType : processorConfigurationType.getScenario()) {
            GcpScenarioBean scenarioBean = findScenarioBean(scenarioType.getBeanName());
            if (Boolean.FALSE.equals(scenarioType.isEnabled())) {
                LOGGER.trace("scenario {} is disabled, skipping", scenarioType.getName());
            } else if (!gcpExpressionHelper.evaluateActivationCondition(scenarioType, ctx.modelContext, ctx.task, result)) {
                LOGGER.trace("activationCondition was evaluated to FALSE for scenario named {}", scenarioType.getName());
            } else if (!scenarioBean.determineActivation(scenarioType, ctx.modelContext, ctx.task, result)) {
                LOGGER.trace("scenarioBean decided to skip scenario named {}", scenarioType.getName());
            } else {
                LOGGER.trace("Applying scenario {} (process name {})", scenarioType.getName(), scenarioType.getProcessName());
                return applyScenario(scenarioType, scenarioBean, ctx, result);
            }
        }
        LOGGER.trace("No scenario found to be applicable, exiting the change processor.");
        return null;
    }

    private HookOperationMode applyScenario(GeneralChangeProcessorScenarioType scenarioType, GcpScenarioBean scenarioBean,
            ModelInvocationContext ctx, OperationResult result) {

        try {
            // ========== preparing root task ===========

            StartInstruction rootInstruction = modelHelper
                    .createInstructionForRoot(this, ctx, ctx.modelContext, result);
            CaseType rootWfCase = modelHelper.addRoot(rootInstruction, ctx.task, result);

            // ========== preparing child task, starting WF process ===========

            StartInstruction instruction = scenarioBean.prepareJobCreationInstruction(scenarioType, (LensContext<?>) ctx.modelContext, rootWfCase, ctx.task, result);
            instruction.setParent(rootWfCase);
            modelHelper.addCase(instruction, ctx.task, result);

            // ========== complete the action ===========

            modelHelper.logJobsBeforeStart(rootWfCase, ctx.task, result);
            if (true) {
                throw new IllegalStateException("Call workflow engine here");   // TODO
            }

            return HookOperationMode.BACKGROUND;

        } catch (SchemaException | ObjectNotFoundException | CommunicationException | ConfigurationException | ObjectAlreadyExistsException | ExpressionEvaluationException | RuntimeException | Error e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Workflow process(es) could not be started", e);
            result.recordFatalError("Workflow process(es) could not be started: " + e, e);
            return HookOperationMode.ERROR;
            // todo rollback - at least close open tasks, maybe stop workflow process instances
        }
    }
    //endregion

    //region Finalizing the processing
    @Override
    public void onProcessEnd(EngineInvocationContext ctx, OperationResult result) throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException {

        Task task = ctx.getTask();
        // we simply put model context back into parent task
        // (or if it is null, we set the task to skip model context processing)

        // it is safe to directly access the parent, because (1) it is in waiting state, (2) we are its only child

        Task rootTask = task.getParentTask(result);
        throw new UnsupportedOperationException("TODO");

//        WfGeneralChangeProcessorStateType processorSpecificState = (WfGeneralChangeProcessorStateType) ctx.getWfContext().getProcessorSpecificState();
//        LensContextType lensContextType = processorSpecificState != null ? processorSpecificState.getModelContext() : null;
//        if (lensContextType == null) {
//            LOGGER.debug(GcpProcessVariableNames.VARIABLE_MODEL_CONTEXT + " not present in process, this means we should stop processing. Task = {}", rootTask);
//            storeModelContext(rootTask, null, false);
//        } else {
//            LOGGER.debug("Putting (changed or unchanged) value of model context into the task {}", rootTask);
//            storeModelContext(rootTask, lensContextType);
//        }
//        rootTask.flushPendingModifications(result);
//        LOGGER.trace("onProcessEnd ending for task {}", task);
    }
    //endregion

    //region Auditing
    @Override
    public AuditEventRecord prepareProcessInstanceAuditRecord(CaseType aCase, AuditEventStage stage, ApprovalContextType wfContext, OperationResult result) {
        return getScenarioBean(wfContext).prepareProcessInstanceAuditRecord(wfContext, aCase, stage, result);
    }

    @Override
    public AuditEventRecord prepareWorkItemCreatedAuditRecord(CaseWorkItemType workItem, CaseType aCase,
            OperationResult result) {
        return getScenarioBean(aCase.getApprovalContext()).prepareWorkItemCreatedAuditRecord(workItem, aCase, result);
    }

    @Override
    public AuditEventRecord prepareWorkItemDeletedAuditRecord(CaseWorkItemType workItem, WorkItemEventCauseInformationType cause,
            CaseType aCase,
            OperationResult result) {
        return getScenarioBean(aCase.getApprovalContext())
                .prepareWorkItemDeletedAuditRecord(workItem, cause, aCase, result);
    }
    //endregion

    public void storeModelContext(Task task, ModelContext context, boolean reduced) throws SchemaException {
        LensContextType modelContext = context != null ? ((LensContext) context).toLensContextType(reduced ? LensContext.ExportType.REDUCED : LensContext.ExportType.OPERATIONAL) : null;
        storeModelContext(task, modelContext);
    }

    public void storeModelContext(Task task, LensContextType context) throws SchemaException {
        task.setModelOperationContext(context);
    }

}

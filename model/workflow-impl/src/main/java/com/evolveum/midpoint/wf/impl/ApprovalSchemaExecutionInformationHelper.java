/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.wf.impl;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.cases.api.temporary.ComputationMode;
import com.evolveum.midpoint.cases.impl.helpers.CaseMiscHelper;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.cases.ApprovalContextUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.impl.processes.common.StageComputeHelper;
import com.evolveum.midpoint.wf.impl.processors.ConfigurationHelper;
import com.evolveum.midpoint.wf.impl.processors.ModelInvocationContext;
import com.evolveum.midpoint.wf.impl.processors.primary.PcpStartInstruction;
import com.evolveum.midpoint.wf.impl.processors.primary.PrimaryChangeProcessor;
import com.evolveum.midpoint.wf.impl.util.ComputationResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

@Component
public class ApprovalSchemaExecutionInformationHelper {

    private static final Trace LOGGER = TraceManager.getTrace(ApprovalSchemaExecutionInformationHelper.class);

    @Autowired private ModelService modelService;
    @Autowired private StageComputeHelper computeHelper;
    @Autowired private CaseMiscHelper caseMiscHelper;
    @Autowired private PrimaryChangeProcessor primaryChangeProcessor;
    @Autowired private ConfigurationHelper configurationHelper;
    @Autowired
    @Qualifier("cacheRepositoryService")
    private RepositoryService repositoryService;

    ApprovalSchemaExecutionInformationType getApprovalSchemaExecutionInformation(String caseOid, Task opTask,
            OperationResult result)
            throws CommunicationException, ObjectNotFoundException, SchemaException, SecurityViolationException,
            ConfigurationException, ExpressionEvaluationException {
        CaseType aCase = modelService.getObject(CaseType.class, caseOid, null, opTask, result).asObjectable();
        return getApprovalSchemaExecutionInformation(aCase, false, opTask, result);
    }

    List<ApprovalSchemaExecutionInformationType> getApprovalSchemaPreview(ModelContext<?> modelContext, Task opTask,
            OperationResult result) throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, SecurityViolationException {
        WfConfigurationType wfConfiguration = configurationHelper.getWorkflowConfiguration(modelContext, result);
        ModelInvocationContext<?> ctx = new ModelInvocationContext<>(modelContext, wfConfiguration, repositoryService, opTask);
        List<PcpStartInstruction> taskInstructions = primaryChangeProcessor.previewModelInvocation(ctx, result);
        List<ApprovalSchemaExecutionInformationType> rv = new ArrayList<>();
        for (PcpStartInstruction taskInstruction : taskInstructions) {
            OperationResult childResult = result.createMinorSubresult(ApprovalSchemaExecutionInformationHelper.class + ".getApprovalSchemaPreview");
            try {
                CaseType aCase = taskInstruction.getCase();
                rv.add(getApprovalSchemaExecutionInformation(aCase, true, opTask, childResult));
                childResult.computeStatus();
            } catch (Throwable t) {
                childResult.recordFatalError("Couldn't preview approval schema for " + taskInstruction, t);
            }
        }
        return rv;
    }

    @NotNull
    private ApprovalSchemaExecutionInformationType getApprovalSchemaExecutionInformation(CaseType aCase, boolean purePreview, Task opTask,
            OperationResult result) {
        ApprovalSchemaExecutionInformationType rv = new ApprovalSchemaExecutionInformationType();
        rv.setCaseRef(ObjectTypeUtil.createObjectRefWithFullObject(aCase));
        ApprovalContextType wfc = aCase.getApprovalContext();
        if (wfc == null) {
            result.recordFatalError("Workflow context in " + aCase + " is missing or not accessible.");
            return rv;
        }
        ApprovalSchemaType approvalSchema = wfc.getApprovalSchema();
        if (approvalSchema == null) {
            result.recordFatalError("Approval schema in " + aCase + " is missing or not accessible.");
            return rv;
        }
        rv.setCurrentStageNumber(aCase.getStageNumber());
        int currentStageNumber;
        if (purePreview) {
            currentStageNumber = 0;
        } else {
            if (aCase.getStageNumber() != null) {
                currentStageNumber = aCase.getStageNumber();
            } else {
                result.recordFatalError("Information on current stage number in " + aCase + " is missing or not accessible.");
                return rv;
            }
        }
        List<ApprovalStageDefinitionType> stagesDef = ApprovalContextUtil.sortAndCheckStages(approvalSchema);
        for (ApprovalStageDefinitionType stageDef : stagesDef) {
            ApprovalStageExecutionInformationType stageExecution = new ApprovalStageExecutionInformationType();
            stageExecution.setNumber(stageDef.getNumber());
            stageExecution.setDefinition(stageDef);
            if (stageDef.getNumber() > currentStageNumber) {
                stageExecution.setExecutionPreview(createStageExecutionPreview(aCase, opTask.getChannel(), stageDef, opTask, result));
            }
            rv.getStage().add(stageExecution);
        }
        if (wfc.getPolicyRules() != null) {
            rv.setPolicyRules(wfc.getPolicyRules().clone());
        }
        return rv;
    }

    private ApprovalStageExecutionPreviewType createStageExecutionPreview(CaseType aCase, String requestChannel,
            ApprovalStageDefinitionType stageDef, Task opTask, OperationResult result) {
        ApprovalStageExecutionPreviewType rv = new ApprovalStageExecutionPreviewType();
        try {
            ComputationResult computationResult = computeHelper
                    .computeStageApprovers(stageDef, aCase,
                            () -> caseMiscHelper.getDefaultVariables(aCase, aCase.getApprovalContext(), requestChannel, result),
                            ComputationMode.PREVIEW, opTask, result);
            rv.getExpectedApproverRef().addAll(computationResult.getApproverRefs());
            rv.setExpectedAutomatedOutcome(computationResult.getPredeterminedOutcome());
            rv.setExpectedAutomatedCompletionReason(computationResult.getAutomatedCompletionReason());
        } catch (Throwable t) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't compute stage execution preview", t);
            rv.setErrorMessage(MiscUtil.formatExceptionMessageWithCause(t));
            rv.getExpectedApproverRef().addAll(CloneUtil.cloneCollectionMembers(stageDef.getApproverRef())); // at least something here
        }
        return rv;
    }
}

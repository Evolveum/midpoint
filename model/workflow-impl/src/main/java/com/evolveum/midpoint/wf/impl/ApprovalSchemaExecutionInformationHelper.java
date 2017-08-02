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

package com.evolveum.midpoint.wf.impl;

import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.WfContextUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.impl.processes.common.WfStageComputeHelper;
import com.evolveum.midpoint.wf.impl.processors.BaseConfigurationHelper;
import com.evolveum.midpoint.wf.impl.processors.primary.PcpChildWfTaskCreationInstruction;
import com.evolveum.midpoint.wf.impl.processors.primary.PrimaryChangeProcessor;
import com.evolveum.midpoint.wf.impl.tasks.WfTaskController;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author mederly
 */
@Component
public class ApprovalSchemaExecutionInformationHelper {

	private static final Trace LOGGER = TraceManager.getTrace(ApprovalSchemaExecutionInformationHelper.class);

	@Autowired private ModelService modelService;
	@Autowired private PrismContext prismContext;
	@Autowired private WfStageComputeHelper computeHelper;
	@Autowired private PrimaryChangeProcessor primaryChangeProcessor;
	@Autowired private BaseConfigurationHelper baseConfigurationHelper;
	@Autowired private WfTaskController wfTaskController;

	ApprovalSchemaExecutionInformationType getApprovalSchemaExecutionInformation(String taskOid, Task opTask,
			OperationResult result)
			throws CommunicationException, ObjectNotFoundException, SchemaException, SecurityViolationException,
			ConfigurationException, ExpressionEvaluationException {
		Collection<SelectorOptions<GetOperationOptions>> options = GetOperationOptions
				.retrieveItemsNamed(new ItemPath(TaskType.F_WORKFLOW_CONTEXT, WfContextType.F_WORK_ITEM));
		TaskType wfTask = modelService.getObject(TaskType.class, taskOid, options, opTask, result).asObjectable();
		return getApprovalSchemaExecutionInformation(wfTask, false, opTask, result);
	}

	List<ApprovalSchemaExecutionInformationType> getApprovalSchemaPreview(ModelContext<?> modelContext, Task opTask,
			OperationResult result) throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException {
		WfConfigurationType wfConfiguration = baseConfigurationHelper.getWorkflowConfiguration(modelContext, result);
		List<PcpChildWfTaskCreationInstruction> taskInstructions = primaryChangeProcessor.previewModelInvocation(modelContext, wfConfiguration, opTask, result);
		List<ApprovalSchemaExecutionInformationType> rv = new ArrayList<>();
		for (PcpChildWfTaskCreationInstruction taskInstruction : taskInstructions) {
			OperationResult childResult = result.createMinorSubresult(ApprovalSchemaExecutionInformationHelper.class + ".getApprovalSchemaPreview");
			try {
				Task wfTask = taskInstruction.createTask(wfTaskController, opTask, wfConfiguration);
				TaskType wfTaskBean = wfTask.getTaskPrismObject().asObjectable();
				rv.add(getApprovalSchemaExecutionInformation(wfTaskBean, true, opTask, childResult));
				childResult.computeStatus();
			} catch (Throwable t) {
				childResult.recordFatalError("Couldn't preview approval schema for " + taskInstruction.getProcessName(), t);
			}
		}
		return rv;
	}

	@NotNull
	private ApprovalSchemaExecutionInformationType getApprovalSchemaExecutionInformation(TaskType wfTask, boolean purePreview, Task opTask,
			OperationResult result) {
		ApprovalSchemaExecutionInformationType rv = new ApprovalSchemaExecutionInformationType(prismContext);
		rv.setTaskRef(ObjectTypeUtil.createObjectRefWithFullObject(wfTask));
		WfContextType wfc = wfTask.getWorkflowContext();
		if (wfc == null) {
			result.recordFatalError("Workflow context in " + wfTask + " is missing or not accessible.");
			return rv;
		}
		WfProcessSpecificStateType processSpecificState = wfc.getProcessSpecificState();
		if (processSpecificState == null) {
			result.recordFatalError("Approval process state in " + wfTask + " is missing or not accessible.");
			return rv;
		}
		if (!(processSpecificState instanceof ItemApprovalProcessStateType)) {
			result.recordFatalError("Task " + wfTask + " does not correspond to ItemApproval process: "
					+ "its process specific state is " + processSpecificState.getClass());
			return rv;
		}
		ItemApprovalProcessStateType itemApprovalState = (ItemApprovalProcessStateType) processSpecificState;
		ApprovalSchemaType approvalSchema = itemApprovalState.getApprovalSchema();
		if (approvalSchema == null) {
			result.recordFatalError("Approval schema in " + wfTask + " is missing or not accessible.");
			return rv;
		}
		Integer stageNumber = !purePreview ? wfc.getStageNumber() : 0;
		if (stageNumber == null) {
			result.recordFatalError("Information on current stage number in " + wfTask + " is missing or not accessible.");
			return rv;
		}
		List<ApprovalStageDefinitionType> stagesDef = WfContextUtil.sortAndCheckStages(approvalSchema);
		for (ApprovalStageDefinitionType stageDef : stagesDef) {
			ApprovalStageExecutionInformationType stageExecution = new ApprovalStageExecutionInformationType(prismContext);
			stageExecution.setNumber(stageDef.getNumber());
			stageExecution.setDefinition(stageDef);
			if (stageDef.getNumber() <= stageNumber) {
				stageExecution.setExecutionRecord(createStageExecutionRecord(wfc, stageNumber));
			} else {
				stageExecution.setExecutionPreview(createStageExecutionPreview(wfc, wfTask.getChannel(), stageDef, opTask, result));
			}
			rv.getStage().add(stageExecution);
		}
		return rv;
	}

	private ApprovalStageExecutionPreviewType createStageExecutionPreview(WfContextType wfc, String requestChannel,
			ApprovalStageDefinitionType stageDef, Task opTask, OperationResult result) {
		ApprovalStageExecutionPreviewType rv = new ApprovalStageExecutionPreviewType(prismContext);
		try {
			WfStageComputeHelper.ComputationResult computationResult = computeHelper
					.computeStageApprovers(stageDef, () -> computeHelper.getDefaultVariables(wfc, requestChannel, result), opTask, result);
			rv.getExpectedApproverRef().addAll(computationResult.getApproverRefs());
			rv.setExpectedAutomatedOutcome(computationResult.getPredeterminedOutcome());
			rv.setExpectedAutomatedCompletionReason(computationResult.getAutomatedCompletionReason());
		} catch (Throwable t) {
			LoggingUtils.logUnexpectedException(LOGGER, "Couldn't compute stage execution preview", t);
			rv.setErrorMessage(MiscUtil.formatExceptionMessageWithCause(t));
			rv.getExpectedApproverRef().addAll(CloneUtil.cloneCollectionMembers(stageDef.getApproverRef()));      // at least something here
		}
		return rv;
	}

	private ApprovalStageExecutionRecordType createStageExecutionRecord(WfContextType wfc,
			int stageNumber) {
		ApprovalStageExecutionRecordType rv = new ApprovalStageExecutionRecordType(prismContext);
		wfc.getEvent().stream()
				.filter(e -> e.getStageNumber() != null && e.getStageNumber() == stageNumber)
				.forEach(e -> rv.getEvent().add(e));
		rv.getWorkItem().addAll(CloneUtil.cloneCollectionMembers(wfc.getWorkItem()));
		return rv;
	}
}

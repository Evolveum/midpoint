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

package com.evolveum.midpoint.web.component.wf;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.page.admin.server.TaskWfChildPanel;
import com.evolveum.midpoint.web.page.admin.workflow.dto.ApprovalProcessExecutionInformationDto;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ApprovalSchemaExecutionInformationType;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxFallbackLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;

/**
 * @author mederly
 */
public class SwitchableApprovalProcessPreviewsPanel extends BasePanel<String> {       // model is task OID

	private static final Trace LOGGER = TraceManager.getTrace(SwitchableApprovalProcessPreviewsPanel.class);

	private static final String ID_NEXT_STAGES_CONTAINER = "nextStagesContainer";
	private static final String ID_NEXT_STAGES = "nextStages";
	private static final String ID_NEXT_STAGES_HELP = "nextStagesHelp";
	private static final String ID_WHOLE_PROCESS_CONTAINER = "wholeProcessContainer";
	private static final String ID_WHOLE_PROCESS = "wholeProcess";
	private static final String ID_WHOLE_PROCESS_HELP = "wholeProcessHelp";
	private static final String ID_SHOW_NEXT_STAGES_CONTAINER = "showNextStagesContainer";
	private static final String ID_SHOW_NEXT_STAGES = "showNextStages";
	private static final String ID_SHOW_NEXT_STAGES_HELP = "showNextStagesHelp";
	private static final String ID_SHOW_WHOLE_PROCESS_CONTAINER = "showWholeProcessContainer";
	private static final String ID_SHOW_WHOLE_PROCESS = "showWholeProcess";
	private static final String ID_SHOW_WHOLE_PROCESS_HELP = "showWholeProcessHelp";

	private LoadableModel<ApprovalSchemaExecutionInformationType> approvalExecutionInfoModel;
	private LoadableModel<ApprovalProcessExecutionInformationDto> nextStagesModel;
	private LoadableModel<ApprovalProcessExecutionInformationDto> wholeProcessModel;

	public SwitchableApprovalProcessPreviewsPanel(String id,
			IModel<String> taskOidModel, IModel<Boolean> showNextStagesModel, PageBase parentPage) {
		super(id, taskOidModel);
		initModels(parentPage);
		initLayout(showNextStagesModel);
	}

	private enum ProcessInfoBox {
		NEXT_STAGES, WHOLE_PROCESS
	}
	private ProcessInfoBox displayedProcessInfoBox;

	private void initModels(PageBase parentPage) {
		approvalExecutionInfoModel = new LoadableModel<ApprovalSchemaExecutionInformationType>() {
			@Override
			protected ApprovalSchemaExecutionInformationType load() {
				String taskOid = getModelObject();
				Task opTask = parentPage.createSimpleTask(TaskWfChildPanel.class.getName() + ".loadApprovalExecutionModel");
				OperationResult result = opTask.getResult();
				ApprovalSchemaExecutionInformationType rv = null;
				try {
					rv = parentPage.getWorkflowManager().getApprovalSchemaExecutionInformation(taskOid, opTask, result);
					parentPage.getModelObjectResolver().resolveAllReferences(Collections.singleton(rv.asPrismContainerValue()), opTask, result);
					result.computeStatus();
				} catch (Throwable t) {
					LoggingUtils.logUnexpectedException(LOGGER, "Couldn't get approval schema execution information for {}", t, getModelObject());
					opTask.getResult().recordFatalError("Couldn't get approval schema execution information: " + t.getMessage(), t);
				}
				if (WebComponentUtil.showResultInPage(result)) {
					parentPage.showResult(result);
				}
				return rv;
			}
		};
		nextStagesModel = new LoadableModel<ApprovalProcessExecutionInformationDto>() {
			@Override
			protected ApprovalProcessExecutionInformationDto load() {
				return createApprovalProcessExecutionInformationDto(parentPage, false);
			}
		};
		wholeProcessModel = new LoadableModel<ApprovalProcessExecutionInformationDto>() {
			@Override
			protected ApprovalProcessExecutionInformationDto load() {
				return createApprovalProcessExecutionInformationDto(parentPage, true);
			}
		};
	}

	@Nullable
	private ApprovalProcessExecutionInformationDto createApprovalProcessExecutionInformationDto(
			PageBase parentPage, boolean wholeProcess) {
		Task opTask = parentPage.createSimpleTask(TaskWfChildPanel.class.getName() + ".createApprovalProcessExecutionInformationDto");
		OperationResult result = opTask.getResult();
		ApprovalSchemaExecutionInformationType info = approvalExecutionInfoModel.getObject();
		ApprovalProcessExecutionInformationDto rv = null;
		try {
			if (info != null) {
				rv = ApprovalProcessExecutionInformationDto
						.createFrom(info, parentPage.getModelObjectResolver(), wholeProcess, opTask, result);
			}
			result.computeStatus();
		} catch (Throwable t) {
			LoggingUtils.logUnexpectedException(LOGGER, "Couldn't create approval process execution information for {}", t, getModelObject());
			opTask.getResult().recordFatalError("Couldn't create approval process execution information: " + t.getMessage(), t);
		}
		if (WebComponentUtil.showResultInPage(result)) {
			;
		}
		return rv;
	}

	private void initLayout(IModel<Boolean> showNextStagesModel) {
		setOutputMarkupId(true);

		WebMarkupContainer nextStagesContainer = new WebMarkupContainer(ID_NEXT_STAGES_CONTAINER);
		nextStagesContainer.add(new ApprovalProcessExecutionInformationPanel(ID_NEXT_STAGES, nextStagesModel));
		nextStagesContainer.add(WebComponentUtil.createHelp(ID_NEXT_STAGES_HELP));
		nextStagesContainer.add(new VisibleBehaviour(() -> displayedProcessInfoBox == ProcessInfoBox.NEXT_STAGES));
		add(nextStagesContainer);

		WebMarkupContainer wholeProcessContainer = new WebMarkupContainer(ID_WHOLE_PROCESS_CONTAINER);
		wholeProcessContainer.add(new ApprovalProcessExecutionInformationPanel(ID_WHOLE_PROCESS, wholeProcessModel));
		wholeProcessContainer.add(WebComponentUtil.createHelp(ID_WHOLE_PROCESS_HELP));
		wholeProcessContainer.add(new VisibleBehaviour(() -> displayedProcessInfoBox == ProcessInfoBox.WHOLE_PROCESS));
		add(wholeProcessContainer);

		WebMarkupContainer showNextStagesContainer = new WebMarkupContainer(ID_SHOW_NEXT_STAGES_CONTAINER);
		showNextStagesContainer.add(new AjaxFallbackLink(ID_SHOW_NEXT_STAGES) {
			public void onClick(AjaxRequestTarget target) {
				displayedProcessInfoBox = ProcessInfoBox.NEXT_STAGES;
				target.add(SwitchableApprovalProcessPreviewsPanel.this);
			}
		});
		showNextStagesContainer.add(WebComponentUtil.createHelp(ID_SHOW_NEXT_STAGES_HELP));
		showNextStagesContainer.add(new VisibleBehaviour(() ->
				Boolean.TRUE.equals(showNextStagesModel.getObject()) && displayedProcessInfoBox != ProcessInfoBox.NEXT_STAGES));
		add(showNextStagesContainer);

		WebMarkupContainer showWholeProcessContainer = new WebMarkupContainer(ID_SHOW_WHOLE_PROCESS_CONTAINER);
		showWholeProcessContainer.add(new AjaxFallbackLink(ID_SHOW_WHOLE_PROCESS) {
			public void onClick(AjaxRequestTarget target) {
				displayedProcessInfoBox = ProcessInfoBox.WHOLE_PROCESS;
				target.add(SwitchableApprovalProcessPreviewsPanel.this);
			}
		});
		showWholeProcessContainer.add(new VisibleBehaviour(() -> displayedProcessInfoBox != ProcessInfoBox.WHOLE_PROCESS));
		showWholeProcessContainer.add(WebComponentUtil.createHelp(ID_SHOW_WHOLE_PROCESS_HELP));
		add(showWholeProcessContainer);

	}

}

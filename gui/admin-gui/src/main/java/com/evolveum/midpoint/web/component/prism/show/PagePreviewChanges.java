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

package com.evolveum.midpoint.web.component.prism.show;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.model.api.ModelInteractionService;
import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.api.context.ModelProjectionContext;
import com.evolveum.midpoint.model.api.visualizer.Scene;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectResolver;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.breadcrumbs.Breadcrumb;
import com.evolveum.midpoint.web.component.breadcrumbs.BreadcrumbPageInstance;
import com.evolveum.midpoint.web.component.util.EnableBehaviour;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.component.wf.ApprovalProcessesPreviewPanel;
import com.evolveum.midpoint.web.page.admin.PageAdmin;
import com.evolveum.midpoint.web.page.admin.PageAdminObjectDetails;
import com.evolveum.midpoint.web.page.admin.workflow.EvaluatedTriggerGroupListPanel;
import com.evolveum.midpoint.web.page.admin.workflow.dto.ApprovalProcessExecutionInformationDto;
import com.evolveum.midpoint.web.page.admin.workflow.dto.EvaluatedTriggerGroupDto;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ApprovalSchemaExecutionInformationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyRuleEnforcerHookPreviewOutputType;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.apache.commons.collections.CollectionUtils.addIgnoreNull;

/**
 * @author mederly
 */
@PageDescriptor(url = "/admin/previewChanges", encoder = OnePageParameterEncoder.class)
public class PagePreviewChanges extends PageAdmin {

	private static final String ID_PRIMARY_DELTAS_SCENE = "primaryDeltas";
	private static final String ID_SECONDARY_DELTAS_SCENE = "secondaryDeltas";
	private static final String ID_APPROVALS_CONTAINER = "approvalsContainer";
	private static final String ID_APPROVALS = "approvals";
	private static final String ID_POLICY_VIOLATIONS_CONTAINER = "policyViolationsContainer";
	private static final String ID_POLICY_VIOLATIONS = "policyViolations";
	private static final String ID_CONTINUE_EDITING = "continueEditing";
	private static final String ID_SAVE = "save";

	private static final Trace LOGGER = TraceManager.getTrace(PagePreviewChanges.class);

	private IModel<SceneDto> primaryDeltasModel;
	private IModel<SceneDto> secondaryDeltasModel;
	private IModel<List<EvaluatedTriggerGroupDto>> policyViolationsModel;
	private IModel<List<ApprovalProcessExecutionInformationDto>> approvalsModel;

	public PagePreviewChanges(ModelContext<? extends ObjectType> modelContext, ModelInteractionService modelInteractionService) {
		final List<ObjectDelta<? extends ObjectType>> primaryDeltas = new ArrayList<>();
		final List<ObjectDelta<? extends ObjectType>> secondaryDeltas = new ArrayList<>();
		final List<? extends Scene> primaryScenes;
		final List<? extends Scene> secondaryScenes;
		try {
			if (modelContext != null) {
				if (modelContext.getFocusContext() != null) {
					addIgnoreNull(primaryDeltas, modelContext.getFocusContext().getPrimaryDelta());
					addIgnoreNull(secondaryDeltas, modelContext.getFocusContext().getSecondaryDelta());
				}
				for (ModelProjectionContext projCtx : modelContext.getProjectionContexts()) {
					addIgnoreNull(primaryDeltas, projCtx.getPrimaryDelta());
					addIgnoreNull(secondaryDeltas, projCtx.getExecutableDelta());
				}
			}
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Primary deltas:\n{}", DebugUtil.debugDump(primaryDeltas));
				LOGGER.trace("Secondary deltas:\n{}", DebugUtil.debugDump(secondaryDeltas));
			}

			Task task = createSimpleTask("visualize");
			primaryScenes = modelInteractionService.visualizeDeltas(primaryDeltas, task, task.getResult());
			secondaryScenes = modelInteractionService.visualizeDeltas(secondaryDeltas, task, task.getResult());
		} catch (SchemaException | ExpressionEvaluationException e) {
			throw new SystemException(e);		// TODO
		}
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Creating context DTO for primary deltas:\n{}", DebugUtil.debugDump(primaryScenes));
			LOGGER.trace("Creating context DTO for secondary deltas:\n{}", DebugUtil.debugDump(secondaryScenes));
		}

		final WrapperScene primaryScene = new WrapperScene(primaryScenes,
				primaryScenes.size() != 1 ? "PagePreviewChanges.primaryChangesMore" : "PagePreviewChanges.primaryChangesOne", primaryScenes.size());
		final WrapperScene secondaryScene = new WrapperScene(secondaryScenes,
				secondaryScenes.size() != 1 ? "PagePreviewChanges.secondaryChangesMore" : "PagePreviewChanges.secondaryChangesOne", secondaryScenes.size());
		final SceneDto primarySceneDto = new SceneDto(primaryScene);
		final SceneDto secondarySceneDto = new SceneDto(secondaryScene);
		primaryDeltasModel = new AbstractReadOnlyModel<SceneDto>() {
			@Override
			public SceneDto getObject() {
				return primarySceneDto;
			}
		};
		secondaryDeltasModel = new AbstractReadOnlyModel<SceneDto>() {
			@Override
			public SceneDto getObject() {
				return secondarySceneDto;
			}
		};

		PolicyRuleEnforcerHookPreviewOutputType enforcements = modelContext != null
				? modelContext.getHookPreviewResult(PolicyRuleEnforcerHookPreviewOutputType.class)
				: null;
		List<EvaluatedTriggerGroupDto> triggerGroups = enforcements != null
				? Collections.singletonList(EvaluatedTriggerGroupDto.initializeFromRules(enforcements.getRule(), false, null))
				: Collections.emptyList();
		policyViolationsModel = Model.ofList(triggerGroups);

		List<ApprovalSchemaExecutionInformationType> approvalsExecutionList = modelContext != null
				? modelContext.getHookPreviewResults(ApprovalSchemaExecutionInformationType.class)
				: Collections.emptyList();
		List<ApprovalProcessExecutionInformationDto> approvals = new ArrayList<>();
		if (!approvalsExecutionList.isEmpty()) {
			Task opTask = createSimpleTask(PagePreviewChanges.class + ".createApprovals");      // TODO
			OperationResult result = opTask.getResult();
			ObjectResolver modelObjectResolver = getModelObjectResolver();
			try {
				for (ApprovalSchemaExecutionInformationType execution : approvalsExecutionList) {
					approvals.add(ApprovalProcessExecutionInformationDto
							.createFrom(execution, modelObjectResolver, true, opTask, result)); // TODO reuse session
				}
				result.computeStatus();
			} catch (Throwable t) {
				LoggingUtils.logUnexpectedException(LOGGER, "Couldn't prepare approval information", t);
				result.recordFatalError("Couldn't prepare approval information: " + t.getMessage(), t);
			}
			if (WebComponentUtil.showResultInPage(result)) {
				showResult(result);
			}
		}
		approvalsModel = Model.ofList(approvals);

		initLayout();
	}

	@Override
	protected void createBreadcrumb() {
		createInstanceBreadcrumb();
	}

	private void initLayout() {
		Form mainForm = new Form("mainForm");
		mainForm.setMultiPart(true);
		add(mainForm);

		mainForm.add(new ScenePanel(ID_PRIMARY_DELTAS_SCENE, primaryDeltasModel));
		mainForm.add(new ScenePanel(ID_SECONDARY_DELTAS_SCENE, secondaryDeltasModel));

		WebMarkupContainer policyViolationsContainer = new WebMarkupContainer(ID_POLICY_VIOLATIONS_CONTAINER);
		policyViolationsContainer.add(new VisibleBehaviour(() -> !violationsEmpty()));
		policyViolationsContainer.add(new EvaluatedTriggerGroupListPanel(ID_POLICY_VIOLATIONS, policyViolationsModel));
		mainForm.add(policyViolationsContainer);

		WebMarkupContainer approvalsContainer = new WebMarkupContainer(ID_APPROVALS_CONTAINER);
		approvalsContainer.add(new VisibleBehaviour(() -> violationsEmpty() && !approvalsEmpty()));
		approvalsContainer.add(new ApprovalProcessesPreviewPanel(ID_APPROVALS, approvalsModel));
		mainForm.add(approvalsContainer);

		initButtons(mainForm);
	}

	private boolean approvalsEmpty() {
		return approvalsModel.getObject().isEmpty();
	}

	private boolean violationsEmpty() {
		return EvaluatedTriggerGroupDto.isEmpty(policyViolationsModel.getObject());
	}

	private void initButtons(Form mainForm) {
		AjaxButton cancel = new AjaxButton(ID_CONTINUE_EDITING, createStringResource("PagePreviewChanges.button.continueEditing")) {
			@Override
			public void onClick(AjaxRequestTarget target) {
				cancelPerformed(target);
			}
		};
		mainForm.add(cancel);

		AjaxButton save = new AjaxButton(ID_SAVE, createStringResource("PagePreviewChanges.button.save")) {
			@Override
			public void onClick(AjaxRequestTarget target) {
				savePerformed(target);
			}
		};
		save.add(new EnableBehaviour(() -> !violationsEmpty()));
		mainForm.add(save);
	}

	private void cancelPerformed(AjaxRequestTarget target) {
		redirectBack();
	}

	private void savePerformed(AjaxRequestTarget target) {
		Breadcrumb bc = redirectBack();
		if (bc instanceof BreadcrumbPageInstance) {
			BreadcrumbPageInstance bcpi = (BreadcrumbPageInstance) bc;
			WebPage page = bcpi.getPage();
			if (page instanceof PageAdminObjectDetails) {
				((PageAdminObjectDetails) page).setSaveOnConfigure(true);
			} else {
				error("Couldn't save changes - unexpected referring page: " + page);
			}
		} else {
			error("Couldn't save changes - no instance for referring page; breadcrumb is " + bc);
		}
	}

}

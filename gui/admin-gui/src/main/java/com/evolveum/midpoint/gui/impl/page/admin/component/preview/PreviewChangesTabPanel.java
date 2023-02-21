/*
 * Copyright (C) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.component.preview;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.page.admin.focus.PageFocusPreviewChanges;
import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.api.visualizer.ModelContextVisualization;
import com.evolveum.midpoint.model.api.visualizer.Visualization;
import com.evolveum.midpoint.repo.common.ObjectResolver;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.prism.show.ChangesPanel;
import com.evolveum.midpoint.web.component.prism.show.VisualizationDto;
import com.evolveum.midpoint.web.component.prism.show.VisualizationPanel;
import com.evolveum.midpoint.web.component.prism.show.WrapperVisualization;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.component.wf.ApprovalProcessesPreviewPanel;
import com.evolveum.midpoint.web.page.admin.workflow.EvaluatedTriggerGroupListPanel;
import com.evolveum.midpoint.web.page.admin.workflow.dto.ApprovalProcessExecutionInformationDto;
import com.evolveum.midpoint.web.page.admin.workflow.dto.EvaluatedTriggerGroupDto;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ApprovalSchemaExecutionInformationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyRuleEnforcerPreviewOutputType;

public class PreviewChangesTabPanel<O extends ObjectType> extends BasePanel<ModelContext<O>> {
    private static final long serialVersionUID = 1L;

    private static final String ID_PRIMARY_DELTAS = "primaryDeltas";
    private static final String ID_SECONDARY_DELTAS = "secondaryDeltas";
    private static final String ID_APPROVALS_CONTAINER = "approvalsContainer";
    private static final String ID_APPROVALS = "approvals";
    private static final String ID_POLICY_VIOLATIONS_CONTAINER = "policyViolationsContainer";
    private static final String ID_POLICY_VIOLATIONS = "policyViolations";

    private IModel<VisualizationDto> primaryDeltasModel;
    private IModel<VisualizationDto> secondaryDeltasModel;
    private IModel<List<EvaluatedTriggerGroupDto>> policyViolationsModel;
    private IModel<List<ApprovalProcessExecutionInformationDto>> approvalsModel;

    private static final Trace LOGGER = TraceManager.getTrace(PreviewChangesTabPanel.class);

    public PreviewChangesTabPanel(String id, IModel<ModelContext<O>> contextModel) {
        super(id, contextModel);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        initModels();
        initLayout();
    }

    private void initModels() {
        ModelContextVisualization mcVisualization;

        ModelContext<O> modelContext = getModelObject();
        try {
            Task task = getPageBase().createSimpleTask("visualize");
            OperationResult result = task.getResult();

            mcVisualization = getPageBase().getModelInteractionService().visualizeModelContext(modelContext, task, result);
        } catch (SchemaException | ExpressionEvaluationException | ConfigurationException e) {
            throw new SystemException(e);        // TODO
        }

        final List<? extends Visualization> primary = mcVisualization.getPrimary();
        final List<? extends Visualization> secondary = mcVisualization.getSecondary();

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Creating context DTO for primary deltas:\n{}", DebugUtil.debugDump(primary));
            LOGGER.trace("Creating context DTO for secondary deltas:\n{}", DebugUtil.debugDump(secondary));
        }

        final WrapperVisualization primaryVisualization = new WrapperVisualization(primary,
                primary.size() != 1 ? "PagePreviewChanges.primaryChangesMore" : "PagePreviewChanges.primaryChangesOne", primary.size());
        final WrapperVisualization secondaryVisualization = new WrapperVisualization(secondary,
                secondary.size() != 1 ? "PagePreviewChanges.secondaryChangesMore" : "PagePreviewChanges.secondaryChangesOne", secondary.size());

        final VisualizationDto primaryDto = new VisualizationDto(primaryVisualization);
        final VisualizationDto secondaryDto = new VisualizationDto(secondaryVisualization);

        primaryDeltasModel = () -> primaryDto;
        secondaryDeltasModel = () -> secondaryDto;

        PolicyRuleEnforcerPreviewOutputType enforcements = modelContext != null
                ? modelContext.getPolicyRuleEnforcerPreviewOutput()
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
            Task opTask = getPageBase().createSimpleTask(PageFocusPreviewChanges.class + ".createApprovals");      // TODO
            OperationResult result = opTask.getResult();
            ObjectResolver modelObjectResolver = getPageBase().getModelObjectResolver();
            try {
                for (ApprovalSchemaExecutionInformationType execution : approvalsExecutionList) {
                    approvals.add(ApprovalProcessExecutionInformationDto
                            .createFrom(execution, modelObjectResolver, true, opTask, result)); // TODO reuse session
                }
                result.computeStatus();
            } catch (Throwable t) {
                LoggingUtils.logUnexpectedException(LOGGER, "Couldn't prepare approval information", t);
                result.recordFatalError(
                        createStringResource("PreviewChangesTabPanel.message.prepareApproval.fatalError", t.getMessage()).getString(), t);
            }
            if (WebComponentUtil.showResultInPage(result)) {
                getPageBase().showResult(result);
            }
        }
        approvalsModel = Model.ofList(approvals);
    }

    private void initLayout() {
        VisualizationPanel primaryDeltas = new VisualizationPanel(ID_PRIMARY_DELTAS, primaryDeltasModel);
        primaryDeltas.add(VisibleBehaviour.ALWAYS_INVISIBLE);
        add(primaryDeltas);

        VisualizationPanel secondaryDeltas = new VisualizationPanel(ID_SECONDARY_DELTAS, secondaryDeltasModel);
        secondaryDeltas.add(VisibleBehaviour.ALWAYS_INVISIBLE);
        add(secondaryDeltas);

        add(new ChangesPanel("primary", null, primaryDeltasModel) {

            @Override
            protected IModel<String> createTitle() {
                return () -> {
                    WrapperVisualization wrapper = (WrapperVisualization) primaryDeltasModel.getObject().getVisualization();

                    return getString(wrapper.getDisplayNameKey(), wrapper.getDisplayNameParameters());
                };
            }
        });
        add(new ChangesPanel("secondary", null, secondaryDeltasModel) {

            @Override
            protected IModel<String> createTitle() {
                return () -> {
                    WrapperVisualization wrapper = (WrapperVisualization) secondaryDeltasModel.getObject().getVisualization();

                    return getString(wrapper.getDisplayNameKey(), wrapper.getDisplayNameParameters());
                };
            }
        });

        WebMarkupContainer policyViolationsContainer = new WebMarkupContainer(ID_POLICY_VIOLATIONS_CONTAINER);
        policyViolationsContainer.add(new VisibleBehaviour(() -> !violationsEmpty()));
        policyViolationsContainer.add(new EvaluatedTriggerGroupListPanel(ID_POLICY_VIOLATIONS, policyViolationsModel));
        add(policyViolationsContainer);

        WebMarkupContainer approvalsContainer = new WebMarkupContainer(ID_APPROVALS_CONTAINER);
        approvalsContainer.add(new VisibleBehaviour(() -> violationsEmpty() && !approvalsEmpty()));
        approvalsContainer.add(new ApprovalProcessesPreviewPanel(ID_APPROVALS, approvalsModel));
        add(approvalsContainer);

    }

    private boolean approvalsEmpty() {
        return approvalsModel.getObject().isEmpty();
    }

    private boolean violationsEmpty() {
        return EvaluatedTriggerGroupDto.isEmpty(policyViolationsModel.getObject());
    }
}

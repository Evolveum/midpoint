/*
 * Copyright (C) 2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.admin.component.preview;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.page.admin.focus.PageFocusPreviewChanges;
import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.api.visualizer.ModelContextVisualization;
import com.evolveum.midpoint.model.api.visualizer.Visualization;
import com.evolveum.midpoint.schema.TaskExecutionMode;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.SingleLocalizableMessage;
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

public class PreviewChangesTabPanel extends BasePanel<Void> {
    private static final long serialVersionUID = 1L;

    private static final String ID_PRIMARY_DELTAS = "primaryDeltas";
    private static final String ID_SECONDARY_DELTAS = "secondaryDeltas";
    private static final String ID_PRIMARY = "primary";
    private static final String ID_SECONDARY = "secondary";
    private static final String ID_APPROVALS_CONTAINER = "approvalsContainer";
    private static final String ID_APPROVALS = "approvals";
    private static final String ID_POLICY_VIOLATIONS_CONTAINER = "policyViolationsContainer";
    private static final String ID_POLICY_VIOLATIONS = "policyViolations";

    private IModel<List<VisualizationDto>> primaryModel;
    private IModel<List<VisualizationDto>> secondaryModel;
    private IModel<List<EvaluatedTriggerGroupDto>> policyViolationsModel;
    private IModel<List<ApprovalProcessExecutionInformationDto>> approvalsModel;

    private final PreviewData data;

    private static final Trace LOGGER = TraceManager.getTrace(PreviewChangesTabPanel.class);

    public PreviewChangesTabPanel(String id, PreviewData data) {
        super(id);
        this.data = data;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        initModels();
        initLayout();
    }

    /** Initializes Wicket models from already extracted serializable preview data. */
    private void initModels() {
        primaryModel = Model.ofList(data.primary());
        secondaryModel = Model.ofList(data.secondary());
        policyViolationsModel = Model.ofList(data.policyViolations());
        approvalsModel = Model.ofList(data.approvals());
    }

    /**
     * Extracts the data needed by the preview page from live model contexts.
     *
     * The returned data is safe to keep in Wicket page state; the original model contexts are not.
     */
    public static <O extends ObjectType> PreviewData createPreviewData(
            String title, ModelContext<O> modelContext, PageBase pageBase) {
        ModelContextVisualization mcVisualization;

        try {
            Task task = pageBase.createSimpleTask("visualize");
            OperationResult result = task.getResult();

            mcVisualization = pageBase.getModelInteractionService().visualizeModelContext(modelContext, task, result);
        } catch (SchemaException | ExpressionEvaluationException | ConfigurationException e) {
            throw new SystemException(e);        // TODO
        }

        final List<? extends Visualization> primary = mcVisualization.getPrimary();
        final List<? extends Visualization> secondary = mcVisualization.getSecondary();

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Creating context DTO for primary deltas:\n{}", DebugUtil.debugDump(primary));
            LOGGER.trace("Creating context DTO for secondary deltas:\n{}", DebugUtil.debugDump(secondary));
        }

        final List<VisualizationDto> primaryList = primary.stream().map(v -> new VisualizationDto(v)).collect(Collectors.toList());
        final List<VisualizationDto> secondaryList = secondary.stream().map(v -> new VisualizationDto(v)).collect(Collectors.toList());

        PolicyRuleEnforcerPreviewOutputType enforcements = modelContext != null
                ? modelContext.getPolicyRuleEnforcerPreviewOutput()
                : null;
        List<EvaluatedTriggerGroupDto> triggerGroups = enforcements != null
                ? Collections.singletonList(EvaluatedTriggerGroupDto.initializeFromRules(enforcements.getRule(), false, null))
                : Collections.emptyList();

        List<ApprovalSchemaExecutionInformationType> approvalsExecutionList = modelContext != null
                ? modelContext.getHookPreviewResults(ApprovalSchemaExecutionInformationType.class)
                : Collections.emptyList();
        List<ApprovalProcessExecutionInformationDto> approvals = new ArrayList<>();
        if (!approvalsExecutionList.isEmpty()) {
            Task opTask = pageBase.createSimpleTask(PageFocusPreviewChanges.class + ".createApprovals");      // TODO
            OperationResult result = opTask.getResult();
            try {
                for (ApprovalSchemaExecutionInformationType execution : approvalsExecutionList) {
                    approvals.add(ApprovalProcessExecutionInformationDto
                            .createFrom(execution, true, opTask, result,
                                    pageBase)); // TODO reuse session
                }
                result.computeStatus();
            } catch (Throwable t) {
                LoggingUtils.logUnexpectedException(LOGGER, "Couldn't prepare approval information", t);
                result.recordFatalError(
                        pageBase.createStringResource(
                                "PreviewChangesTabPanel.message.prepareApproval.fatalError", t.getMessage()).getString(), t);
            }
            if (WebComponentUtil.showResultInPage(result)) {
                pageBase.showResult(result);
            }
        }

        return new PreviewData(
                title,
                primaryList,
                secondaryList,
                triggerGroups,
                approvals,
                modelContext != null
                        && TaskExecutionMode.SIMULATED_PRODUCTION.equals(modelContext.getTaskExecutionMode()));
    }

    private IModel<VisualizationDto> createVisualizationModel(IModel<List<VisualizationDto>> model, String oneKey, String moreKey) {
        return new LoadableModel<>(false) {

            @Override
            protected VisualizationDto load() {
                List<Visualization> visualizations = model.getObject().stream().map(v -> v.getVisualization()).collect(Collectors.toList());

                int size = visualizations.size();
                String key = size != 1 ? moreKey : oneKey;

                return new VisualizationDto(new WrapperVisualization(new SingleLocalizableMessage(key, new Object[] { size }), visualizations));
            }
        };
    }

    private void initLayout() {
        VisualizationPanel primaryDeltas = new VisualizationPanel(ID_PRIMARY_DELTAS,
                createVisualizationModel(primaryModel, "PagePreviewChanges.primaryChangesOne", "PagePreviewChanges.primaryChangesMore"));
        primaryDeltas.add(VisibleBehaviour.ALWAYS_INVISIBLE);
        add(primaryDeltas);

        VisualizationPanel secondaryDeltas = new VisualizationPanel(ID_SECONDARY_DELTAS,
                createVisualizationModel(secondaryModel, "PagePreviewChanges.secondaryChangesOne", "PagePreviewChanges.secondaryChangesMore"));
        add(secondaryDeltas);

        add(new ChangesPanel(ID_PRIMARY, primaryModel) {

            @Override
            protected IModel<String> createTitle() {
                return () -> {
                    int size = primaryModel.getObject().size();
                    String key = size != 1 ? "PagePreviewChanges.primaryChangesMore" : "PagePreviewChanges.primaryChangesOne";

                    return getString(key, size);
                };
            }
        });
        add(new ChangesPanel(ID_SECONDARY, secondaryModel) {

            @Override
            protected IModel<String> createTitle() {
                return () -> {
                    int size = secondaryModel.getObject().size();
                    String key = size != 1 ? "PagePreviewChanges.secondaryChangesMore" : "PagePreviewChanges.secondaryChangesOne";

                    return getString(key, size);
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

    /**
     * Serializable page-state representation of one preview tab.
     *
     * It is created once while rendering the preview page and then reused by tab panels
     * and button visibility checks during later Wicket requests.
     */
    public record PreviewData(
            String title,
            List<VisualizationDto> primary,
            List<VisualizationDto> secondary,
            List<EvaluatedTriggerGroupDto> policyViolations,
            List<ApprovalProcessExecutionInformationDto> approvals,
            boolean withProductionConfiguration) implements Serializable {

        @Serial private static final long serialVersionUID = 1L;

        public boolean violationsEmpty() {
            return EvaluatedTriggerGroupDto.isEmpty(policyViolations);
        }
    }
}

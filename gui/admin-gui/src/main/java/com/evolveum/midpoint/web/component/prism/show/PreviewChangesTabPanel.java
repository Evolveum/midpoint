/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.prism.show;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.api.context.ModelProjectionContext;
import com.evolveum.midpoint.model.api.visualizer.Scene;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.repo.common.ObjectResolver;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.component.wf.ApprovalProcessesPreviewPanel;
import com.evolveum.midpoint.web.page.admin.workflow.EvaluatedTriggerGroupListPanel;
import com.evolveum.midpoint.web.page.admin.workflow.dto.ApprovalProcessExecutionInformationDto;
import com.evolveum.midpoint.web.page.admin.workflow.dto.EvaluatedTriggerGroupDto;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ApprovalSchemaExecutionInformationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyRuleEnforcerPreviewOutputType;

import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.apache.commons.collections.CollectionUtils.addIgnoreNull;

/**
 * Created by kate on 22.8.2018.
 */
public class PreviewChangesTabPanel<O extends ObjectType> extends BasePanel<ModelContext<O>> {
    private static final long serialVersionUID = 1L;

    private static final String ID_PRIMARY_DELTAS_SCENE = "primaryDeltas";
    private static final String ID_SECONDARY_DELTAS_SCENE = "secondaryDeltas";
    private static final String ID_APPROVALS_CONTAINER = "approvalsContainer";
    private static final String ID_APPROVALS = "approvals";
    private static final String ID_POLICY_VIOLATIONS_CONTAINER = "policyViolationsContainer";
    private static final String ID_POLICY_VIOLATIONS = "policyViolations";

    private IModel<SceneDto> primaryDeltasModel;
    private IModel<SceneDto> secondaryDeltasModel;
    private IModel<List<EvaluatedTriggerGroupDto>> policyViolationsModel;
    private IModel<List<ApprovalProcessExecutionInformationDto>> approvalsModel;

    private static final Trace LOGGER = TraceManager.getTrace(PreviewChangesTabPanel.class);

    public PreviewChangesTabPanel(String id, IModel<ModelContext<O>> contextModel){
        super(id, contextModel);
    }

    @Override
    protected void onInitialize(){
        super.onInitialize();

        initModels();
        initLayout();
    }

    private void initModels(){
        final List<ObjectDelta<? extends ObjectType>> primaryDeltas = new ArrayList<>();
        final List<ObjectDelta<? extends ObjectType>> secondaryDeltas = new ArrayList<>();
        final List<? extends Scene> primaryScenes;
        final List<? extends Scene> secondaryScenes;

        ModelContext<O> modelContext = getModelObject();
        try {
            if (modelContext != null) {
                if (modelContext.getFocusContext() != null) {
                    addIgnoreNull(primaryDeltas, CloneUtil.clone(modelContext.getFocusContext().getPrimaryDelta()));
                    addIgnoreNull(secondaryDeltas, CloneUtil.clone(modelContext.getFocusContext().getSecondaryDelta()));
                }
                for (ModelProjectionContext projCtx : modelContext.getProjectionContexts()) {
                    addIgnoreNull(primaryDeltas, CloneUtil.clone(projCtx.getPrimaryDelta()));
                    addIgnoreNull(secondaryDeltas, CloneUtil.clone(projCtx.getExecutableDelta()));
                }
            }
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Primary deltas:\n{}", DebugUtil.debugDump(primaryDeltas));
                LOGGER.trace("Secondary deltas:\n{}", DebugUtil.debugDump(secondaryDeltas));
            }

            Task task = getPageBase().createSimpleTask("visualize");
            primaryScenes = getPageBase().getModelInteractionService().visualizeDeltas(primaryDeltas, task, task.getResult());
            secondaryScenes = getPageBase().getModelInteractionService().visualizeDeltas(secondaryDeltas, task, task.getResult());
        } catch (SchemaException | ExpressionEvaluationException e) {
            throw new SystemException(e);        // TODO
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
        primaryDeltasModel = new IModel<SceneDto>() {
            @Override
            public SceneDto getObject() {
                return primarySceneDto;
            }
        };
        secondaryDeltasModel = new IModel<SceneDto>() {
            @Override
            public SceneDto getObject() {
                return secondarySceneDto;
            }
        };

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
            Task opTask = getPageBase().createSimpleTask(PagePreviewChanges.class + ".createApprovals");      // TODO
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

    private void initLayout(){
        add(new ScenePanel(ID_PRIMARY_DELTAS_SCENE, primaryDeltasModel));
        add(new ScenePanel(ID_SECONDARY_DELTAS_SCENE, secondaryDeltasModel));

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

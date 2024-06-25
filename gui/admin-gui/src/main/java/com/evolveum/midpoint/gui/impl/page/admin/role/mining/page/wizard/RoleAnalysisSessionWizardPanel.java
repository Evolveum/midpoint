/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.wizard;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.model.api.ModelInteractionService;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.panel.Fragment;

import com.evolveum.midpoint.gui.api.component.wizard.WizardModel;
import com.evolveum.midpoint.gui.api.component.wizard.WizardPanel;
import com.evolveum.midpoint.gui.api.component.wizard.WizardStep;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.component.wizard.AbstractWizardPanel;
import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.gui.impl.page.admin.ObjectChangesExecutorImpl;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.AssignmentHolderDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.page.PageRoleAnalysis;
import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.ObjectDeltaOperation;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;

import org.apache.wicket.model.IModel;

public class RoleAnalysisSessionWizardPanel extends AbstractWizardPanel<RoleAnalysisSessionType, AssignmentHolderDetailsModel<RoleAnalysisSessionType>> {

    private static final String DOT_CLASS = RoleAnalysisSessionWizardPanel.class.getName() + ".";
    private static final String OP_PROCESS_CLUSTERING = DOT_CLASS + "processClustering";

    public static final Trace LOGGER = TraceManager.getTrace(RoleAnalysisSessionWizardPanel.class);

    public RoleAnalysisSessionWizardPanel(String id, WizardPanelHelper<RoleAnalysisSessionType, AssignmentHolderDetailsModel<RoleAnalysisSessionType>> helper) {
        super(id, helper);
    }

    protected void initLayout() {
        getPageBase().getFeedbackPanel().add(VisibleEnableBehaviour.ALWAYS_INVISIBLE);

        String idOfChoicePanel = getIdOfChoicePanel();

        AnalysisCategoryChoiceStepPanel components = new AnalysisCategoryChoiceStepPanel(idOfChoicePanel, getHelper().getDetailsModel()) {
            @Override
            protected void onExitPerformed(AjaxRequestTarget target) {
                RoleAnalysisSessionWizardPanel.this.onExitPerformed();
            }

            @Override
            protected void onSubmitPerformed(AjaxRequestTarget target) {
                showWizardFragment(target, new WizardPanel(getIdOfWizardPanel(), new WizardModel(createBasicSteps())));
                super.onSubmitPerformed(target);
            }
        };

        Fragment choiceFragment = createChoiceFragment(new ProcessModeChoiceStepPanel(idOfChoicePanel, getHelper().getDetailsModel()) {
            @Override
            protected void onExitPerformed(AjaxRequestTarget target) {
                RoleAnalysisSessionWizardPanel.this.onExitPerformed();
            }

            @Override
            protected void onSubmitPerformed(AjaxRequestTarget target) {
                showChoiceFragment(target, components);
                super.onSubmitPerformed(target);
            }
        });

        add(choiceFragment);
    }

    private List<WizardStep> createBasicSteps() {
        TaskType taskType = new TaskType();
        List<WizardStep> steps = new ArrayList<>();

        steps.add(new BasicSessionInformationStepPanel(getHelper().getDetailsModel()) {
            @Override
            public VisibleEnableBehaviour getBackBehaviour() {
                return VisibleEnableBehaviour.ALWAYS_INVISIBLE;
            }

            @Override
            protected void onExitPerformed(AjaxRequestTarget target) {
                RoleAnalysisSessionWizardPanel.this.onExitPerformed();
            }

            @Override
            protected void onSubmitPerformed(AjaxRequestTarget target) {
                RoleAnalysisSessionType session = getHelper().getDetailsModel().getObjectType();

                RoleAnalysisCategoryType analysisCategory = session.getAnalysisOption().getAnalysisCategory();
                if (analysisCategory.equals(RoleAnalysisCategoryType.ADVANCED)
                        || analysisCategory.equals(RoleAnalysisCategoryType.OUTLIERS)) {
//                    || analysisCategory.equals(RoleAnalysisCategoryType.STANDARD)
                    showWizardFragment(target, new WizardPanel(getIdOfWizardPanel(), new WizardModel(createBasicSteps())));
                    super.onSubmitPerformed(target);
                }
                finalSubmitPerform(target, taskType);
            }
        });

        RoleAnalysisSessionType session = getHelper().getDetailsModel().getObjectType();

        RoleAnalysisCategoryType analysisCategory = session.getAnalysisOption().getAnalysisCategory();
        if (!analysisCategory.equals(RoleAnalysisCategoryType.ADVANCED)
                && !analysisCategory.equals(RoleAnalysisCategoryType.OUTLIERS)) {
//                    || analysisCategory.equals(RoleAnalysisCategoryType.STANDARD)
            return steps;
        }

        steps.add(new FilteringRoleAnalysisSessionOptionWizardPanel(getHelper().getDetailsModel()) {
            @Override
            public VisibleEnableBehaviour getBackBehaviour() {
                return VisibleEnableBehaviour.ALWAYS_VISIBLE_ENABLED;
            }

            @Override
            public boolean onNextPerformed(AjaxRequestTarget target) {
                return super.onNextPerformed(target);
            }

            @Override
            protected void onExitPerformed(AjaxRequestTarget target) {
                RoleAnalysisSessionWizardPanel.this.onExitPerformed();
            }
        });

        steps.add(new ClusteringRoleAnalysisSessionOptionWizardPanel(getHelper().getDetailsModel()) {
            @Override
            public VisibleEnableBehaviour getBackBehaviour() {
                return VisibleEnableBehaviour.ALWAYS_VISIBLE_ENABLED;
            }

            @Override
            public boolean onNextPerformed(AjaxRequestTarget target) {
                return super.onNextPerformed(target);
            }

            @Override
            protected void onExitPerformed(AjaxRequestTarget target) {
                RoleAnalysisSessionWizardPanel.this.onExitPerformed();
            }
        });

        steps.add(new RoleAnalysisSessionDetectionOptionsWizardPanel(getHelper().getDetailsModel()) {
            @Override
            public VisibleEnableBehaviour getBackBehaviour() {
                return VisibleEnableBehaviour.ALWAYS_VISIBLE_ENABLED;
            }

            @Override
            public IModel<String> getTitle() {
                return super.getTitle();
            }

            @Override
            protected IModel<String> getTextModel() {
                return super.getTextModel();
            }

            @Override
            protected IModel<String> getSubTextModel() {
                return super.getSubTextModel();
            }

            @Override
            protected boolean isVisibleSubContainer(PrismContainerWrapper c) {
                return super.isVisibleSubContainer(c);
            }

            @Override
            public boolean onBackPerformed(AjaxRequestTarget target) {
                return super.onBackPerformed(target);
            }

            @Override
            protected void onExitPerformed(AjaxRequestTarget target) {
                RoleAnalysisSessionWizardPanel.this.onExitPerformed();
            }

            @Override
            protected void onSubmitPerformed(AjaxRequestTarget target) {
                finalSubmitPerform(target, taskType);
            }

        });

        steps.add(new RoleAnalysisSessionMaintenanceWizardPanel(getHelper().getDetailsModel(), taskType) {
            @Override
            public IModel<String> getTitle() {
                return createStringResource("RoleAnalysisSessionMaintenanceWizardPanel.title");
            }

            @Override
            public boolean onBackPerformed(AjaxRequestTarget target) {
                return super.onBackPerformed(target);
            }

            @Override
            protected void onExitPerformed(AjaxRequestTarget target) {
                RoleAnalysisSessionWizardPanel.this.onExitPerformed();
            }

            @Override
            protected void onSubmitPerformed(AjaxRequestTarget target) {
                onSubmitPerform();
                finalSubmitPerform(target, taskType);
            }
        });

        return steps;
    }

    private void finalSubmitPerform(AjaxRequestTarget target, TaskType taskType) {
        Task task = getPageBase().createSimpleTask(OP_PROCESS_CLUSTERING);
        OperationResult result = task.getResult();

        Collection<ObjectDelta<? extends ObjectType>> deltas;
        try {
            deltas = getHelper().getDetailsModel().collectDeltas(result);

            Collection<ObjectDeltaOperation<? extends ObjectType>> objectDeltaOperations = new ObjectChangesExecutorImpl()
                    .executeChanges(deltas, false, task, result, target);

            String sessionOid = ObjectDeltaOperation.findAddDeltaOidRequired(objectDeltaOperations,
                    RoleAnalysisSessionType.class);

            RoleAnalysisService roleAnalysisService = getPageBase().getRoleAnalysisService();

            PrismObject<RoleAnalysisSessionType> sessionTypeObject = roleAnalysisService.getSessionTypeObject(sessionOid, task, result);

            if (sessionTypeObject != null) {
                ModelInteractionService modelInteractionService = getPageBase().getModelInteractionService();
                roleAnalysisService.executeClusteringTask(modelInteractionService, sessionTypeObject,
                        null, null, task, result, taskType);
            }
        } catch (Throwable e) {
            LoggingUtils.logException(LOGGER, "Couldn't process clustering", e);
            result.recordFatalError(
                    createStringResource("RoleAnalysisSessionWizardPanel.message.clustering.error").getString()
                    , e);
        }

        setResponsePage(PageRoleAnalysis.class);
        ((PageBase) getPage()).showResult(result);
        target.add(getFeedbackPanel());
    }

    private void onExitPerformed() {
        setResponsePage(PageRoleAnalysis.class);
    }

    private void exitToPreview(AjaxRequestTarget target) {
    }

}

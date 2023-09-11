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

import org.apache.wicket.ajax.AjaxRequestTarget;

import com.evolveum.midpoint.gui.api.component.wizard.WizardModel;
import com.evolveum.midpoint.gui.api.component.wizard.WizardPanel;
import com.evolveum.midpoint.gui.api.component.wizard.WizardStep;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.component.wizard.AbstractWizardPanel;
import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.gui.impl.page.admin.ObjectChangesExecutorImpl;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.AssignmentHolderDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.page.PageRoleAnalysis;
import com.evolveum.midpoint.model.api.ActivitySubmissionOptions;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.ObjectDeltaOperation;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

public class RoleAnalysisSessionWizardPanel extends AbstractWizardPanel<RoleAnalysisSessionType, AssignmentHolderDetailsModel<RoleAnalysisSessionType>> {

    private static final String DOT_CLASS = RoleAnalysisSessionWizardPanel.class.getName() + ".";
    private static final String OP_PROCESS_CLUSTERING = DOT_CLASS + "processClustering";

    public RoleAnalysisSessionWizardPanel(String id, WizardPanelHelper<RoleAnalysisSessionType, AssignmentHolderDetailsModel<RoleAnalysisSessionType>> helper) {
        super(id, helper);
    }

    protected void initLayout() {
        getPageBase().getFeedbackPanel().add(VisibleEnableBehaviour.ALWAYS_INVISIBLE);

        add(createChoiceFragment(new ProcessModeChoiceStepPanel(getIdOfChoicePanel(), getHelper().getDetailsModel()) {
            @Override
            protected void onExitPerformed(AjaxRequestTarget target) {
                RoleAnalysisSessionWizardPanel.this.onExitPerformed();
            }

            @Override
            protected void onSubmitPerformed(AjaxRequestTarget target) {
                showWizardFragment(target, new WizardPanel(getIdOfWizardPanel(),
                        new WizardModel(createBasicSteps())));
                super.onSubmitPerformed(target);
            }

        }));

    }

    private List<WizardStep> createBasicSteps() {
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

        });

        steps.add(new RoleAnalysisSessionSimpleObjectsWizardPanel(getHelper().getDetailsModel()) {
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
            public boolean onBackPerformed(AjaxRequestTarget target) {
                return super.onBackPerformed(target);
            }

            @Override
            protected void onExitPerformed(AjaxRequestTarget target) {
                RoleAnalysisSessionWizardPanel.this.onExitPerformed();
            }

            @Override
            protected void onSubmitPerformed(AjaxRequestTarget target) {
                Task task = getPageBase().createSimpleTask(OP_PROCESS_CLUSTERING);
                OperationResult result = task.getResult();

                Collection<ObjectDelta<? extends ObjectType>> deltas;
                try {
                    deltas = getHelper().getDetailsModel().collectDeltas(result);
                } catch (SchemaException e) {
                    throw new RuntimeException(e);
                }

                Collection<ObjectDeltaOperation<? extends ObjectType>> objectDeltaOperations = new ObjectChangesExecutorImpl()
                        .executeChanges(deltas, false, task, result, target);

                String sessionOid = ObjectDeltaOperation.findAddDeltaOidRequired(objectDeltaOperations,
                        RoleAnalysisSessionType.class);

                executeClusteringTask(result, task, sessionOid);

                setResponsePage(PageRoleAnalysis.class);
                ((PageBase) getPage()).showResult(result);
                target.add(getFeedbackPanel());
            }

        });

        return steps;
    }

    private void onExitPerformed() {
        setResponsePage(PageRoleAnalysis.class);
    }

    private void executeClusteringTask(OperationResult result, Task task, String sessionOid) {
        try {
            ActivityDefinitionType activity = createActivity(sessionOid);

            getPageBase().getModelInteractionService().submit(
                    activity,
                    ActivitySubmissionOptions.create()
                            .withTaskTemplate(new TaskType()
                                    .name("Session clustering  (" + sessionOid + ")"))
                            .withArchetypes(
                                    SystemObjectsType.ARCHETYPE_UTILITY_TASK.value()),
                    task, result);

        } catch (CommonException e) {
            //TODO
        }
    }

    private ActivityDefinitionType createActivity(String sessionOid) {

        ObjectReferenceType objectReferenceType = new ObjectReferenceType();
        objectReferenceType.setType(RoleAnalysisSessionType.COMPLEX_TYPE);
        objectReferenceType.setOid(sessionOid);

        RoleAnalysisClusteringWorkDefinitionType rdw = new RoleAnalysisClusteringWorkDefinitionType();
        rdw.setSessionRef(objectReferenceType);

        return new ActivityDefinitionType()
                .work(new WorkDefinitionsType()
                        .roleAnalysisClustering(rdw));
    }

    private void exitToPreview(AjaxRequestTarget target) {
    }

}

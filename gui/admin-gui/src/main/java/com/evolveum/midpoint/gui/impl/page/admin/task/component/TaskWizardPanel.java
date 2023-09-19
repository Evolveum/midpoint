/*
 * Copyright (C) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.task.component;

import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.web.component.AjaxIconButton;

import org.apache.wicket.ajax.AjaxRequestTarget;

import com.evolveum.midpoint.gui.api.component.wizard.WizardModel;
import com.evolveum.midpoint.gui.api.component.wizard.WizardPanel;
import com.evolveum.midpoint.gui.api.component.wizard.WizardStep;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.wizard.AbstractWizardPanel;
import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.gui.impl.page.admin.task.TaskDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.task.component.wizard.*;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemObjectsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WorkDefinitionsType;

import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

public class TaskWizardPanel extends AbstractWizardPanel<TaskType, TaskDetailsModel> {


    public TaskWizardPanel(String id, WizardPanelHelper<TaskType, TaskDetailsModel> helper) {
        super(id, helper);
    }

    @Override
    protected void initLayout() {
        getPageBase().getFeedbackPanel().add(VisibleEnableBehaviour.ALWAYS_INVISIBLE);
        add(createWizardFragment(new WizardPanel(getIdOfWizardPanel(), new WizardModel(createBasicSteps()))));
    }

    private List<WizardStep> createBasicSteps() {
        List<WizardStep> steps = new ArrayList<>();

        steps.add(new TaskBasicWizardPanel(getAssignmentHolderModel()));

        TaskType task = getAssignmentHolderModel().getObjectType();

        ItemName activityName = getActivityName(task);
        if (activityName != null) {
            steps.add(new TaskResourceObjectsWizardPanel(activityName, getAssignmentHolderModel()));
        }

        //TODO this is not very clean, should be somehow passed to the wizard
        boolean isSimulationTask = task.getActivity().getExecution() != null && task.getActivity().getExecution().getMode() != null;
        if (isSimulationTask) {
            steps.add(new TaskExecutionWizardPanel(getAssignmentHolderModel()));
        }

        boolean isImport = WebComponentUtil.hasArchetypeAssignment(task, SystemObjectsType.ARCHETYPE_IMPORT_TASK.value());
        if (!isImport) {
            steps.add(new TaskScheduleWizardPanel(getAssignmentHolderModel()));
        }

        steps.add(new TaskDistributionWizardPanel(getAssignmentHolderModel()) {

            @Override
            protected IModel<String> getSubmitLabelModel() {
                return getPageBase().createStringResource("PageBase.button.saveAndRun");
            }

            @Override
            protected void onSubmitPerformed(AjaxRequestTarget target) {
                WebComponentUtil.setTaskStateBeforeSave(
                        getDetailsModel().getObjectWrapper(), true, getPageBase(), target);
                getHelper().onSaveObjectPerformed(target);
            }

            @Override
            protected void initCustomButtons(RepeatingView customButtons) {
                AjaxIconButton saveAndRun = new AjaxIconButton(
                        customButtons.newChildId(),
                        Model.of("mr-1 fa fa-save"),
                        getPageBase().createStringResource("WizardPanel.submit")) {
                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        WebComponentUtil.setTaskStateBeforeSave(
                                getDetailsModel().getObjectWrapper(), false, getPageBase(), target);
                        getHelper().onSaveObjectPerformed(target);
                    }
                };
                saveAndRun.showTitleAsLabel(true);
                customButtons.add(saveAndRun);
            }
        });

        return steps;
    }

    private ItemName getActivityName(TaskType task) {
        if (WebComponentUtil.hasArchetypeAssignment(task, SystemObjectsType.ARCHETYPE_LIVE_SYNC_TASK.value())) {
            return WorkDefinitionsType.F_LIVE_SYNCHRONIZATION;
        } else if (WebComponentUtil.hasArchetypeAssignment(task, SystemObjectsType.ARCHETYPE_RECONCILIATION_TASK.value())) {
            return WorkDefinitionsType.F_RECONCILIATION;
        } else if (WebComponentUtil.hasArchetypeAssignment(task, SystemObjectsType.ARCHETYPE_IMPORT_TASK.value())) {
            return WorkDefinitionsType.F_IMPORT;
        }
        return null;
    }
}

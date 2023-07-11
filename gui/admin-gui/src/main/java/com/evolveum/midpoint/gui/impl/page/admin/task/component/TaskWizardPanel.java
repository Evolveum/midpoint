/*
 * Copyright (C) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.task.component;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.component.wizard.WizardModel;
import com.evolveum.midpoint.gui.api.component.wizard.WizardPanel;
import com.evolveum.midpoint.gui.api.component.wizard.WizardStep;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemVisibilityHandler;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.wizard.AbstractFormWizardStepPanel;
import com.evolveum.midpoint.gui.impl.component.wizard.AbstractWizardPanel;
import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.gui.impl.page.admin.task.TaskDetailsModel;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.web.component.prism.ItemVisibility;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

public class TaskWizardPanel extends AbstractWizardPanel<TaskType, TaskDetailsModel> {

    private static final String BASIC_PANEL_TYPE = "tw-basic";
    private static final String SCHEDULE_PANEL_TYPE = "tw-schedule";
    private static final String SIMULATE_PANEL_TYPE = "tw-simulation";
    private static final String DISTRIBUTION_PANEL_TYPE = "tw-distribution";
    private static final String WORK_PANEL_TYPE = "tw-work";

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

        steps.add(new AbstractFormWizardStepPanel<>(getAssignmentHolderModel()) {
            @Override
            protected String getPanelType() {
                return BASIC_PANEL_TYPE;
            }

            @Override
            protected ItemVisibilityHandler getVisibilityHandler() {
                return wrapper -> {
                    if (wrapper.getItemName().equals(TaskType.F_DIAGNOSTIC_INFORMATION)
                            || wrapper.getItemName().equals(TaskType.F_EFFECTIVE_MARK_REF)
                            || wrapper.getItemName().equals(TaskType.F_HANDLER_URI)
                            || wrapper.getItemName().equals(TaskType.F_LIFECYCLE_STATE)
                            || wrapper.getItemName().equals(TaskType.F_INDESTRUCTIBLE)
                            || wrapper.getItemName().equals(TaskType.F_RECURRENCE)){
                        return ItemVisibility.HIDDEN;
                    }
                    return ItemVisibility.AUTO;
                };
            }

            @Override
            protected String getIcon() {
                return "fa fa-wrench";
            }

            @Override
            public IModel<String> getTitle() {
                return createStringResource("TaskWizardPanel.wizard.step.basic");
            }
        });

        TaskType task = getAssignmentHolderModel().getObjectType();
        boolean isImport = WebComponentUtil.hasArchetypeAssignment(task, SystemObjectsType.ARCHETYPE_IMPORT_TASK.value());

        ItemName activityName = getActivityName(task);
        if (activityName != null) {
            steps.add(new AbstractFormWizardStepPanel<>(getAssignmentHolderModel()) {

                @Override
                protected IModel<? extends PrismContainerWrapper> getContainerFormModel() {
                    return PrismContainerWrapperModel.fromContainerWrapper(getAssignmentHolderModel().getObjectWrapperModel(),
                            ItemPath.create(TaskType.F_ACTIVITY, ActivityDefinitionType.F_WORK,
                                    activityName, ReconciliationWorkDefinitionType.F_RESOURCE_OBJECTS));
                }

                @Override
                protected String getPanelType() {
                    return WORK_PANEL_TYPE;
                }

                @Override
                protected ItemVisibilityHandler getVisibilityHandler() {
                    return wrapper -> {
                        if (wrapper.getItemName().equals(ResourceObjectSetType.F_QUERY)
                                || wrapper.getItemName().equals(ResourceObjectSetType.F_QUERY_APPLICATION)){
                            return ItemVisibility.HIDDEN;
                        }
                        return ItemVisibility.AUTO;
                    };
                }

                @Override
                protected String getIcon() {
                    return "fa fa-wrench";
                }

                @Override
                public IModel<String> getTitle() {
                    return createStringResource("TaskWizardPanel.wizard.step.work.resourceObjects");
                }
            });
        }

        //TODO this is not very clean, should be somehow passed to the wizard
        boolean isSimulationTask = task.getActivity().getExecution() != null && ExecutionModeType.PREVIEW == task.getActivity().getExecution().getMode();

        if (isSimulationTask) {
            steps.add(new AbstractFormWizardStepPanel<>(getAssignmentHolderModel()) {

                @Override
                protected IModel<? extends PrismContainerWrapper> getContainerFormModel() {
                    return PrismContainerWrapperModel.fromContainerWrapper(getAssignmentHolderModel().getObjectWrapperModel(), ItemPath.create(TaskType.F_ACTIVITY, ActivityDefinitionType.F_EXECUTION));
                }

                @Override
                protected String getPanelType() {
                    return SIMULATE_PANEL_TYPE;
                }

                @Override
                protected ItemVisibilityHandler getVisibilityHandler() {
                    return super.getVisibilityHandler();
                }

                @Override
                protected String getIcon() {
                    return "fa fa-wrench";
                }

                @Override
                public IModel<String> getTitle() {
                    return createStringResource("TaskWizardPanel.wizard.step.execution");
                }
            });
        }

        if (!isImport) {
            steps.add(new AbstractFormWizardStepPanel<>(getAssignmentHolderModel()) {

                @Override
                protected IModel<? extends PrismContainerWrapper> getContainerFormModel() {
                    return PrismContainerWrapperModel.fromContainerWrapper(getAssignmentHolderModel().getObjectWrapperModel(), TaskType.F_SCHEDULE);
                }

                @Override
                protected String getPanelType() {
                    return SCHEDULE_PANEL_TYPE;
                }

                @Override
                protected ItemVisibilityHandler getVisibilityHandler() {
                    return wrapper -> {
                        if (wrapper.getItemName().equals(ScheduleType.F_EARLIEST_START_TIME)
                                || wrapper.getItemName().equals(ScheduleType.F_LATEST_START_TIME)
                                || wrapper.getItemName().equals(ScheduleType.F_MISFIRE_ACTION)
                                || wrapper.getItemName().equals(ScheduleType.F_RECURRENCE)){
                            return ItemVisibility.HIDDEN;
                        }
                        return ItemVisibility.AUTO;
                    };
                    //cron-like pattern - better help, maybe some example how to configure it
                }

                @Override
                protected String getIcon() {
                    return "fa fa-calendar";
                }

                @Override
                public IModel<String> getTitle() {
                    return createStringResource("TaskWizardPanel.wizard.step.schedule");
                }

            });
        }

        steps.add(new AbstractFormWizardStepPanel<>(getAssignmentHolderModel()) {

            @Override
            protected IModel<? extends PrismContainerWrapper> getContainerFormModel() {
                return PrismContainerWrapperModel.fromContainerWrapper(getAssignmentHolderModel().getObjectWrapperModel(), ItemPath.create(TaskType.F_ACTIVITY, ActivityDefinitionType.F_DISTRIBUTION));
            }

            @Override
            protected String getPanelType() {
                return DISTRIBUTION_PANEL_TYPE;
            }

            @Override
            protected ItemVisibilityHandler getVisibilityHandler() {
                return super.getVisibilityHandler();
            }

            @Override
            protected String getIcon() {
                return "fa fa-wrench";
            }

            @Override
            public IModel<String> getTitle() {
                return createStringResource("TaskWizardPanel.wizard.step.distribution");
            }

            @Override
            protected void onSubmitPerformed(AjaxRequestTarget target) {
                getHelper().onSaveObjectPerformed(target);
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

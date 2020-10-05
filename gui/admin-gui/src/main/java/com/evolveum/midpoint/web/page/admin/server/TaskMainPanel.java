/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.server;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.ComponentConstants;
import com.evolveum.midpoint.gui.api.component.tabs.PanelTab;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemEditabilityHandler;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.gui.api.util.ObjectTabVisibleBehavior;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.prism.panel.SingleContainerPanel;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.component.objectdetails.AssignmentHolderTypeMainPanel;
import com.evolveum.midpoint.web.component.prism.ItemVisibility;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.web.page.admin.PageAdminObjectDetails;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ScheduleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskWorkManagementType;

public class TaskMainPanel extends AssignmentHolderTypeMainPanel<TaskType> {

    private static final String ID_SAVE_AND_RUN = "saveAndRun";

    public TaskMainPanel(String id, LoadableModel<PrismObjectWrapper<TaskType>> objectModel, PageAdminObjectDetails<TaskType> parentPage) {
        super(id, objectModel, parentPage);
    }

    @Override
    protected boolean getOptionsPanelVisibility() {
        return false;
    }

    @Override
    protected void initLayoutButtons(PageAdminObjectDetails<TaskType> parentPage) {
        super.initLayoutButtons(parentPage);
        initLayoutSaveAndRunButton();
    }

    @Override
    protected List<ITab> createTabs(PageAdminObjectDetails<TaskType> parentPage) {
        List<ITab> tabs = new ArrayList<>();

        PageTask parentTaskPage = (PageTask) parentPage;

        createBasicPanel(tabs, parentTaskPage);
        createScheduleTab(tabs, parentTaskPage);
        createWorkManagementTab(tabs, parentTaskPage);
//        createCleanupPoliciesTab(tabs, parentTaskPage);
        createSubtasksTab(tabs, parentTaskPage);
        createOperationStatisticsPanel(tabs, parentTaskPage);
        createEnvironmentalPerformanceTab(tabs, parentTaskPage);
        createOperationTab(tabs, parentTaskPage);
        createInternalPerformanceTab(tabs, parentTaskPage);
        createResultTab(tabs, parentTaskPage);
        createErrorsTab(tabs, parentTaskPage);
        return tabs;
    }

    private void createBasicPanel(List<ITab> tabs, PageTask parentPage) {
        ObjectTabVisibleBehavior<TaskType> basicTabVisibility = new ObjectTabVisibleBehavior<TaskType>
                (Model.of(getObjectWrapper().getObject()), ComponentConstants.UI_TASK_TAB_BASIC_URL, parentPage) {

            private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible() {
                return super.isVisible() && parentPage.getTaskTabVisibilty().isBasicVisible();
            }
        };
        tabs.add(new PanelTab(parentPage.createStringResource("pageTask.basic.title"), basicTabVisibility) {
            private static final long serialVersionUID = 1L;

            @Override
            public WebMarkupContainer createPanel(String panelId) {
                return new TaskBasicTabPanel(panelId, getObjectModel()) {
                    @Override
                    protected void updateHandlerPerformed(AjaxRequestTarget target) {
                        parentPage.refresh(target);
                    }
                };
            }
        });
    }

    private void createScheduleTab(List<ITab> tabs, PageTask parentPage) {
        ObjectTabVisibleBehavior<TaskType> scheduleTabVisibility = new ObjectTabVisibleBehavior<TaskType>
                (Model.of(getObjectWrapper().getObject()), ComponentConstants.UI_TASK_TAB_SCHEDULE_URL, parentPage) {

            private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible() {
                return super.isVisible() && parentPage.getTaskTabVisibilty().isSchedulingVisible();
            }
        };
        tabs.add(new PanelTab(parentPage.createStringResource("pageTask.schedule.title"), scheduleTabVisibility) {
            private static final long serialVersionUID = 1L;

            @Override
            public WebMarkupContainer createPanel(String panelId) {
                return new SingleContainerPanel<ScheduleType>(panelId, PrismContainerWrapperModel.fromContainerWrapper(getObjectModel(), TaskType.F_SCHEDULE), ScheduleType.COMPLEX_TYPE) {

                    @Override
                    protected ItemEditabilityHandler getEditabilityHandler() {
                        return TaskMainPanel.this.getTaskEditabilityHandler();
                    }
                };

            }
        });
    }

    private void createWorkManagementTab(List<ITab> tabs, PageTask parentPage) {
        ObjectTabVisibleBehavior<TaskType> workManagementTabVisibility = new ObjectTabVisibleBehavior<TaskType>
                (Model.of(getObjectWrapper().getObject()), ComponentConstants.UI_TASK_TAB_WORK_MANAGEMENT_URL, parentPage) {

            private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible() {
                return super.isVisible() && parentPage.getTaskTabVisibilty().isWorkManagementVisible(getTask());
            }
        };
        tabs.add(new PanelTab(parentPage.createStringResource("pageTask.workManagement.title"), workManagementTabVisibility) {
            private static final long serialVersionUID = 1L;

            @Override
            public WebMarkupContainer createPanel(String panelId) {
                return new SingleContainerPanel<TaskWorkManagementType>(panelId, PrismContainerWrapperModel.fromContainerWrapper(getObjectModel(), TaskType.F_WORK_MANAGEMENT), TaskWorkManagementType.COMPLEX_TYPE) {

                    @Override
                    protected ItemVisibility getVisibility(ItemPath itemPath) {
                        return getWorkManagementVisibility(itemPath);
                    }

                    @Override
                    protected ItemEditabilityHandler getEditabilityHandler() {
                        return getTaskEditabilityHandler();
                    }
                };
            }
        });
    }

//    private void createCleanupPoliciesTab(List<ITab> tabs, PageTask parentPage) {
//        ObjectTabVisibleBehavior<TaskType> cleanupPoliciesTabVisibility = new ObjectTabVisibleBehavior<TaskType>
//                (Model.of(getObjectWrapper().getObject()), ComponentConstants.UI_TASK_TAB_CLEANUP_POLICIES_URL, parentPage) {
//
//            private static final long serialVersionUID = 1L;
//
//            @Override
//            public boolean isVisible() {
//                return super.isVisible() && parentPage.getTaskTabVisibilty().isCleanupPolicyVisible();
//            }
//        };
//        tabs.add(new PanelTab(parentPage.createStringResource("pageTask.cleanupPolicies.title"), cleanupPoliciesTabVisibility) {
//            private static final long serialVersionUID = 1L;
//
//            @Override
//            public WebMarkupContainer createPanel(String panelId) {
//                ItemVisibilityHandler visibilityHandler = wrapper -> ItemVisibility.AUTO;
//                return createContainerPanel(panelId, TaskType.COMPLEX_TYPE,
//                        PrismContainerWrapperModel.fromContainerWrapper(getObjectModel(), ItemPath.create(TaskType.F_EXTENSION, SchemaConstants.MODEL_EXTENSION_CLEANUP_POLICIES)),
//                        visibilityHandler, getTaskEditabilityHandler());
//            }
//        });
//
//    }

    private void createSubtasksTab(List<ITab> tabs, PageTask parentPage) {
        ObjectTabVisibleBehavior<TaskType> subtasksTabVisibility = new ObjectTabVisibleBehavior<TaskType>
                (Model.of(getObjectWrapper().getObject()), ComponentConstants.UI_TASK_TAB_SUBTASKS_URL, parentPage) {

            private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible() {
                return super.isVisible() && parentPage.getTaskTabVisibilty().isSubtasksAndThreadsVisible(getTask());
            }
        };
        tabs.add(new PanelTab(parentPage.createStringResource("pageTask.subtasks.title"), subtasksTabVisibility) {
            private static final long serialVersionUID = 1L;

            @Override
            public WebMarkupContainer createPanel(String panelId) {
                return new TaskSubtasksAndThreadsTabPanel(panelId, getObjectModel());
            }

        });
    }

    private void createOperationStatisticsPanel(List<ITab> tabs, PageTask parentPage) {
        ObjectTabVisibleBehavior<TaskType> operationStatsAndInternalPerfTabsVisibility = new ObjectTabVisibleBehavior<TaskType>
                (Model.of(getObjectWrapper().getObject()), ComponentConstants.UI_TASK_TAB_OPERATION_STATISTICS_URL, parentPage) {

            private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible() {
                return super.isVisible() && parentPage.getTaskTabVisibilty().isInternalPerformanceVisible();
            }
        };
        tabs.add(new PanelTab(parentPage.createStringResource("pageTask.operationStats.title"), operationStatsAndInternalPerfTabsVisibility) {
            private static final long serialVersionUID = 1L;

            @Override
            public WebMarkupContainer createPanel(String panelId) {
                return new TaskOperationStatisticsPanel(panelId, getObjectModel());
            }

        });
    }

    private void createEnvironmentalPerformanceTab(List<ITab> tabs, PageTask parentPage) {
        ObjectTabVisibleBehavior<TaskType> envPerfTabVisibility = new ObjectTabVisibleBehavior<TaskType>
                (Model.of(getObjectWrapper().getObject()), ComponentConstants.UI_TASK_TAB_ENVIRONMENTAL_PERFORMANCE_URL, parentPage) {

            private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible() {
                return super.isVisible() && parentPage.getTaskTabVisibilty().isEnvironmentalPerformanceVisible();
            }
        };
        tabs.add(new PanelTab(parentPage.createStringResource("pageTask.environmentalPerformance.title"), envPerfTabVisibility) {
            private static final long serialVersionUID = 1L;

            @Override
            public WebMarkupContainer createPanel(String panelId) {
                return new TaskPerformanceTabPanel(panelId, getObjectModel());
            }

        });
    }

    private void createOperationTab(List<ITab> tabs, PageTask parentPage) {
        ObjectTabVisibleBehavior<TaskType> operationTabVisibility = new ObjectTabVisibleBehavior<TaskType>
                (Model.of(getObjectWrapper().getObject()), ComponentConstants.UI_TASK_TAB_OPERATION_URL, parentPage) {

            private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible() {
                return super.isVisible() && parentPage.getTaskTabVisibilty().isOperationVisible();
            }
        };
        tabs.add(new PanelTab(parentPage.createStringResource("pageTaskEdit.operation"), operationTabVisibility) {
            private static final long serialVersionUID = 1L;

            @Override
            public WebMarkupContainer createPanel(String panelId) {
                return new TaskOperationTabPanel(panelId, PrismContainerWrapperModel.fromContainerWrapper(getObjectModel(), TaskType.F_MODEL_OPERATION_CONTEXT));
            }
        });
    }

    private void createInternalPerformanceTab(List<ITab> tabs, PageTask parentPage) {
        ObjectTabVisibleBehavior<TaskType> internalPerfTabsVisibility = new ObjectTabVisibleBehavior<TaskType>
                (Model.of(getObjectWrapper().getObject()), ComponentConstants.UI_TASK_TAB_INTERNAL_PERFORMANCE_URL, parentPage) {

            private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible() {
                return super.isVisible() && parentPage.getTaskTabVisibilty().isInternalPerformanceVisible();
            }
        };
        tabs.add(new PanelTab(parentPage.createStringResource("pageTask.internalPerformance.title"), internalPerfTabsVisibility) {
            private static final long serialVersionUID = 1L;

            @Override
            public WebMarkupContainer createPanel(String panelId) {
                return new TaskInternalPerformanceTabPanel(panelId, PrismContainerWrapperModel.fromContainerWrapper(getObjectModel(), TaskType.F_OPERATION_STATS));
            }
        });
    }

    private void createResultTab(List<ITab> tabs, PageTask parentPage) {
        ObjectTabVisibleBehavior<TaskType> resultTabVisibility = new ObjectTabVisibleBehavior<TaskType>
                (Model.of(getObjectWrapper().getObject()), ComponentConstants.UI_TASK_TAB_RESULT_URL, parentPage) {

            private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible() {
                return super.isVisible() && parentPage.getTaskTabVisibilty().isResultVisible();
            }
        };
        tabs.add(new PanelTab(parentPage.createStringResource("pageTask.result.title"), resultTabVisibility) {
            private static final long serialVersionUID = 1L;

            @Override
            public WebMarkupContainer createPanel(String panelId) {
                return new TaskResultTabPanel(panelId, getObjectModel());
            }
        });
    }

    private void createErrorsTab(List<ITab> tabs, PageTask parentPage) {
        ObjectTabVisibleBehavior<TaskType> errorsTabVisibility = new ObjectTabVisibleBehavior<TaskType>
                (Model.of(getObjectWrapper().getObject()), ComponentConstants.UI_TASK_TAB_ERRORS_URL, parentPage) {

            private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible() {
                return super.isVisible() && parentPage.getTaskTabVisibilty().isErrorsVisible();
            }
        };
        tabs.add(new PanelTab(parentPage.createStringResource("pageTask.errors.title"), errorsTabVisibility) {
            private static final long serialVersionUID = 1L;

            @Override
            public WebMarkupContainer createPanel(String panelId) {
                return new TaskErrorsTabPanel(panelId, getObjectModel());
            }
        });
    }

    private ItemVisibility getWorkManagementVisibility(ItemPath path) {
        TaskType task = getTask();
        String handler = task.getHandlerUri();
        if (handler == null) {
            return ItemVisibility.AUTO;
        }

        if (ItemPath.create(TaskType.F_WORK_MANAGEMENT, TaskWorkManagementType.F_PARTITIONS).equivalent(path)) {
            if (handler.endsWith("task/workers-creation/handler-3")) {
                return ItemVisibility.AUTO;
            } else {
                return ItemVisibility.HIDDEN;
            }
        }

        if (ItemPath.create(TaskType.F_WORK_MANAGEMENT, TaskWorkManagementType.F_WORKERS).equivalent(path)) {
            if (handler.endsWith("task/lightweight-partitioning/handler-3") || handler.endsWith("model/partitioned-focus-validity-scanner/handler-3")
                    || handler.endsWith("model/synchronization/task/partitioned-reconciliation/handler-3") || handler.endsWith("task/generic-partitioning/handler-3")) {
                return ItemVisibility.AUTO;
            }
            return ItemVisibility.HIDDEN;
        }

        return ItemVisibility.AUTO;

    }

    protected void initLayoutSaveAndRunButton() {
        AjaxSubmitButton saveButton = new AjaxSubmitButton(ID_SAVE_AND_RUN, getDetailsPage().createStringResource("TaskMainPanel.button.saveAndRun")) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit(AjaxRequestTarget target) {
                ((PageTask) getDetailsPage()).saveAndRunPerformed(target);
            }

            @Override
            protected void onError(AjaxRequestTarget target) {
                target.add(getDetailsPage().getFeedbackPanel());
            }
        };
        saveButton.add(new VisibleEnableBehaviour() {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible() {
                return !getObjectWrapper().isReadOnly() &&
                        !getDetailsPage().isForcedPreview();
            }

            @Override
            public boolean isEnabled() {
                return !ItemStatus.NOT_CHANGED.equals(getObjectWrapper().getStatus())
                        || getObjectWrapper().canModify();
            }
        });
        saveButton.setOutputMarkupId(true);
        saveButton.setOutputMarkupPlaceholderTag(true);
        getMainForm().add(saveButton);
    }

    private ItemEditabilityHandler getTaskEditabilityHandler() {
        return wrapper -> !WebComponentUtil.isRunningTask(getTask());
    }

    private TaskType getTask() {
        return getObject().asObjectable();
    }
}

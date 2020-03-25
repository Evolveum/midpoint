/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.server;

import com.evolveum.midpoint.gui.api.ComponentConstants;
import com.evolveum.midpoint.gui.api.component.tabs.PanelTab;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.prism.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.prism.PrismObjectWrapper;
import com.evolveum.midpoint.gui.api.util.ObjectTabVisibleBehavior;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.prism.ItemEditabilityHandler;
import com.evolveum.midpoint.gui.impl.prism.ItemMandatoryHandler;
import com.evolveum.midpoint.gui.impl.prism.ItemPanelSettingsBuilder;
import com.evolveum.midpoint.gui.impl.prism.ItemVisibilityHandler;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.component.objectdetails.AbstractObjectMainPanel;
import com.evolveum.midpoint.web.component.objectdetails.AssignmentHolderTypeMainPanel;
import com.evolveum.midpoint.web.component.prism.ItemVisibility;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.web.page.admin.PageAdminObjectDetails;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskWorkManagementType;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import javax.xml.namespace.QName;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TaskMainPanel extends AssignmentHolderTypeMainPanel<TaskType> {

    private static final transient Trace LOGGER = TraceManager.getTrace(TaskMainPanel.class);

    private static final String ID_SAVE_AND_RUN = "saveAndRun";
    private static final String ID_FORM = "taskForm";

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

        ObjectTabVisibleBehavior<TaskType> basicTabVisibility = new ObjectTabVisibleBehavior<TaskType>
                (Model.of(getObjectWrapper().getObject()), ComponentConstants.UI_TASK_TAB_BASIC_URL, parentTaskPage){

            private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible(){
                return super.isVisible() && parentTaskPage.getTaskTabVisibilty().isBasicVisible();
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
//                ItemVisibilityHandler visibilityHandler = wrapper -> getBasicTabVisibility(wrapper.getPath());
//                ItemEditabilityHandler editabilityHandler = wrapper -> getBasicTabEditability(wrapper.getPath());
//                return createContainerPanel(panelId, TaskType.COMPLEX_TYPE, getObjectModel(), visibilityHandler, editabilityHandler);
            }
        });

        ObjectTabVisibleBehavior<TaskType> scheduleTabVisibility = new ObjectTabVisibleBehavior<TaskType>
                (Model.of(getObjectWrapper().getObject()), ComponentConstants.UI_TASK_TAB_SCHEDULE_URL, parentPage){

            private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible(){
                return super.isVisible() && parentTaskPage.getTaskTabVisibilty().isSchedulingVisible();
            }
        };
        tabs.add(new PanelTab(parentPage.createStringResource("pageTask.schedule.title"), scheduleTabVisibility) {
            private static final long serialVersionUID = 1L;

            @Override
            public WebMarkupContainer createPanel(String panelId) {
                ItemVisibilityHandler visibilityHandler = wrapper -> ItemVisibility.AUTO;
                return createContainerPanel(panelId, TaskType.COMPLEX_TYPE, PrismContainerWrapperModel.fromContainerWrapper(getObjectModel(), TaskType.F_SCHEDULE), visibilityHandler, getTaskEditabilityHandler());
            }
        });

        ObjectTabVisibleBehavior<TaskType> workManagementTabVisibility = new ObjectTabVisibleBehavior<TaskType>
                (Model.of(getObjectWrapper().getObject()), ComponentConstants.UI_TASK_TAB_WORK_MANAGEMENT_URL, parentPage){

            private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible(){
                return super.isVisible() && parentTaskPage.getTaskTabVisibilty().isWorkManagementVisible();
            }
        };
        tabs.add(new PanelTab(parentPage.createStringResource("pageTask.workManagement.title"), workManagementTabVisibility) {
            private static final long serialVersionUID = 1L;

            @Override
            public WebMarkupContainer createPanel(String panelId) {
                ItemVisibilityHandler visibilityHandler = wrapper -> getWorkManagementVisibility(wrapper.getPath());
                return createContainerPanel(panelId, TaskType.COMPLEX_TYPE,
                        PrismContainerWrapperModel.fromContainerWrapper(getObjectModel(), TaskType.F_WORK_MANAGEMENT), visibilityHandler, getTaskEditabilityHandler());
            }
        });

//        ObjectTabVisibleBehavior<TaskType> cleanupPoliciesTabVisibility = new ObjectTabVisibleBehavior<TaskType>
//                (Model.of(getObjectWrapper().getObject()), ComponentConstants.UI_TASK_TAB_CLEANUP_POLICIES_URL, parentPage){
//
//            private static final long serialVersionUID = 1L;
//
//            @Override
//            public boolean isVisible(){
//                return super.isVisible() && taskTabsVisibility.isCleanupPolicyVisible();
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

        ObjectTabVisibleBehavior<TaskType> subtasksTabVisibility = new ObjectTabVisibleBehavior<TaskType>
                (Model.of(getObjectWrapper().getObject()), ComponentConstants.UI_TASK_TAB_SUBTASKS_URL, parentPage){

            private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible(){
                return super.isVisible() && parentTaskPage.getTaskTabVisibilty().isSubtasksAndThreadsVisible();
            }
        };
        tabs.add(new PanelTab(parentPage.createStringResource("pageTask.subtasks.title"), subtasksTabVisibility) {
            private static final long serialVersionUID = 1L;

            @Override
            public WebMarkupContainer createPanel(String panelId) {
                return new TaskSubtasksAndThreadsTabPanel(panelId, getObjectModel());
            }

        });

        ObjectTabVisibleBehavior<TaskType> operationStatsAndInternalPerfTabsVisibility = new ObjectTabVisibleBehavior<TaskType>
                (Model.of(getObjectWrapper().getObject()), ComponentConstants.UI_TASK_TAB_OPERATION_STATISTICS_URL, parentPage){

            private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible(){
                return super.isVisible() && parentTaskPage.getTaskTabVisibilty().isInternalPerformanceVisible();
            }
        };
        tabs.add(new PanelTab(parentPage.createStringResource("pageTask.operationStats.title"), operationStatsAndInternalPerfTabsVisibility) {
            private static final long serialVersionUID = 1L;

            @Override
            public WebMarkupContainer createPanel(String panelId) {
                return new TaskOperationStatisticsPanel(panelId, getObjectModel());
            }

        });

        ObjectTabVisibleBehavior<TaskType> envPerfTabVisibility = new ObjectTabVisibleBehavior<TaskType>
                (Model.of(getObjectWrapper().getObject()), ComponentConstants.UI_TASK_TAB_ENVIRONMENTAL_PERFORMANCE_URL, parentPage){

            private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible(){
                return super.isVisible() && parentTaskPage.getTaskTabVisibilty().isEnvironmentalPerformanceVisible();
            }
        };
        tabs.add(new PanelTab(parentPage.createStringResource("pageTask.environmentalPerformance.title"), envPerfTabVisibility) {
            private static final long serialVersionUID = 1L;

            @Override
            public WebMarkupContainer createPanel(String panelId) {
                return new TaskPerformanceTabPanel(panelId, getObjectModel());
            }

        });

        ObjectTabVisibleBehavior<TaskType> operationTabVisibility = new ObjectTabVisibleBehavior<TaskType>
                (Model.of(getObjectWrapper().getObject()), ComponentConstants.UI_TASK_TAB_OPERATION_URL, parentPage){

            private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible(){
                return super.isVisible() && parentTaskPage.getTaskTabVisibilty().isOperationVisible();
            }
        };
        tabs.add(new PanelTab(parentPage.createStringResource("pageTaskEdit.operation"), operationTabVisibility) {
            private static final long serialVersionUID = 1L;

            @Override
            public WebMarkupContainer createPanel(String panelId) {
                return new TaskOperationTabPanel(panelId, PrismContainerWrapperModel.fromContainerWrapper(getObjectModel(), TaskType.F_MODEL_OPERATION_CONTEXT));
            }
        });

        ObjectTabVisibleBehavior<TaskType> internalPerfTabsVisibility = new ObjectTabVisibleBehavior<TaskType>
                (Model.of(getObjectWrapper().getObject()), ComponentConstants.UI_TASK_TAB_INTERNAL_PERFORMANCE_URL, parentPage){

            private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible(){
                return super.isVisible() && parentTaskPage.getTaskTabVisibilty().isInternalPerformanceVisible();
            }
        };
        tabs.add(new PanelTab(parentPage.createStringResource("pageTask.internalPerformance.title"), internalPerfTabsVisibility) {
            private static final long serialVersionUID = 1L;

            @Override
            public WebMarkupContainer createPanel(String panelId) {
                return new TaskInternalPerformanceTabPanel(panelId, PrismContainerWrapperModel.fromContainerWrapper(getObjectModel(), TaskType.F_OPERATION_STATS));
            }
        });

        ObjectTabVisibleBehavior<TaskType> resultTabVisibility = new ObjectTabVisibleBehavior<TaskType>
                (Model.of(getObjectWrapper().getObject()), ComponentConstants.UI_TASK_TAB_RESULT_URL, parentPage){

            private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible(){
                return super.isVisible() && parentTaskPage.getTaskTabVisibilty().isResultVisible();
            }
        };
        tabs.add(new PanelTab(parentPage.createStringResource("pageTask.result.title"), resultTabVisibility) {
            private static final long serialVersionUID = 1L;

            @Override
            public WebMarkupContainer createPanel(String panelId) {
                return new TaskResultTabPanel(panelId, getObjectModel());
            }
        });


        ObjectTabVisibleBehavior<TaskType> errorsTabVisibility = new ObjectTabVisibleBehavior<TaskType>
                (Model.of(getObjectWrapper().getObject()), ComponentConstants.UI_TASK_TAB_ERRORS_URL, parentPage){

            private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible(){
                return super.isVisible() && parentTaskPage.getTaskTabVisibilty().isErrorsVisible();
            }
        };
        tabs.add(new PanelTab(parentPage.createStringResource("pageTask.errors.title"), errorsTabVisibility) {
            private static final long serialVersionUID = 1L;

            @Override
            public WebMarkupContainer createPanel(String panelId) {
                return new TaskErrorsTabPanel(panelId, getObjectModel());
            }
        });

        return tabs;
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
                ((PageTask)getDetailsPage()).saveAndRunPerformed(target);
            }

            @Override
            protected void onError(AjaxRequestTarget target) {
                target.add(getDetailsPage().getFeedbackPanel());
            }
        };
        saveButton.add(new VisibleEnableBehaviour(){
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


    private <C extends Containerable> Panel createContainerPanel(String id, QName typeName, IModel<? extends PrismContainerWrapper<C>> model, ItemVisibilityHandler visibilityHandler, ItemEditabilityHandler editabilityHandler) {
        try {
            ItemPanelSettingsBuilder builder = new ItemPanelSettingsBuilder()
                    .visibilityHandler(visibilityHandler)
                    .editabilityHandler(editabilityHandler)
                    .showOnTopLevel(true);
            Panel panel = getDetailsPage().initItemPanel(id, typeName, model, builder.build());
            return panel;
        } catch (SchemaException e) {
            LOGGER.error("Cannot create panel for {}, {}", typeName, e.getMessage(), e);
            getSession().error("Cannot create panel for " + typeName); // TODO opertion result? localization?
        }

        return null;
    }

    private ItemEditabilityHandler getTaskEditabilityHandler(){
        ItemEditabilityHandler editableHandler = wrapper -> !WebComponentUtil.isRunningTask(getTask());
        return editableHandler;
    }

    private TaskType getTask() {
        return getObject().asObjectable();
    }

}

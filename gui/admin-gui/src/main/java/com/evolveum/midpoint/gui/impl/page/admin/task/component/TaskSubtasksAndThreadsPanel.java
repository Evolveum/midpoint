/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.task.component;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.impl.page.admin.AbstractObjectMainPanel;
import com.evolveum.midpoint.gui.impl.page.admin.task.TaskDetailsModel;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.GetOperationOptionsBuilder;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.util.task.TaskInformation;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.gui.api.component.data.provider.ISelectableDataProvider;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.gui.impl.component.data.provider.SelectableListDataProvider;
import com.evolveum.midpoint.web.page.admin.server.RefreshableTabPanel;
import com.evolveum.midpoint.web.page.admin.server.TaskTablePanel;
import com.evolveum.midpoint.web.page.admin.server.dto.TaskInformationUtil;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ContainerPanelConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

import org.apache.wicket.Component;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.ResourceModel;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author semancik
 */
@PanelType(name = "subtasks")
@PanelInstance(identifier = "subtasks", applicableForType = TaskType.class, applicableForOperation = OperationTypeType.MODIFY,
        display = @PanelDisplay(label = "pageTask.subtasks.title", icon = GuiStyleConstants.CLASS_OBJECT_TASK_ICON, order = 50))
public class TaskSubtasksAndThreadsPanel extends AbstractObjectMainPanel<TaskType, TaskDetailsModel> implements RefreshableTabPanel {
    private static final long serialVersionUID = 1L;

    private static final String ID_WORKER_THREADS_TABLE = "workerThreadsTable";
    private static final String ID_WORKER_THREADS_TABLE_LABEL = "workerThreadsTableLabel";

    private static final String ID_SUBTASKS_LABEL = "subtasksLabel";
    private static final String ID_SUBTASKS_PANEL = "subtasksPanel";

    public TaskSubtasksAndThreadsPanel(String id,
            TaskDetailsModel taskWrapperModel, ContainerPanelConfigurationType config) {
        super(id, taskWrapperModel, config);
        setOutputMarkupId(true);
    }

    private String createTaskKindExpression() {
        return "none"; // FIXME
        //return SelectableBeanImpl.F_VALUE + "." + TaskType.F_WORK_MANAGEMENT.getLocalPart() + "." + TaskWorkManagementType.F_TASK_KIND.getLocalPart();
    }

    protected void initLayout() {
        Label subtasksLabel = new Label(ID_SUBTASKS_LABEL, new ResourceModel("pageTaskEdit.subtasksLabel"));
        add(subtasksLabel);

        TaskTablePanel subtasksPanel = new TaskTablePanel(ID_SUBTASKS_PANEL) {

            @Override
            protected ISelectableDataProvider<SelectableBean<TaskType>> createProvider() {
                return createSelectableBeanObjectDataProvider(() -> createSubtasksQuery(), null, createOperationOptions());
            }

            @Override
            protected List<Component> createToolbarButtonsList(String buttonId) {
                return new ArrayList<>();
            }

            @Override
            protected List<IColumn<SelectableBean<TaskType>, String>> createDefaultColumns() {
                List<IColumn<SelectableBean<TaskType>, String>> columns = super.createDefaultColumns();

                // TODO add "task kind" column

//                columns.add(2, new EnumPropertyColumn<>(createStringResource("SubtasksPanel.label.kind"), createTaskKindExpression()) {
//
//                    @Override
//                    protected String translate(Enum<?> en) {
//                        return createStringResource(en).getString();
//                    }
//                });
                return columns;
            }

            @Override
            protected UserProfileStorage.TableId getTableId() {
                return UserProfileStorage.TableId.TABLE_SUBTASKS;
            }

            @Override
            protected @NotNull TaskInformation getAttachedTaskInformation(SelectableBean<TaskType> selectableTaskBean) {
                return TaskInformationUtil.getOrCreateInfo(selectableTaskBean, getObjectDetailsModels().getRootTaskModelObject());
            }
        };

        add(subtasksPanel);

        Label workerThreadsTableLabel = new Label(ID_WORKER_THREADS_TABLE_LABEL, new ResourceModel("TaskStatePanel.workerThreads"));
        add(workerThreadsTableLabel);

        TaskTablePanel workerThreadsTable = new TaskTablePanel(ID_WORKER_THREADS_TABLE) {

            @Override
            protected ISelectableDataProvider<SelectableBean<TaskType>> createProvider() {
                return new SelectableListDataProvider<>(TaskSubtasksAndThreadsPanel.this, createWorkersModel());
            }

            @Override
            protected UserProfileStorage.TableId getTableId() {
                return UserProfileStorage.TableId.TABLE_WORKERS;
            }

            @Override
            protected boolean isHeaderVisible() {
                return false;
            }

            @Override
            protected List<Component> createToolbarButtonsList(String buttonId) {
                return new ArrayList<>();
            }

            @Override
            protected @NotNull TaskInformation getAttachedTaskInformation(SelectableBean<TaskType> selectableTaskBean) {
                return TaskInformationUtil.getOrCreateInfo(selectableTaskBean, getObjectDetailsModels().getRootTaskModelObject());
            }
        };
        add(workerThreadsTable);
    }

    private ObjectQuery createSubtasksQuery() {
        String parent = getParentIdentifier();
        if (parent == null) {
            return null;
        }
        return getPrismContext().queryFor(TaskType.class)
                .item(TaskType.F_PARENT)
                .eq(parent)
                .build();
    }

    private Collection<SelectorOptions<GetOperationOptions>> createOperationOptions() {
        List<QName> propertiesToGet = new ArrayList<>();
        propertiesToGet.add(TaskType.F_SUBTASK_REF);
        propertiesToGet.add(TaskType.F_NODE_AS_OBSERVED);

        GetOperationOptionsBuilder getOperationOptionsBuilder = getPageBase().getOperationOptionsBuilder();
        getOperationOptionsBuilder = getOperationOptionsBuilder.resolveNames();
        return getOperationOptionsBuilder
                .items(propertiesToGet.toArray(new Object[0])).retrieve()
                .build();
    }

    private IModel<List<TaskType>> createWorkersModel() {
        return () -> {
            PrismObject<TaskType> taskPrism = TaskSubtasksAndThreadsPanel.this.getObjectWrapper().getObject();
            PrismReference subtasks = taskPrism.findReference(TaskType.F_SUBTASK_REF);

            if (subtasks == null) {
                return new ArrayList<>();
            }

            List<TaskType> workers = new ArrayList<>();
            for (PrismReferenceValue val : subtasks.getValues()) {
                if (val.getOid() == null && val.getObject() != null) {
                    workers.add((TaskType) val.getObject().asObjectable());
                }
            }
            return workers;
        };
    }

    private String getParentIdentifier() {
        PrismObject<TaskType> taskPrism = getObjectWrapper().getObject();
        PrismProperty<String> taskIdentifier = taskPrism.findProperty(TaskType.F_TASK_IDENTIFIER);
        if (taskIdentifier == null) {
            return null; //TODO is this valid?
        }

        return taskIdentifier.getRealValue();
    }

    @Override
    public Collection<Component> getComponentsToUpdate() {
        return Collections.singleton(this);
    }

}

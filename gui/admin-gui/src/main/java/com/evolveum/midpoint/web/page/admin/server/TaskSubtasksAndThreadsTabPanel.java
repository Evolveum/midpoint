/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.server;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import javax.xml.namespace.QName;

import org.apache.wicket.Component;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.ResourceModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.GetOperationOptionsBuilder;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.web.component.data.ISelectableDataProvider;
import com.evolveum.midpoint.web.component.data.column.EnumPropertyColumn;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.component.util.SelectableBeanImpl;
import com.evolveum.midpoint.web.component.util.SelectableListDataProvider;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskWorkManagementType;

/**
 * @author semancik
 */
public class TaskSubtasksAndThreadsTabPanel extends BasePanel<PrismObjectWrapper<TaskType>> implements RefreshableTabPanel {
    private static final long serialVersionUID = 1L;

    private static final String ID_WORKER_THREADS_TABLE = "workerThreadsTable";
    private static final String ID_WORKER_THREADS_TABLE_LABEL = "workerThreadsTableLabel";

    private static final String ID_SUBTASKS_LABEL = "subtasksLabel";
    private static final String ID_SUBTASKS_PANEL = "subtasksPanel";


    public TaskSubtasksAndThreadsTabPanel(String id,
                                             LoadableModel<PrismObjectWrapper<TaskType>> taskWrapperModel) {
        super(id, taskWrapperModel);
        setOutputMarkupId(true);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private String createTaskKindExpression() {
        return SelectableBeanImpl.F_VALUE + "." + TaskType.F_WORK_MANAGEMENT.getLocalPart() + "." + TaskWorkManagementType.F_TASK_KIND.getLocalPart();
    }
    private void initLayout() {
        Label subtasksLabel = new Label(ID_SUBTASKS_LABEL, new ResourceModel("pageTaskEdit.subtasksLabel"));
        add(subtasksLabel);

        TaskTablePanel subtasksPanel = new TaskTablePanel(ID_SUBTASKS_PANEL, createOperationOptions()) {
            @Override
            protected ObjectQuery getCustomizeContentQuery() {
                String parent = getParentIdentifier();
                if (parent == null) {
                    return null;
                }
                return getPrismContext().queryFor(TaskType.class)
                        .item(TaskType.F_PARENT)
                        .eq(parent)
                        .build();
            }

            @Override
            protected List<IColumn<SelectableBean<TaskType>, String>> createDefaultColumns() {
                List<IColumn<SelectableBean<TaskType>, String>> columns = super.createDefaultColumns();
                columns.add(2, new EnumPropertyColumn<>(createStringResource("SubtasksPanel.label.kind"), createTaskKindExpression()) {

                    @Override
                    protected String translate(Enum<?> en) {
                        return createStringResource(en).getString();
                    }
                });
                return columns;
            }

            @Override
            protected UserProfileStorage.TableId getTableId() {
                return UserProfileStorage.TableId.TABLE_SUBTASKS;
            }
        };

        add(subtasksPanel);

        Label workerThreadsTableLabel = new Label(ID_WORKER_THREADS_TABLE_LABEL, new ResourceModel("TaskStatePanel.workerThreads"));
        add(workerThreadsTableLabel);

        TaskTablePanel workerThreadsTable = new TaskTablePanel(ID_WORKER_THREADS_TABLE, null) {

            @Override
            protected ISelectableDataProvider<TaskType, SelectableBean<TaskType>> createProvider() {
                return new SelectableListDataProvider<>(TaskSubtasksAndThreadsTabPanel.this, createWorkersModel());
            }

            @Override
            protected UserProfileStorage.TableId getTableId() {
                return UserProfileStorage.TableId.TABLE_WORKERS;
            }

            @Override
            protected boolean isHeaderVisible() {
                return false;
            }
        };
        add(workerThreadsTable);
    }

    private Collection<SelectorOptions<GetOperationOptions>> createOperationOptions() {
        List<QName> propertiesToGet = new ArrayList<>();
        propertiesToGet.add(TaskType.F_SUBTASK_REF);
        propertiesToGet.add(TaskType.F_NODE_AS_OBSERVED);

        GetOperationOptionsBuilder getOperationOptionsBuilder = getSchemaService().getOperationOptionsBuilder();
        getOperationOptionsBuilder = getOperationOptionsBuilder.resolveNames();
        return getOperationOptionsBuilder
                .items(propertiesToGet.toArray(new Object[0])).retrieve()
                .build();
    }

    private IModel<List<TaskType>> createWorkersModel() {
        return (IModel<List<TaskType>>) () -> {
            PrismObject<TaskType> taskPrism = TaskSubtasksAndThreadsTabPanel.this.getModelObject().getObject();
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
        PrismObject<TaskType> taskPrism = getModelObject().getObject();
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

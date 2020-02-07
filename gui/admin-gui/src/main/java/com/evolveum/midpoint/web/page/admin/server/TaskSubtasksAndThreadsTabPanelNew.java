/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.server;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.autocomplete.AutoCompleteQNamePanel;
import com.evolveum.midpoint.gui.api.component.autocomplete.AutoCompleteTextPanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.PrismObjectWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.factory.ItemRealValueModel;
import com.evolveum.midpoint.gui.impl.prism.PrismReferenceValueWrapperImpl;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.web.component.data.BaseSortableDataProvider;
import com.evolveum.midpoint.web.component.data.column.EnumPropertyColumn;
import com.evolveum.midpoint.web.component.form.ValueChoosePanel;
import com.evolveum.midpoint.web.component.input.TextPanel;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.component.util.SelectableBeanImpl;
import com.evolveum.midpoint.web.component.util.SelectableListDataProvider;
import com.evolveum.midpoint.web.model.PrismReferenceWrapperModel;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskWorkManagementType;
import org.apache.wicket.Component;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.*;

import javax.xml.namespace.QName;
import java.util.*;

/**
 * @author semancik
 */
public class TaskSubtasksAndThreadsTabPanelNew extends BasePanel<PrismObjectWrapper<TaskType>> implements TaskTabPanel {
    private static final long serialVersionUID = 1L;

    private static final String ID_WORKER_THREADS_TABLE = "workerThreadsTable";
    private static final String ID_WORKER_THREADS_TABLE_LABEL = "workerThreadsTableLabel";

    private static final String ID_SUBTASKS_LABEL = "subtasksLabel";
    private static final String ID_SUBTASKS_PANEL = "subtasksPanel";


    public TaskSubtasksAndThreadsTabPanelNew(String id,
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

        TaskTablePanel subtasksPanel = new TaskTablePanel(ID_SUBTASKS_PANEL, UserProfileStorage.TableId.TABLE_SUBTASKS, null) {
            @Override
            protected ObjectQuery addFilterToContentQuery(ObjectQuery query) {

                String parent = getParentIdentifier();
                if (parent == null) {
                    return query;
                }

                if (query == null) {
                    query = getPrismContext().queryFactory().createQuery();
                }

                query.addFilter(getPrismContext().queryFor(TaskType.class)
                        .item(TaskType.F_PARENT)
                        .eq(parent)
                        .buildFilter());

                return query;
            }

            @Override
            protected List<IColumn<SelectableBean<TaskType>, String>> createColumns() {
                List<IColumn<SelectableBean<TaskType>, String>> columns = super.createColumns();
                columns.add(2, new EnumPropertyColumn<SelectableBean<TaskType>>(createStringResource("SubtasksPanel.label.kind"), createTaskKindExpression()) {

                    @Override
                    protected String translate(Enum<?> en) {
                        return createStringResource(en).getString();
                    }
                });
                return columns;
            }

            @Override
            protected WebMarkupContainer createTableButtonToolbar(String id) {
                return null;
            }
        };

        add(subtasksPanel);

        Label workerThreadsTableLabel = new Label(ID_WORKER_THREADS_TABLE_LABEL, new ResourceModel("TaskStatePanel.workerThreads"));
        add(workerThreadsTableLabel);

        TaskTablePanel workerThreadsTable = new TaskTablePanel(ID_WORKER_THREADS_TABLE, UserProfileStorage.TableId.TABLE_WORKERS, null) {

            @Override
            protected BaseSortableDataProvider<SelectableBean<TaskType>> initProvider() {
                return new SelectableListDataProvider<>(TaskSubtasksAndThreadsTabPanelNew.this, createWorkersModel());
            }

            @Override
            protected WebMarkupContainer createTableButtonToolbar(String id) {
                return null;
            }

            @Override
            protected boolean isHeaderVisible() {
                return false;
            }
        };
        add(workerThreadsTable);
    }

    private IModel<List<TaskType>> createWorkersModel() {
        return (IModel<List<TaskType>>) () -> {
            PrismObject<TaskType> taskPrism = TaskSubtasksAndThreadsTabPanelNew.this.getModelObject().getObject();
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
        return Collections.<Component>singleton(this);
    }

}

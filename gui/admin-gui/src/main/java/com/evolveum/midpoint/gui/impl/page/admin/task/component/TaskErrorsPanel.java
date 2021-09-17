/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.task.component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import javax.xml.namespace.QName;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.page.admin.AbstractObjectMainPanel;
import com.evolveum.midpoint.gui.impl.page.admin.task.TaskDetailsModel;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectOrdering;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.OrderDirection;
import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.component.data.BoxedTablePanel;
import com.evolveum.midpoint.web.component.data.SelectableBeanObjectDataProvider;
import com.evolveum.midpoint.web.component.data.column.AjaxLinkColumn;
import com.evolveum.midpoint.web.component.data.column.EnumPropertyColumn;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.page.admin.server.RefreshableTabPanel;
import com.evolveum.midpoint.web.page.admin.server.dto.TaskErrorSelectableBeanImpl;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Created by honchar.
 */
@PanelType(name = "taskErrors")
@PanelInstance(identifier = "taskErrors", applicableForType = TaskType.class, applicableForOperation = OperationTypeType.MODIFY,
        display = @PanelDisplay(label = "pageTask.errors.title", order = 50))
public class TaskErrorsPanel extends AbstractObjectMainPanel<TaskType, TaskDetailsModel> implements RefreshableTabPanel {
    private static final long serialVersionUID = 1L;

    private static final String ID_TASK_ERRORS = "taskErrors";

    public TaskErrorsPanel(String id, TaskDetailsModel taskWrapperModel, ContainerPanelConfigurationType config) {
        super(id, taskWrapperModel, config);

    }

    protected void initLayout() {

        SelectableBeanObjectDataProvider<? extends ObjectType> provider = new SelectableBeanObjectDataProvider<>(this, null) {

            @Override
            protected String getDefaultSortParam() {
                return TaskErrorSelectableBeanImpl.F_ERROR_TIMESTAMP;
            }

            @Override
            protected SortOrder getDefaultSortOrder() {
                return SortOrder.DESCENDING;
            }

            @Override
            public SelectableBean<ObjectType> createDataObjectWrapper(ObjectType obj) {
                return new TaskErrorSelectableBeanImpl<>(obj, getObjectWrapper().getOid());
            }

            @Override
            public ObjectQuery getQuery() {
                return createContentQuery(getObjectWrapper().getOid(), getPageBase());
            }

            @NotNull
            @Override
            protected List<ObjectOrdering> createObjectOrderings(SortParam<String> sortParam) {
                if (sortParam != null && sortParam.getProperty() != null) {
                    OrderDirection order = sortParam.isAscending() ? OrderDirection.ASCENDING : OrderDirection.DESCENDING;
                    ItemPath ordering;
                    if (sortParam.getProperty().equals(TaskErrorSelectableBeanImpl.F_ERROR_TIMESTAMP)) {
                        ordering = ItemPath.create("operationExecution", "timestamp");
                    } else {
                        ordering = ItemPath.create(new QName(SchemaConstantsGenerated.NS_COMMON, sortParam.getProperty()));
                    }
                    return Collections.singletonList(
                            getPrismContext().queryFactory().createOrdering(
                                    ordering, order));
                } else {
                    return Collections.emptyList();
                }
            }

            @Override
            public Class<ObjectType> getType() {
                return ObjectType.class;
            }
        };

        BoxedTablePanel<TaskErrorSelectableBeanImpl<ObjectType>> table = new BoxedTablePanel<>(ID_TASK_ERRORS, provider, initColumns());
        table.setOutputMarkupId(true);
        add(table);

    }

    private List<IColumn<TaskErrorSelectableBeanImpl<ObjectType>, String>> initColumns() {
        List<IColumn<TaskErrorSelectableBeanImpl<ObjectType>, String>> columns = new ArrayList<>();
        columns.add(new PropertyColumn<>(createStringResource("pageTaskEdit.taskErros.objectName"), TaskErrorSelectableBeanImpl.F_OBJECT_REF_NAME) {
            @Override
            public String getSortProperty() {
                return "name";
            }
        });
        columns.add(new AbstractColumn<>(createStringResource("pageTaskEdit.taskErros.timestamp"), TaskErrorSelectableBeanImpl.F_ERROR_TIMESTAMP) {
            private static final long serialVersionUID = 1L;

            @Override
            public void populateItem(Item<ICellPopulator<TaskErrorSelectableBeanImpl<ObjectType>>> cellItem, String componentId,
                    IModel<TaskErrorSelectableBeanImpl<ObjectType>> rowModel) {
                Label label = new Label(componentId, (IModel<String>) () -> WebComponentUtil.getShortDateTimeFormattedValue(rowModel.getObject().getErrorTimestamp(), getPageBase()));
                cellItem.add(label);
            }

        });
        columns.add(new EnumPropertyColumn<>(createStringResource("pageTaskEdit.taskErros.status"), TaskErrorSelectableBeanImpl.F_STATUS));
        columns.add(new PropertyColumn<>(createStringResource("pageTaskEdit.taskErros.message"), TaskErrorSelectableBeanImpl.F_MESSAGE));
        columns.add(new EnumPropertyColumn<>(createStringResource("pageTaskEdit.taskErros.recordType"), TaskErrorSelectableBeanImpl.F_RECORD_TYPE));
        columns.add(new AjaxLinkColumn<>(createStringResource("pageTaskEdit.taskErros.realOwner"), TaskErrorSelectableBeanImpl.F_REAL_OWNER_DESCRIPTION) {

            @Override
            public void onClick(AjaxRequestTarget target, IModel<TaskErrorSelectableBeanImpl<ObjectType>> rowModel) {
                TaskErrorSelectableBeanImpl<ObjectType> object = rowModel.getObject();
                PrismObject<ObjectType> realOwner = object.getRealOwner();
                WebComponentUtil.dispatchToObjectDetailsPage(realOwner.getCompileTimeClass(), realOwner.getOid(), TaskErrorsPanel.this, false);
            }
        });

        return columns;
    }

    private ObjectQuery createContentQuery(String taskOid, PageBase pageBase) {
        return pageBase.getPrismContext().queryFor(ObjectType.class)
                .exists(ObjectType.F_OPERATION_EXECUTION)
                .block()
                .item(OperationExecutionType.F_TASK_REF).ref(taskOid)
                .and()
                .block().item(OperationExecutionType.F_STATUS)
                .eq(OperationResultStatusType.FATAL_ERROR)
                .or().item(OperationExecutionType.F_STATUS)
                .eq(OperationResultStatusType.PARTIAL_ERROR)
                .or().item(OperationExecutionType.F_STATUS)
                .eq(OperationResultStatusType.WARNING)
                .endBlock()
                .endBlock()
                .build();
    }

    @Override
    public Collection<Component> getComponentsToUpdate() {
        return Collections.singleton(get(ID_TASK_ERRORS));
    }

    @Override
    protected void detachModel() {
        super.detachModel();
        getObjectWrapperModel().reset();
    }
}

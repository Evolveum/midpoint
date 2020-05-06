/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.server;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectOrdering;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.OrderDirection;
import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.web.component.data.BoxedTablePanel;
import com.evolveum.midpoint.web.component.data.SelectableBeanObjectDataProvider;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.page.admin.server.dto.TaskErrorSelectableBeanImpl;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationExecutionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
import org.apache.wicket.Component;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Created by honchar.
 */
public class TaskErrorsTabPanel extends BasePanel<PrismObjectWrapper<TaskType>> implements RefreshableTabPanel {
    private static final long serialVersionUID = 1L;

    private static final String ID_TASK_ERRORS = "taskErrors";

    public TaskErrorsTabPanel(String id, LoadableModel<PrismObjectWrapper<TaskType>> taskWrapperModel) {
        super(id, taskWrapperModel);

    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
        setOutputMarkupId(true);
    }

    private void initLayout() {


        SelectableBeanObjectDataProvider<? extends ObjectType> provider = new SelectableBeanObjectDataProvider<ObjectType>(this, ObjectType.class, null) {

            @Override
            public SelectableBean<ObjectType> createDataObjectWrapper(ObjectType obj) {
                return new TaskErrorSelectableBeanImpl<>(obj, getModelObject().getOid());
            }

            @Override
            public ObjectQuery getQuery() {
                return createContentQuery(getModelObject().getOid(), getPageBase());
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
        };

        BoxedTablePanel<TaskErrorSelectableBeanImpl<ObjectType>> table = new BoxedTablePanel<>(ID_TASK_ERRORS, provider, initColumns());
        table.setOutputMarkupId(true);
        add(table);

    }

    private List<IColumn<TaskErrorSelectableBeanImpl<ObjectType>, String>> initColumns() {
        List<IColumn<TaskErrorSelectableBeanImpl<ObjectType>, String>> columns = new ArrayList<>();
        columns.add(new PropertyColumn<TaskErrorSelectableBeanImpl<ObjectType>, String>(createStringResource("pageTaskEdit.taskErros.objectName"), TaskErrorSelectableBeanImpl.F_OBJECT_REF_NAME){
            @Override
            public String getSortProperty() {
                return "name";
            }
        });
        columns.add(new PropertyColumn<>(createStringResource("pageTaskEdit.taskErros.status"), TaskErrorSelectableBeanImpl.F_STATUS));
        columns.add(new AbstractColumn<TaskErrorSelectableBeanImpl<ObjectType>, String>(createStringResource("pageTaskEdit.taskErros.timestamp"), TaskErrorSelectableBeanImpl.F_ERROR_TIMESTAMP){
            private static final long serialVersionUID = 1L;

            @Override
            public void populateItem(Item<ICellPopulator<TaskErrorSelectableBeanImpl<ObjectType>>> cellItem, String componentId,
                                     IModel<TaskErrorSelectableBeanImpl<ObjectType>> rowModel) {
                Label label = new Label(componentId, (IModel<String>) () -> WebComponentUtil.getShortDateTimeFormattedValue(rowModel.getObject().getErrorTimestamp(), getPageBase()));
                cellItem.add(label);
            }

        });
        columns.add(new PropertyColumn<>(createStringResource("pageTaskEdit.taskErros.message"), TaskErrorSelectableBeanImpl.F_MESSAGE));
        return columns;
    }

    private ObjectQuery createContentQuery(String taskOid, PageBase pageBase){
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
        ((LoadableModel) getModel()).reset();
    }
}

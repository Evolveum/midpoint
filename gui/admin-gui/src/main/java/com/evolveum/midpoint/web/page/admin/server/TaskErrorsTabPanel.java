/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.server;

import java.util.*;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.schema.result.OperationResult;
import org.apache.wicket.Component;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.PrismObjectWrapper;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectOrdering;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.OrderDirection;
import com.evolveum.midpoint.web.component.data.ObjectDataProvider;
import com.evolveum.midpoint.web.component.data.TablePanel;
import com.evolveum.midpoint.web.component.form.Form;
import com.evolveum.midpoint.web.component.objectdetails.AbstractObjectTabPanel;
import com.evolveum.midpoint.web.page.admin.server.dto.TaskDto;
import com.evolveum.midpoint.web.page.admin.server.dto.TaskErrorDto;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationExecutionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

/**
 * Created by honchar.
 */
public class TaskErrorsTabPanel extends AbstractObjectTabPanel<TaskType> implements TaskTabPanel {
    private static final long serialVersionUID = 1L;

    private static final String ID_TASK_ERRORS = "taskErrors";

    private IModel<TaskDto> taskDtoModel;

    public TaskErrorsTabPanel(String id, Form mainForm,
                              LoadableModel<PrismObjectWrapper<TaskType>> taskWrapperModel,
                              IModel<TaskDto> taskDtoModel) {
        super(id, mainForm, taskWrapperModel);
        this.taskDtoModel = taskDtoModel;
        setOutputMarkupId(true);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        ObjectDataProvider<TaskErrorDto, ObjectType> provider = new ObjectDataProvider<TaskErrorDto, ObjectType>
                (TaskErrorsTabPanel.this, ObjectType.class) {
            private static final long serialVersionUID = 1L;

            @Override
            public TaskErrorDto createDataObjectWrapper(PrismObject<ObjectType> obj) {
                return convertToTaskErrorDto(obj.asObjectable(), taskDtoModel);
            }

            @Override
            public void setQuery(ObjectQuery query) {

                super.setQuery(query);
            }

            @Override
            public ObjectQuery getQuery() {
                return createContentQuery(taskDtoModel.getObject().getOid(), getPageBase());
            }

            protected List<ObjectOrdering> createObjectOrderings(SortParam<String> sortParam) {
                if (sortParam != null && sortParam.getProperty() != null) {
                    OrderDirection order = sortParam.isAscending() ? OrderDirection.ASCENDING : OrderDirection.DESCENDING;
                    ItemPath ordering = null;
                    if (sortParam.getProperty().equals(TaskErrorDto.F_ERROR_TIMESTAMP)) {
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
        TablePanel resultTablePanel = new TablePanel<>(ID_TASK_ERRORS, provider, initColumns());
        resultTablePanel.setStyle("padding-top: 0px;");
        resultTablePanel.setShowPaging(false);
        resultTablePanel.setOutputMarkupId(true);
        add(resultTablePanel);

    }

    private TaskErrorDto convertToTaskErrorDto(ObjectType object, IModel<TaskDto> taskDtoModel){
        return new TaskErrorDto(object, taskDtoModel.getObject().getOid());
    }
    private List<IColumn<TaskErrorDto, String>> initColumns() {
        List<IColumn<TaskErrorDto, String>> columns = new ArrayList<>();
        columns.add(new PropertyColumn<TaskErrorDto, String>(createStringResource("pageTaskEdit.taskErros.objectName"), TaskErrorDto.F_OBJECT_REF_NAME){
            @Override
            public String getSortProperty() {
                return "name";
            }
        });
        columns.add(new PropertyColumn<>(createStringResource("pageTaskEdit.taskErros.status"), TaskErrorDto.F_STATUS));
        columns.add(new AbstractColumn<TaskErrorDto, String>(createStringResource("pageTaskEdit.taskErros.timestamp"), TaskErrorDto.F_ERROR_TIMESTAMP){
            private static final long serialVersionUID = 1L;

            @Override
            public void populateItem(Item<ICellPopulator<TaskErrorDto>> cellItem, String componentId,
                                     IModel<TaskErrorDto> rowModel) {
                Label label = new Label(componentId, new IModel<String>() {
                    @Override
                    public String getObject() {
                        return WebComponentUtil.getShortDateTimeFormattedValue(rowModel.getObject().getErrorTimestamp(), getPageBase());
                    }
                });
                cellItem.add(label);
            }

//            TODO:uncomment after fixing of MID-5748
//            @Override
//            public String getSortProperty() {
//                return "timestamp";
//            }
        });
        columns.add(new PropertyColumn<>(createStringResource("pageTaskEdit.taskErros.message"), TaskErrorDto.F_MESSAGE));
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

}

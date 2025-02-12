/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.server;

import java.io.Serial;
import java.util.*;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.schema.util.task.TaskTypeUtil;
import com.evolveum.midpoint.authentication.api.authorization.AuthorizationAction;
import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;
import com.evolveum.midpoint.web.application.*;
import com.evolveum.midpoint.gui.api.component.data.provider.ISelectableDataProvider;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.export.AbstractExportableColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.GetOperationOptionsBuilder;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.web.component.DateLabelComponent;
import com.evolveum.midpoint.web.component.data.column.ObjectReferenceColumn;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.component.util.SelectableBeanImpl;
import com.evolveum.midpoint.web.page.admin.PageAdmin;
import com.evolveum.midpoint.web.session.UserProfileStorage;

@PageDescriptor(
        urls = {
                @Url(mountUrl = "/admin/tasks", matchUrlForSecurity = "/admin/tasks")
        },
        action = {
                @AuthorizationAction(actionUri = PageAdminTasks.AUTHORIZATION_TASKS_ALL,
                        label = PageAdminTasks.AUTH_TASKS_ALL_LABEL,
                        description = PageAdminTasks.AUTH_TASKS_ALL_DESCRIPTION),
                @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_TASKS_URL,
                        label = "PageTasks.auth.tasks.label",
                        description = "PageTasks.auth.tasks.description"),
                @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_TASKS_VIEW_URL,
                        label = "PageTasks.auth.tasks.view.label",
                        description = "PageTasks.auth.tasks.view.description") })
@CollectionInstance(identifier = "allTasks", applicableForType = TaskType.class, applicableForOperation = OperationTypeType.MODIFY,
        display = @PanelDisplay(label = "PageAdmin.menu.top.tasks.list", singularLabel = "ObjectType.task", icon = GuiStyleConstants.CLASS_OBJECT_TASK_ICON))
public class PageTasks extends PageAdmin {
    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_TABLE = "table";

    public static final long WAIT_FOR_TASK_STOP = 2000L;

    public PageTasks() {
        this(null);
    }

    public PageTasks(PageParameters params) {
        this(null, params);
    }

    public PageTasks(ObjectQuery predefinedQuery, PageParameters params) {
        super(params);

        TaskTablePanel tablePanel = new TaskTablePanel(ID_TABLE) {

            @Override
            protected ISelectableDataProvider<SelectableBean<TaskType>> createProvider() {
                return createSelectableBeanObjectDataProvider(() -> getTaskQuery(predefinedQuery), null, createOperationOptions());
            }

            @Override
            protected List<IColumn<SelectableBean<TaskType>, String>> createDefaultColumns() {
                List<IColumn<SelectableBean<TaskType>, String>> columns = super.createDefaultColumns();
                addCustomColumns(columns);
                return columns;
            }

            @Override
            protected UserProfileStorage.TableId getTableId() {
                return UserProfileStorage.TableId.TABLE_TASKS;
            }
        };
        add(tablePanel);
    }

    private ObjectQuery getTaskQuery(ObjectQuery predefinedQuery) {
        ObjectQuery query = getPrismContext().queryFor(TaskType.class)
                .item(TaskType.F_PARENT)
                .isNull()
                .build();
        if (predefinedQuery != null) {
            query.addFilter(predefinedQuery.getFilter());
        }
        return query;
    }

    private void addCustomColumns(List<IColumn<SelectableBean<TaskType>, String>> columns) {
        columns.add(0, new ObjectReferenceColumn<>(createStringResource("pageTasks.task.objectRef"), SelectableBeanImpl.F_VALUE + "." + TaskType.F_OBJECT_REF.getLocalPart()) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public IModel<List<ObjectReferenceType>> extractDataModel(IModel<SelectableBean<TaskType>> rowModel) {
                SelectableBean<TaskType> bean = rowModel.getObject();
                ObjectReferenceType objectRef = bean.getValue().getObjectRef();
                if (objectRef != null) {
                    objectRef.asReferenceValue().clearParent();
                }
                return Model.ofList(Collections.singletonList(objectRef));
            }
        });
    }

    private Collection<SelectorOptions<GetOperationOptions>> createOperationOptions() {
        List<QName> propertiesToGet = new ArrayList<>();
        propertiesToGet.add(TaskType.F_NODE_AS_OBSERVED);
        propertiesToGet.add(TaskType.F_NEXT_RUN_START_TIMESTAMP);
        propertiesToGet.add(TaskType.F_NEXT_RETRY_TIMESTAMP);

        GetOperationOptionsBuilder getOperationOptionsBuilder = getSchemaService().getOperationOptionsBuilder();
        getOperationOptionsBuilder = getOperationOptionsBuilder.resolveNames();
        return getOperationOptionsBuilder
                .items(propertiesToGet.toArray(new Object[0])).retrieve()
                .build();
    }

    private Date getCurrentRuntime(IModel<SelectableBean<TaskType>> taskModel) {
        TaskType task = taskModel.getObject().getValue();

        if (task.getExecutionState() == TaskExecutionStateType.CLOSED) {

            Long time = getCompletionTimestamp(task);
            if (time == null) {
                return null;
            }
            return new Date(time);

        }
        return null;
    }

    public Long getCompletionTimestamp(TaskType taskType) {
        return xgc2long(taskType.getCompletionTimestamp());
    }

    private Long xgc2long(XMLGregorianCalendar gc) {
        return gc != null ? XmlTypeConverter.toMillis(gc) : null;
    }

    @Override
    protected List<String> pageParametersToBeRemoved() {
        return List.of(PageBase.PARAMETER_SEARCH_BY_NAME);
    }
}

/*
 * Copyright (c) 2010-2017 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evolveum.midpoint.web.page.admin.server;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.builder.QueryBuilder;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.data.ObjectDataProvider;
import com.evolveum.midpoint.web.component.data.TablePanel;
import com.evolveum.midpoint.web.component.form.Form;
import com.evolveum.midpoint.web.component.objectdetails.AbstractObjectTabPanel;
import com.evolveum.midpoint.web.component.prism.ObjectWrapper;
import com.evolveum.midpoint.web.page.admin.server.dto.TaskDto;
import com.evolveum.midpoint.web.page.admin.server.dto.TaskErrorDto;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.wicket.Component;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.model.IModel;

import java.util.*;

/**
 * Created by honchar.
 */
public class TaskErrorsTabPanel extends AbstractObjectTabPanel<TaskType> implements TaskTabPanel {
    private static final long serialVersionUID = 1L;

    private static final String ID_TASK_ERRORS = "taskErrors";

    public TaskErrorsTabPanel(String id, Form mainForm,
                              LoadableModel<ObjectWrapper<TaskType>> taskWrapperModel,
                              IModel<TaskDto> taskDtoModel, PageBase pageBase) {
        super(id, mainForm, taskWrapperModel, pageBase);
        initLayout(taskDtoModel, pageBase);
        setOutputMarkupId(true);
    }

    private void initLayout(final IModel<TaskDto> taskDtoModel, PageBase pageBase) {
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
                return createContentQuery(taskDtoModel.getObject().getOid(), pageBase);
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
        List<IColumn<TaskErrorDto, String>> columns = new ArrayList<IColumn<TaskErrorDto, String>>();
        columns.add(new PropertyColumn<>(createStringResource("pageTaskEdit.taskErros.objectName"), TaskErrorDto.F_OBJECT_REF_NAME));
        columns.add(new PropertyColumn<>(createStringResource("pageTaskEdit.taskErros.status"), TaskErrorDto.F_STATUS));
        columns.add(new PropertyColumn<>(createStringResource("pageTaskEdit.taskErros.message"), TaskErrorDto.F_MESSAGE));
        return columns;
    }

    private ObjectQuery createContentQuery(String taskOid, PageBase pageBase){
        return QueryBuilder.queryFor(ObjectType.class, pageBase.getPrismContext())
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

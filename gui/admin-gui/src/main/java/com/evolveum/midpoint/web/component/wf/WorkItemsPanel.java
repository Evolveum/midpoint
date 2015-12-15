/*
 * Copyright (c) 2010-2013 Evolveum
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

package com.evolveum.midpoint.web.component.wf;

import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.web.component.data.TablePanel;
import com.evolveum.midpoint.web.component.data.column.LinkColumn;
import com.evolveum.midpoint.web.component.util.ListDataProvider;
import com.evolveum.midpoint.web.component.util.SimplePanel;
import com.evolveum.midpoint.web.page.PageBase;
import com.evolveum.midpoint.web.page.admin.home.dto.MyWorkItemDto;
import com.evolveum.midpoint.web.page.admin.workflow.PageWorkItem;
import com.evolveum.midpoint.web.page.admin.workflow.dto.WorkItemDto;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import java.util.ArrayList;
import java.util.List;

/**
 * @author lazyman
 * @author mederly
 */
public class WorkItemsPanel extends SimplePanel<List<WorkItemDto>> {

    private static final String ID_WORK_ITEMS_TABLE = "workItemsTable";

    public WorkItemsPanel(String id, IModel<List<WorkItemDto>> model) {
        super(id, model);
        initLayoutLocal(true);
    }

    public WorkItemsPanel(String id, IModel<List<WorkItemDto>> model, boolean showAssigned) {
        super(id, model);
        initLayoutLocal(showAssigned);
    }

    // this is called locally in order to take showAssigned into account
    private void initLayoutLocal(boolean showAssigned) {
        List<IColumn<WorkItemDto, String>> columns = new ArrayList<IColumn<WorkItemDto, String>>();
        if (WebMiscUtil.isAuthorized(AuthorizationConstants.AUTZ_UI_WORK_ITEMS_ALL_URL,
                AuthorizationConstants.AUTZ_UI_WORK_ITEM_URL)) {
            columns.add(new LinkColumn<WorkItemDto>(createStringResource("WorkItemsPanel.name"), MyWorkItemDto.F_NAME, MyWorkItemDto.F_NAME) {

                @Override
                public void onClick(AjaxRequestTarget target, IModel<WorkItemDto> rowModel) {
                    WorkItemDto workItemDto = rowModel.getObject();
                    PageParameters parameters = new PageParameters();
                    parameters.add(OnePageParameterEncoder.PARAMETER, workItemDto.getWorkItem().getWorkItemId());
                    setResponsePage(new PageWorkItem(parameters, (PageBase) WorkItemsPanel.this.getPage()));
                }
            });
        } else {
            columns.add(new AbstractColumn<WorkItemDto, String>(createStringResource("WorkItemsPanel.name")) {
                @Override
                public void populateItem(Item<ICellPopulator<WorkItemDto>> item, String componentId,
                                         final IModel<WorkItemDto> rowModel) {
                    item.add(new Label(componentId, new AbstractReadOnlyModel<Object>() {

                        @Override
                        public Object getObject() {
                            WorkItemDto dto = rowModel.getObject();
                            return dto.getName();
                        }
                    }));
                }
            });
        }


        if (showAssigned) {
            columns.add(new PropertyColumn(createStringResource("WorkItemsPanel.assigned"), WorkItemDto.F_OWNER_OR_CANDIDATES));
        }
        columns.add(new PropertyColumn(createStringResource("WorkItemsPanel.created"), WorkItemDto.F_CREATED));

        ISortableDataProvider provider = new ListDataProvider(this, getModel());
        TablePanel accountsTable = new TablePanel<WorkItemDto>(ID_WORK_ITEMS_TABLE, provider, columns);
        add(accountsTable);
    }
}

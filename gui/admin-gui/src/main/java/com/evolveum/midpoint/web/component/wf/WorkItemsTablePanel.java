/*
 * Copyright (c) 2010-2016 Evolveum
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

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.web.component.data.TablePanel;
import com.evolveum.midpoint.web.component.data.column.CheckBoxHeaderColumn;
import com.evolveum.midpoint.web.component.data.column.LinkColumn;
import com.evolveum.midpoint.web.page.admin.workflow.PageWorkItem;
import com.evolveum.midpoint.web.page.admin.workflow.dto.WorkItemDtoProvider;
import com.evolveum.midpoint.web.page.admin.workflow.dto.WorkItemDto;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.*;
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
public class WorkItemsTablePanel extends BasePanel {

    private static final String ID_WORK_ITEMS_TABLE = "workItemsTable";

    private ISortableDataProvider<WorkItemDto, String> provider;

    public WorkItemsTablePanel(String id, ISortableDataProvider<WorkItemDto, String> provider,
            UserProfileStorage.TableId tableId, long pageSize, boolean showAssigned) {
        super(id);
        this.provider = provider;
        initLayout(tableId, pageSize, showAssigned);
    }

    // this is called locally in order to take showAssigned into account
    private void initLayout(UserProfileStorage.TableId tableId, long pageSize, boolean showAssigned) {
        List<IColumn<WorkItemDto, String>> columns = new ArrayList<>();

        // TODO configurable
        columns.add(new CheckBoxHeaderColumn<WorkItemDto>());

        // TODO clickable links and info icons
        columns.add(new PropertyColumn(createStringResource("WorkItemsPanel.object"), WorkItemDto.F_OBJECT_NAME));
        columns.add(new PropertyColumn(createStringResource("WorkItemsPanel.target"), WorkItemDto.F_TARGET_NAME));

        if (WebComponentUtil.isAuthorized(AuthorizationConstants.AUTZ_UI_WORK_ITEMS_ALL_URL,
                AuthorizationConstants.AUTZ_UI_WORK_ITEM_URL)) {
            columns.add(new LinkColumn<WorkItemDto>(createStringResource("WorkItemsPanel.name"), WorkItemDto.F_NAME, WorkItemDto.F_NAME) {
                @Override
                public void onClick(AjaxRequestTarget target, IModel<WorkItemDto> rowModel) {
                    PageParameters parameters = new PageParameters();
                    parameters.add(OnePageParameterEncoder.PARAMETER, rowModel.getObject().getWorkItemId());
                    setResponsePage(new PageWorkItem(parameters, (PageBase) WorkItemsTablePanel.this.getPage()));
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
                            return rowModel.getObject().getName();
                        }
                    }));
                }
            });
        }

        columns.add(new PropertyColumn(createStringResource("WorkItemsPanel.started"), WorkItemDto.F_PROCESS_STARTED));
        columns.add(new PropertyColumn(createStringResource("WorkItemsPanel.created"), WorkItemDto.F_CREATED));
        if (showAssigned) {
            columns.add(new PropertyColumn(createStringResource("WorkItemsPanel.assigned"), WorkItemDto.F_ASSIGNEE_OR_CANDIDATES));
        }

        TablePanel workItemsTable = new TablePanel<>(ID_WORK_ITEMS_TABLE, provider, columns, tableId, pageSize);
        add(workItemsTable);
    }

    private TablePanel getWorkItemTable() {
        return (TablePanel) get(ID_WORK_ITEMS_TABLE);
    }

    public List<WorkItemDto> getSelectedWorkItems() {
        DataTable table = getWorkItemTable().getDataTable();
        WorkItemDtoProvider provider = (WorkItemDtoProvider) table.getDataProvider();

        List<WorkItemDto> selected = new ArrayList<>();
        for (WorkItemDto row : provider.getAvailableData()) {
            if (row.isSelected()) {
                selected.add(row);
            }
        }

        return selected;
    }

}

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
import com.evolveum.midpoint.web.component.data.column.LinkColumn;
import com.evolveum.midpoint.web.component.util.ListDataProvider;
import com.evolveum.midpoint.web.page.admin.workflow.PageWorkItem;
import com.evolveum.midpoint.web.page.admin.workflow.dto.WorkItemNewDto;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
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
public class WorkItemsPanel extends BasePanel<List<WorkItemNewDto>> {

    private static final String ID_WORK_ITEMS_TABLE = "workItemsTable";

    public WorkItemsPanel(String id, IModel<List<WorkItemNewDto>> model) {
        super(id, model);
        initLayout(true);
    }

    public WorkItemsPanel(String id, IModel<List<WorkItemNewDto>> model, boolean showAssigned) {
        super(id, model);
        initLayout(showAssigned);
    }

    // this is called locally in order to take showAssigned into account
    private void initLayout(boolean showAssigned) {
        List<IColumn<WorkItemNewDto, String>> columns = new ArrayList<>();

        // TODO clickable links and info icons
        columns.add(new PropertyColumn(createStringResource("WorkItemsPanel.object"), WorkItemNewDto.F_OBJECT_NAME));
        columns.add(new PropertyColumn(createStringResource("WorkItemsPanel.target"), WorkItemNewDto.F_TARGET_NAME));

        if (WebComponentUtil.isAuthorized(AuthorizationConstants.AUTZ_UI_WORK_ITEMS_ALL_URL,
                AuthorizationConstants.AUTZ_UI_WORK_ITEM_URL)) {
            columns.add(new LinkColumn<WorkItemNewDto>(createStringResource("WorkItemsPanel.name"), WorkItemNewDto.F_NAME, WorkItemNewDto.F_NAME) {
                @Override
                public void onClick(AjaxRequestTarget target, IModel<WorkItemNewDto> rowModel) {
                    PageParameters parameters = new PageParameters();
                    parameters.add(OnePageParameterEncoder.PARAMETER, rowModel.getObject().getWorkItemId());
                    setResponsePage(new PageWorkItem(parameters, (PageBase) WorkItemsPanel.this.getPage()));
                }
            });
        } else {
            columns.add(new AbstractColumn<WorkItemNewDto, String>(createStringResource("WorkItemsPanel.name")) {
                @Override
                public void populateItem(Item<ICellPopulator<WorkItemNewDto>> item, String componentId,
                                         final IModel<WorkItemNewDto> rowModel) {
                    item.add(new Label(componentId, new AbstractReadOnlyModel<Object>() {
                        @Override
                        public Object getObject() {
                            return rowModel.getObject().getName();
                        }
                    }));
                }
            });
        }

        columns.add(new PropertyColumn(createStringResource("WorkItemsPanel.started"), WorkItemNewDto.F_PROCESS_STARTED));
        columns.add(new PropertyColumn(createStringResource("WorkItemsPanel.created"), WorkItemNewDto.F_CREATED));
        if (showAssigned) {
            columns.add(new PropertyColumn(createStringResource("WorkItemsPanel.assigned"), WorkItemNewDto.F_ASSIGNEE_OR_CANDIDATES));
        }

        ISortableDataProvider provider = new ListDataProvider(this, getModel());
        TablePanel accountsTable = new TablePanel<>(ID_WORK_ITEMS_TABLE, provider, columns);
        add(accountsTable);
    }
}

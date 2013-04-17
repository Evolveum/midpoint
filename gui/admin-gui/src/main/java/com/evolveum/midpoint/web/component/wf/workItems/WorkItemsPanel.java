/*
 * Copyright (c) 2012 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2013 [name of copyright owner]
 */

package com.evolveum.midpoint.web.component.wf.workItems;

import com.evolveum.midpoint.web.component.data.TablePanel;
import com.evolveum.midpoint.web.component.util.ListDataProvider;
import com.evolveum.midpoint.web.component.util.SimplePanel;
import com.evolveum.midpoint.web.page.admin.workflow.dto.WorkItemDto;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.model.IModel;

import java.util.ArrayList;
import java.util.List;

/**
 * @author lazyman
 * @author mederly
 */
public class WorkItemsPanel extends SimplePanel<List<WorkItemDto>> {

    private static final String ID_WORK_ITEMS_TABLE = "workItemsTable";

    // todo options to select which columns will be shown
    public WorkItemsPanel(String id, IModel<List<WorkItemDto>> model) {
        super(id, model);
    }

    @Override
    protected void initLayout() {
        List<IColumn<WorkItemDto, String>> columns = new ArrayList<IColumn<WorkItemDto, String>>();
        columns.add(new PropertyColumn(createStringResource("ItemApprovalPanel.name"), WorkItemDto.F_NAME));
        columns.add(new PropertyColumn(createStringResource("ItemApprovalPanel.assigned"), WorkItemDto.F_OWNER_OR_CANDIDATES));
        columns.add(new PropertyColumn(createStringResource("ItemApprovalPanel.created"), WorkItemDto.F_CREATED));

        ISortableDataProvider provider = new ListDataProvider(this, getModel());
        TablePanel accountsTable = new TablePanel<WorkItemDto>(ID_WORK_ITEMS_TABLE, provider, columns);
        add(accountsTable);
    }
}

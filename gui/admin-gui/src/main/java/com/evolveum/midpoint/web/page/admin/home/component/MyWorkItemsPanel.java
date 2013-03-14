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

package com.evolveum.midpoint.web.page.admin.home.component;

import com.evolveum.midpoint.web.component.data.TablePanel;
import com.evolveum.midpoint.web.component.util.ListDataProvider;
import com.evolveum.midpoint.web.component.util.SimplePanel;
import com.evolveum.midpoint.web.page.admin.home.dto.MyWorkItemDto;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.model.IModel;

import java.util.ArrayList;
import java.util.List;

/**
 * @author lazyman
 */
public class MyWorkItemsPanel extends SimplePanel<List<MyWorkItemDto>> {

    private static final String ID_WORK_ITEMS_TABLE = "workItemsTable";

    public MyWorkItemsPanel(String id, IModel<List<MyWorkItemDto>> model) {
        super(id, model);
    }

    @Override
    protected void initLayout() {
        List<IColumn<MyWorkItemDto, String>> columns = new ArrayList<IColumn<MyWorkItemDto, String>>();
        columns.add(new PropertyColumn(createStringResource("MyWorkItemsPanel.name"), MyWorkItemDto.F_NAME));
        columns.add(new PropertyColumn(createStringResource("MyWorkItemsPanel.created"), MyWorkItemDto.F_CREATED));

        ISortableDataProvider provider = new ListDataProvider(this, getModel());
        TablePanel accountsTable = new TablePanel<MyWorkItemDto>(ID_WORK_ITEMS_TABLE, provider, columns);
        add(accountsTable);
    }
}

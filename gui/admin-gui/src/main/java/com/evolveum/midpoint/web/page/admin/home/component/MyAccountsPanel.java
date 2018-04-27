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

package com.evolveum.midpoint.web.page.admin.home.component;

import com.evolveum.midpoint.web.component.data.TablePanel;
import com.evolveum.midpoint.web.component.util.ListDataProvider;
import com.evolveum.midpoint.web.component.util.SimplePanel;
import com.evolveum.midpoint.web.page.admin.home.dto.SimpleAccountDto;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.model.IModel;

import java.util.ArrayList;
import java.util.List;

/**
 * @author lazyman
 */
public class MyAccountsPanel extends SimplePanel<List<SimpleAccountDto>> {

    private static final String ID_ACCOUNTS_TABLE = "accountsTable";

    public MyAccountsPanel(String id, IModel<List<SimpleAccountDto>> model) {
        super(id, model);
    }

    @Override
    protected void initLayout() {
        List<IColumn<SimpleAccountDto, String>> columns = new ArrayList<>();
        columns.add(new PropertyColumn(createStringResource("MyAccountsPanel.account.name"), "accountName"));
        columns.add(new PropertyColumn(createStringResource("MyAccountsPanel.account.resource"), "resourceName"));

        ISortableDataProvider provider = new ListDataProvider(this, getModel());
        TablePanel accountsTable = new TablePanel<>(ID_ACCOUNTS_TABLE, provider, columns);
        accountsTable.setShowPaging(false);

        add(accountsTable);
    }
}

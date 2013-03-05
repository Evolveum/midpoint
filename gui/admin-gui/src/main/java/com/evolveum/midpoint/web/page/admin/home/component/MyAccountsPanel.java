/*
 * Copyright (c) 2013 Evolveum
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
import com.evolveum.midpoint.web.page.admin.home.dto.SimpleAccountDto;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.model.Model;

import java.util.ArrayList;
import java.util.List;

/**
 * @author lazyman
 */
public class MyAccountsPanel extends SimplePanel {

    private static final String ID_ACCOUNTS_TABLE = "accountsTable";

    public MyAccountsPanel(String id) {
        super(id);
    }

    @Override
    protected void initLayout() {
        List<IColumn<SimpleAccountDto, String>> columns = new ArrayList<IColumn<SimpleAccountDto, String>>();
        columns.add(new PropertyColumn(createStringResource("MyAccountsPanel.account.name"), "accountName"));
        columns.add(new PropertyColumn(createStringResource("MyAccountsPanel.account.resource"), "resourceName"));

        ISortableDataProvider provider = new ListDataProvider(this, new Model(new ArrayList()));
        TablePanel accountsTable = new TablePanel<SimpleAccountDto>(ID_ACCOUNTS_TABLE, provider, columns);
        accountsTable.setShowPaging(false);

        add(accountsTable);
    }
}

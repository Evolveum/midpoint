/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.home.component;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.web.component.data.TablePanel;
import com.evolveum.midpoint.gui.impl.component.data.provider.ListDataProvider;
import com.evolveum.midpoint.web.page.admin.home.dto.SimpleAccountDto;

/**
 * @author lazyman
 */
public class MyAccountsPanel extends BasePanel<List<SimpleAccountDto>> {

    private static final String ID_ACCOUNTS_TABLE = "accountsTable";

    public MyAccountsPanel(String id, IModel<List<SimpleAccountDto>> model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

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

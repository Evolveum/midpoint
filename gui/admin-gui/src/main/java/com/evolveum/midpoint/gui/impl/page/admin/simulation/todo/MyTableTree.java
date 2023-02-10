/*
 * Copyright (c) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.simulation.todo;

import java.util.List;
import java.util.Set;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.tree.ITreeProvider;
import org.apache.wicket.extensions.markup.html.repeater.tree.TableTree;
import org.apache.wicket.markup.repeater.data.IDataProvider;
import org.apache.wicket.model.IModel;

/**
 * Created by Viliam Repan (lazyman).
 */
public class MyTableTree<T, S> extends TableTree<T, S> {

    public MyTableTree(String id, List<? extends IColumn<T, S>> columns, ITreeProvider<T> provider, long rowsPerPage) {
        this(id, columns, provider, rowsPerPage, null);
    }

    public MyTableTree(String id, List<? extends IColumn<T, S>> columns, ITreeProvider<T> provider, long rowsPerPage, IModel<? extends Set<T>> state) {
        super(id, columns, provider, rowsPerPage, state);
    }

    @Override
    public Component newContentComponent(String id, IModel<T> model) {
        return new MyTreeColumnPanel<>(id, this, model);
    }

    @Override
    protected DataTable<T, S> newDataTable(String id, List<? extends IColumn<T, S>> iColumns, IDataProvider<T> dataProvider, long rowsPerPage) {
        DataTable<T, S> table = super.newDataTable(id, iColumns, dataProvider, rowsPerPage);
        table.add(AttributeModifier.replace("class", "table"));
        return table;
    }
}

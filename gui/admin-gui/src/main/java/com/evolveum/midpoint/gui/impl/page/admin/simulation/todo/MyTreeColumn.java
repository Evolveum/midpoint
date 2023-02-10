/*
 * Copyright (c) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.simulation.todo;

import org.apache.wicket.Component;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.tree.table.NodeModel;
import org.apache.wicket.extensions.markup.html.repeater.tree.table.TreeColumn;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;

/**
 * Created by Viliam Repan (lazyman).
 */
public class MyTreeColumn<T, S> extends TreeColumn<T, S> {

    public MyTreeColumn(IModel<String> displayModel) {
        super(displayModel);
    }

    public MyTreeColumn(IModel<String> displayModel, S sortProperty) {
        super(displayModel, sortProperty);
    }

    @Override
    public void populateItem(Item<ICellPopulator<T>> item, String id, IModel<T> rowModel) {
        NodeModel<T> model = (NodeModel<T>) rowModel;

        Component component = getTree().newNodeComponent(id, model.getWrappedModel());
        component.add(new MyNodeBorder(model.getBranches()));

        item.add(component);
    }
}

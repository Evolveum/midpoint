/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.component.data.column.icon;

import com.evolveum.midpoint.xml.ns._public.common.common_3.DisplayType;

import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.export.IExportableColumn;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

public abstract class CompositedIconWithLabelColumn<T> extends CompositedIconColumn<T> implements IExportableColumn<T, String> {

    public CompositedIconWithLabelColumn(IModel<String> displayModel) {
        super(displayModel);
    }

    @Override
    public void populateItem(Item<ICellPopulator<T>> cellItem, String componentId, IModel<T> rowModel) {
        cellItem.add(new CompositedIconWithLabelPanel(componentId, Model.of(getCompositedIcon(rowModel)), getLabelDisplayModel(rowModel)));
    }

    public abstract IModel<DisplayType> getLabelDisplayModel(IModel<T> rowModel);

}

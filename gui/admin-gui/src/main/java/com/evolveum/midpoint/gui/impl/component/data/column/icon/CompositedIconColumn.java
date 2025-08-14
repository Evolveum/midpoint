/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.component.data.column.icon;

import com.evolveum.midpoint.gui.impl.component.icon.CompositedIcon;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

/**
 * @author skublik
 */
public abstract class CompositedIconColumn<T> extends AbstractIconColumn<T, String> implements IColumn<T, String> {
    private static final long serialVersionUID = 1L;

    public CompositedIconColumn(IModel<String> displayModel) {
        super(displayModel);
    }

    @Override
    public String getCssClass() {
        IModel<String> display = getDisplayModel();
        if (display != null && StringUtils.isNotEmpty(display.getObject())) {
            return "align-middle";
        }

        return "composited-icon align-middle";
    }

    @Override
    public void populateItem(Item<ICellPopulator<T>> cellItem, String componentId, IModel<T> rowModel) {
        cellItem.add(new CompositedIconPanel(componentId, Model.of(getCompositedIcon(rowModel))));
    }

    protected abstract CompositedIcon getCompositedIcon(final IModel<T> rowModel);

    public IModel<String> getDataModel(IModel<T> rowModel) {
        return Model.of("");
    }
}

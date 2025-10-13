/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web.component.data.column;

import com.evolveum.midpoint.gui.api.model.ReadOnlyModel;
import com.evolveum.midpoint.gui.impl.component.data.column.icon.AbstractIconColumn;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DisplayType;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;

import java.util.Map;

/**
 * @author skublik
 */
public abstract class CountIconColumn<T> extends AbstractIconColumn<T, String> {//implements IExportableColumn<T, String> {
    private static final long serialVersionUID = 1L;

    public CountIconColumn(IModel<String> displayModel) {
        super(displayModel);
    }

    public CountIconColumn(IModel<String> displayModel, String sortProperty) {
        super(displayModel, sortProperty);
    }

    @Override
    public void populateItem(Item<ICellPopulator<T>> cellItem, String componentId, IModel<T> rowModel) {
        Map<DisplayType, Integer> map = getIconDisplayType(rowModel);
        if (map != null) {
            cellItem.add(new CountIconPanel(componentId, getIconDisplayType(rowModel)));
        } else {
            cellItem.add(new WebMarkupContainer(componentId));
        }
    }

    protected abstract Map<DisplayType, Integer> getIconDisplayType(final IModel<T> rowModel);

}

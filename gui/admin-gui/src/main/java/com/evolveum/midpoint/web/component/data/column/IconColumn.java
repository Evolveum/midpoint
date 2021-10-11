/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.data.column;

import com.evolveum.midpoint.xml.ns._public.common.common_3.DisplayType;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.export.IExportableColumn;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

/**
 * @author lazyman
 */
public abstract class IconColumn<T> extends AbstractColumn<T, String> {//implements IExportableColumn<T, String> {
    private static final long serialVersionUID = 1L;

    public IconColumn(IModel<String> displayModel) {
        super(displayModel);
    }

    public IconColumn(IModel<String> displayModel, String sortProperty) {
        super(displayModel, sortProperty);
    }

    @Override
    public String getCssClass() {
        IModel<String> display = getDisplayModel();
        if (display != null && StringUtils.isNotEmpty(display.getObject())) {
            return null;
        }

        return "icon";
    }


    @Override
    public void populateItem(Item<ICellPopulator<T>> cellItem, String componentId, IModel<T> rowModel) {
        cellItem.add(new ImagePanel(componentId, getIconDisplayType(rowModel)));
    }

    protected abstract DisplayType getIconDisplayType(final IModel<T> rowModel);

//    @Override
//    public IModel<String> getDataModel(IModel<T> rowModel) {
//        return Model.of("");
//    }
}

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
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;

import java.io.Serial;

/**
 * @author lazyman
 */
public abstract class IconColumn<T> extends AbstractIconColumn<T, String> {
    @Serial private static final long serialVersionUID = 1L;

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
            return "align-middle";
        }

        return "icon align-middle";
    }

    @Override
    public void populateItem(Item<ICellPopulator<T>> cellItem, String componentId, IModel<T> rowModel) {
        ImagePanel panel = new ImagePanel(componentId, new ReadOnlyModel<>(() -> getIconDisplayType(rowModel)));
        panel.setIconRole(ImagePanel.IconRole.IMAGE);
        cellItem.add(panel);
    }

    protected abstract DisplayType getIconDisplayType(final IModel<T> rowModel);
}

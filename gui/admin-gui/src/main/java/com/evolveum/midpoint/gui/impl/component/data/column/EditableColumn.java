/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.component.data.column;

import org.apache.wicket.Component;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.web.component.input.TextPanel;
import com.evolveum.midpoint.web.component.util.Editable;

/**
 * @author lazyman
 * @author skublik
 */
public abstract class EditableColumn<T  extends Editable, S> extends AbstractColumn<T, S> {

    private static final long serialVersionUID = 1L;

    public EditableColumn(IModel<String> displayModel) {
        super(displayModel);
    }

    public EditableColumn(IModel<String> displayModel, S sortProperty) {
        super(displayModel, sortProperty);
    }

    @Override
    public void populateItem(Item<ICellPopulator<T>> cellItem, String componentId,
            final IModel<T> rowModel) {
        if (!rowModel.getObject().isEditing()) {
            cellItem.add(createStaticPanel(componentId, rowModel));
        } else {
            cellItem.add(createInputPanel(componentId, rowModel));
        }
    }

    protected Component createInputPanel(String componentId, IModel<T> rowModel) {
        return new TextPanel(componentId, rowModel);
    }

    protected Component createStaticPanel(String componentId, IModel<T> rowModel) {
        return new Label(componentId, rowModel.getObject().toString());
    }
}

/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.data.column;

import com.evolveum.midpoint.web.component.input.TextPanel;
import com.evolveum.midpoint.web.component.util.Editable;
import org.apache.wicket.Component;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;

/**
 * @author lazyman
 */
public class EditableAjaxLinkColumn<T extends Editable> extends AjaxLinkColumn<T> {

    public EditableAjaxLinkColumn(IModel<String> displayModel) {
        super(displayModel);
    }

    public EditableAjaxLinkColumn(IModel<String> displayModel, String propertyExpression) {
        super(displayModel, propertyExpression);
    }

    public EditableAjaxLinkColumn(IModel<String> displayModel, String sortProperty, String propertyExpression) {
        super(displayModel, sortProperty, propertyExpression);
    }

    @Override
    public void populateItem(Item<ICellPopulator<T>> cellItem, String componentId,
            final IModel<T> rowModel) {
        Editable editable = rowModel.getObject();
        if (!editable.isEditing()) {
            super.populateItem(cellItem, componentId, rowModel);
        } else {
            cellItem.add(createInputPanel(componentId, rowModel));
        }
    }

    protected Component createInputPanel(String componentId, IModel<T> model) {
        return new TextPanel(componentId, model);
    }
}

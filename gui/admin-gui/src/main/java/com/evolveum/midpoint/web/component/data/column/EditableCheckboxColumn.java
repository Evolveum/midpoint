/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web.component.data.column;

import com.evolveum.midpoint.web.component.input.CheckPanel;
import com.evolveum.midpoint.web.component.prism.InputPanel;
import com.evolveum.midpoint.web.component.util.Editable;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

/**
 * @author lazyman
 */
public class EditableCheckboxColumn<T extends Editable> extends CheckBoxColumn<T> {

    public EditableCheckboxColumn(IModel<String> displayModel) {
        super(displayModel);
    }

    public EditableCheckboxColumn(IModel<String> displayModel, String propertyExpression) {
        super(displayModel, propertyExpression);
    }

    @Override
    public void populateItem(Item<ICellPopulator<T>> cellItem, String componentId,
                             final IModel<T> rowModel) {

        if (!isEditing(rowModel)) {
            super.populateItem(cellItem, componentId, rowModel);
        } else {
            cellItem.add(createInputPanel(componentId, rowModel));
        }
    }

    protected boolean isEditing(IModel<T> rowModel) {
        Editable editable = rowModel.getObject();
        return editable.isEditing();
    }

    protected InputPanel createInputPanel(String componentId, IModel<T> model) {
        return new CheckPanel(componentId, new PropertyModel(model, getPropertyExpression()));
    }
}

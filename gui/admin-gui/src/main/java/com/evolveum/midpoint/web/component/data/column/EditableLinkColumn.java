/*
 * Copyright (c) 2012 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2012 [name of copyright owner]
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
public class EditableLinkColumn<T extends Editable> extends LinkColumn<T> {

    public EditableLinkColumn(IModel<String> displayModel) {
        super(displayModel);
    }

    public EditableLinkColumn(IModel<String> displayModel, String propertyExpression) {
        super(displayModel, propertyExpression);
    }

    public EditableLinkColumn(IModel<String> displayModel, String sortProperty, String propertyExpression) {
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

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

import com.evolveum.midpoint.web.component.prism.input.TextPanel;
import com.evolveum.midpoint.web.component.util.Editable;
import com.evolveum.midpoint.web.component.util.Selectable;
import org.apache.wicket.Component;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

/**
 * @author lazyman
 */
public class EditableCheckboxColumn<T extends Editable> extends CheckBoxColumn<Selectable<T>> {

    public EditableCheckboxColumn(IModel<String> displayModel) {
        super(displayModel);
    }

    public EditableCheckboxColumn(IModel<String> displayModel, String propertyExpression) {
        super(displayModel, propertyExpression);
    }

//    @Override
//    public void populateItem(Item<ICellPopulator<Selectable<T>>> cellItem, String componentId,
//            final IModel<Selectable<T>> rowModel) {
//
//        if (!isEditing(rowModel)) {
//            super.populateItem(cellItem, componentId, rowModel);
//        } else {
//            cellItem.add(createInputPanel(componentId, rowModel));
//        }
//    }

    protected boolean isEditing(IModel<T> rowModel) {
        Editable editable = rowModel.getObject();
        return editable.isEditing();
    }

    protected Component createInputPanel(String componentId, IModel<T> model) {
        return new TextPanel(componentId, new PropertyModel(model, getPropertyExpression()));
    }
}

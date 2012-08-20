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

import com.evolveum.midpoint.web.component.button.ButtonType;
import org.apache.commons.lang.Validate;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

/**
 * @author lazyman
 */
public class ButtonColumn<T> extends AbstractColumn<T> {

    private IModel<String> buttonLabel;
    private String propertyExpression;
    private ButtonType buttonType = ButtonType.SIMPLE;

    public ButtonColumn(IModel<String> displayModel, IModel<String> buttonLabel) {
        super(displayModel);
        this.buttonLabel = buttonLabel;
    }

    public ButtonColumn(IModel<String> displayModel, String propertyExpression) {
        super(displayModel);
        this.propertyExpression = propertyExpression;
    }

    @Override
    public void populateItem(Item<ICellPopulator<T>> cellItem, String componentId,
                             final IModel<T> rowModel) {
        IModel<String> label = buttonLabel;
        if (label == null) {
            label = new PropertyModel<String>(rowModel, propertyExpression);
        }

        cellItem.add(new ButtonPanel(componentId, label, buttonType) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                ButtonColumn.this.onClick(target, rowModel);
            }
        });
    }

    public void onClick(AjaxRequestTarget target, IModel<T> rowModel) {

    }

    public void setButtonType(ButtonType buttonType) {
        Validate.notNull(buttonType, "Button type must not be null.");
        this.buttonType = buttonType;
    }
}

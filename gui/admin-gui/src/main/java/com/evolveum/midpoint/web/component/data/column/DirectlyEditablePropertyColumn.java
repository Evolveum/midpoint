/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web.component.data.column;

import com.evolveum.midpoint.web.component.input.TextPanel;
import com.evolveum.midpoint.web.component.prism.InputPanel;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;

import java.io.Serial;

/**
 * @author lazyman
 *
 * EXPERIMENTAL - to be used with PageCertDecisions until sufficiently stable
 */
public class DirectlyEditablePropertyColumn<T> extends PropertyColumn<T, String> {

    @Serial private static final long serialVersionUID = 1L;

    public DirectlyEditablePropertyColumn(IModel<String> displayModel, String propertyExpression) {
        super(displayModel, propertyExpression);
    }

    @Override
    public void populateItem(Item<ICellPopulator<T>> cellItem, String componentId,
            final IModel<T> rowModel) {
        cellItem.add(createInputPanel(componentId, rowModel));
    }

    protected InputPanel createInputPanel(String componentId, final IModel<T> model) {
        // due to #10912 the idea to refresh the whole cert. items table after each comment update was refused.
        // instead, we remember the value of the previous comment value to be able to perform comment update
        // only if the comment was changed
        IModel<String> previousTextInputValue = Model.of();

        TextPanel<?> textPanel = new TextPanel<String>(componentId, new PropertyModel<>(model, getPropertyExpression()));
        TextField<?> textField = (TextField<?>) textPanel.getBaseFormComponent();     // UGLY HACK
        textField.add(new AjaxFormComponentUpdatingBehavior("blur") {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                onBlur(target, model, previousTextInputValue);
            }
        });
        return textPanel;
    }

    public void onBlur(AjaxRequestTarget target, IModel<T> model, IModel<String> previousTextInputValue) {
        // doing nothing; may be overridden in subclasses
    }
}

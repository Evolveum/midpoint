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
        TextPanel<?> textPanel = new TextPanel<String>(componentId, new PropertyModel<>(model, getPropertyExpression()));
        TextField<?> textField = (TextField<?>) textPanel.getBaseFormComponent();     // UGLY HACK
        textField.add(new AjaxFormComponentUpdatingBehavior("change") {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                onUpdateValue(target, model);
            }
        });
        return textPanel;
    }

    public void onUpdateValue(AjaxRequestTarget target, IModel<T> model) {
        // doing nothing; may be overridden in subclasses
    }
}

/*
 * Copyright (c) 2010-2015 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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

/**
 * @author lazyman
 * @author mederly
 *
 * EXPERIMENTAL - to be used with PageCertDecisions until sufficiently stable
 */
public class DirectlyEditablePropertyColumn<T> extends PropertyColumn<T, String> {

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
        textField.add(new AjaxFormComponentUpdatingBehavior("blur") {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                onBlur(target, model);
            }
        });
        return textPanel;
    }

    public void onBlur(AjaxRequestTarget target, IModel<T> model) {
        // doing nothing; may be overridden in subclasses
    }
}

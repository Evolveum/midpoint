/*
 * Copyright (c) 2010-2013 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.gui.impl.component.data.column;

import com.evolveum.midpoint.web.component.data.column.LinkColumn;
import com.evolveum.midpoint.web.component.input.TextPanel;
import org.apache.wicket.Component;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import com.evolveum.midpoint.web.component.prism.ContainerValueWrapper;

/**
 * @author lazyman
 */
public class EditableLinkColumnForContainerWrapper<ConValWrapp extends ContainerValueWrapper> extends LinkColumn<ConValWrapp> {

	private static final long serialVersionUID = 1L;

	public EditableLinkColumnForContainerWrapper(IModel<String> displayModel) {
        super(displayModel);
    }

    public EditableLinkColumnForContainerWrapper(IModel<String> displayModel, String propertyExpression) {
        super(displayModel, propertyExpression);
    }

    public EditableLinkColumnForContainerWrapper(IModel<String> displayModel, String sortProperty, String propertyExpression) {
        super(displayModel, sortProperty, propertyExpression);
    }

    @Override
    public void populateItem(Item<ICellPopulator<ConValWrapp>> cellItem, String componentId,
            final IModel<ConValWrapp> rowModel) {
        if (!rowModel.getObject().isSelected()) {
            super.populateItem(cellItem, componentId, rowModel);
        } else {
            cellItem.add(createInputPanel(componentId, rowModel));
            rowModel.getObject().setSelected(true);
        }
    }

    protected Component createInputPanel(String componentId, IModel<ConValWrapp> rowModel) {
        return new TextPanel<ConValWrapp>(componentId, rowModel);
    }
}

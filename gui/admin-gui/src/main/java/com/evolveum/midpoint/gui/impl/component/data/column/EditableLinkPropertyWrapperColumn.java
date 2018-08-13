/*
 * Copyright (c) 2018 Evolveum
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

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.model.PropertyWrapperFromContainerValueWrapperModel;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.data.column.LinkColumn;
import com.evolveum.midpoint.web.component.form.Form;

import javax.xml.namespace.QName;

import org.apache.wicket.Component;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.web.component.prism.ContainerValueWrapper;
import com.evolveum.midpoint.web.component.prism.PrismPropertyColumn;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ClassLoggerConfigurationType;

/**
 * @author skublik
 */
public class EditableLinkPropertyWrapperColumn<C extends Containerable> extends LinkColumn<ContainerValueWrapper<C>> {

	private static final long serialVersionUID = 1L;
	
	private static final Trace LOGGER = TraceManager.getTrace(EditableLinkPropertyWrapperColumn.class);
	
	private QName qNameOfProperty;
	private PageBase page;

	public EditableLinkPropertyWrapperColumn(IModel<String> displayModel, QName item, PageBase page) {
        super(displayModel);
        this.qNameOfProperty = item;
        this.page = page;
    }

    public EditableLinkPropertyWrapperColumn(IModel<String> displayModel, String propertyExpression, QName item, PageBase page) {
        super(displayModel, propertyExpression);
        this.qNameOfProperty = item;
        this.page = page;
    }

    public EditableLinkPropertyWrapperColumn(IModel<String> displayModel, String sortProperty, String propertyExpression, QName item, PageBase page) {
        super(displayModel, sortProperty, propertyExpression);
        this.qNameOfProperty = item;
        this.page = page;
    }

    @Override
    public void populateItem(Item<ICellPopulator<ContainerValueWrapper<C>>> cellItem, String componentId,
            final IModel<ContainerValueWrapper<C>> rowModel) {
        if (!rowModel.getObject().isSelected()) {
            super.populateItem(cellItem, componentId, rowModel);
        } else {
            cellItem.add(createInputPanel(componentId, rowModel));
            rowModel.getObject().setSelected(true);
        }
    }

    protected Component createInputPanel(String componentId, IModel<ContainerValueWrapper<C>> rowModel) {
    	Form form= new Form("form");
    	PropertyWrapperFromContainerValueWrapperModel model = new PropertyWrapperFromContainerValueWrapperModel<>(rowModel, qNameOfProperty);
    	PrismPropertyColumn panel = new PrismPropertyColumn<>(componentId, model, form, getPageBase());
    	return panel;
    }
    
    @Override
    protected IModel createLinkModel(IModel<ContainerValueWrapper<C>> rowModel) {
    	Form form= new Form("form");
    	PropertyWrapperFromContainerValueWrapperModel model = new PropertyWrapperFromContainerValueWrapperModel<>(rowModel, qNameOfProperty);
    	return Model.of(String.valueOf(model.getObject().getItem().getRealValue()));
    }
    
    private PageBase getPageBase() {
		return page;
	}
}

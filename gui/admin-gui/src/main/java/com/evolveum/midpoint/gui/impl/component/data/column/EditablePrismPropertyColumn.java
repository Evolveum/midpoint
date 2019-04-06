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

import org.apache.wicket.Component;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.PrismContainerWrapper;
import com.evolveum.midpoint.gui.impl.component.prism.PrismPropertyColumnPanel;
import com.evolveum.midpoint.gui.impl.model.PropertyOrReferenceWrapperFromContainerModel;
import com.evolveum.midpoint.gui.impl.prism.ContainerWrapperImpl;
import com.evolveum.midpoint.gui.impl.prism.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.prism.PrismPropertyValueWrapper;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.web.component.form.Form;
import com.evolveum.midpoint.web.component.prism.ContainerValueWrapper;
import com.evolveum.midpoint.web.component.prism.PropertyOrReferenceWrapper;

/**
 * @author skublik
 */
public class EditablePrismPropertyColumn<C extends Containerable, T> extends AbstractItemWrapperColumn<C, PrismPropertyValueWrapper<T>> {

	/**
	 * @param mainModel
	 * @param itemName
	 * @param pageBase
	 * @param readonly
	 */
	EditablePrismPropertyColumn(IModel<PrismContainerWrapper<C>> mainModel, ItemName itemName, PageBase pageBase,
			boolean readonly) {
		super(mainModel, itemName, pageBase, readonly);
		// TODO Auto-generated constructor stub
	}

	/* (non-Javadoc)
	 * @see org.apache.wicket.extensions.markup.html.repeater.data.table.export.IExportableColumn#getDataModel(org.apache.wicket.model.IModel)
	 */
	@Override
	public IModel<?> getDataModel(IModel<PrismContainerValueWrapper<C>> rowModel) {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.gui.impl.component.data.column.AbstractItemWrapperColumn#createLabel(com.evolveum.midpoint.gui.impl.prism.PrismValueWrapper)
	 */
	@Override
	protected String createLabel(PrismPropertyValueWrapper<T> object) {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.gui.impl.component.data.column.AbstractItemWrapperColumn#createValuePanel(org.apache.wicket.model.IModel, com.evolveum.midpoint.gui.impl.prism.PrismValueWrapper)
	 */
	@Override
	protected Panel createValuePanel(IModel<?> headerModel, PrismPropertyValueWrapper<T> object) {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.gui.impl.component.data.column.AbstractItemWrapperColumn#createHeader(org.apache.wicket.model.IModel)
	 */
	@Override
	protected Component createHeader(String componentId, IModel<PrismContainerWrapper<C>> mainModel) {
		// TODO Auto-generated method stub
		return null;
	}

	
//	@Override
//	public void populateItem(Item<ICellPopulator<ContainerValueWrapper<C>>> cellItem, String componentId,
//			IModel<ContainerValueWrapper<C>> rowModel) {
//		PropertyOrReferenceWrapperFromContainerModel property = new PropertyOrReferenceWrapperFromContainerModel<>(rowModel.getObject(), getqNameOfItem());
//		cellItem.add(new PrismPropertyColumnPanel(componentId, property, new Form("form"), null));
//	}
	
}

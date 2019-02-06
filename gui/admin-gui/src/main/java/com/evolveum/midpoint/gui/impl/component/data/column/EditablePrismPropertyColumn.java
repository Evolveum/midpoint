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

import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.component.prism.PrismPropertyColumnPanel;
import com.evolveum.midpoint.gui.impl.model.PropertyOrReferenceWrapperFromContainerModel;
import com.evolveum.midpoint.gui.impl.prism.ContainerWrapperImpl;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.web.component.form.Form;
import com.evolveum.midpoint.web.component.prism.ContainerValueWrapper;
import com.evolveum.midpoint.web.component.prism.PropertyOrReferenceWrapper;

/**
 * @author skublik
 */
public class EditablePrismPropertyColumn<C extends Containerable> extends AbstractItemWrapperColumn<C> {

	public EditablePrismPropertyColumn(final IModel<ContainerWrapperImpl<Containerable>> headerModel, ItemName name, PageBase pageBase) {
		super(headerModel == null ? null : getPropertyOrReferenceForHeaderWrapper(headerModel, name, pageBase),
				pageBase);
	}
	
	public EditablePrismPropertyColumn(IModel<PropertyOrReferenceWrapper> headerModel, PageBase pageBase) {
		super(headerModel, pageBase);
	}
	
	@Override
	public void populateItem(Item<ICellPopulator<ContainerValueWrapper<C>>> cellItem, String componentId,
			IModel<ContainerValueWrapper<C>> rowModel) {
		PropertyOrReferenceWrapperFromContainerModel property = new PropertyOrReferenceWrapperFromContainerModel<>(rowModel.getObject(), getqNameOfItem());
		cellItem.add(new PrismPropertyColumnPanel(componentId, property, new Form("form"), null));
	}
	
}

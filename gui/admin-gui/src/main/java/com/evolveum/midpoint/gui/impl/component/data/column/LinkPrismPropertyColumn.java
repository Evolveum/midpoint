/*
 * Copyright (c) 2010-2018 Evolveum
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
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.export.IExportableColumn;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.prism.StaticItemWrapperColumnPanel;
import com.evolveum.midpoint.gui.impl.factory.ItemRealValueModel;
import com.evolveum.midpoint.gui.impl.model.PropertyOrReferenceWrapperFromContainerModel;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.Referencable;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.web.component.data.column.LinkPanel;
import com.evolveum.midpoint.web.component.form.Form;
import com.evolveum.midpoint.web.component.prism.ContainerValueWrapper;
import com.evolveum.midpoint.web.component.prism.ContainerWrapper;
import com.evolveum.midpoint.web.component.prism.PropertyOrReferenceWrapper;
import com.evolveum.midpoint.web.component.prism.ValueWrapper;

/**
 * @author skublik
 */
public class LinkPrismPropertyColumn<T,C extends Containerable> extends AbstractItemWrapperColumn<C> implements IExportableColumn<ContainerValueWrapper<C>, String>{

	public LinkPrismPropertyColumn(final IModel<ContainerWrapper<Containerable>> headerModel, ItemName name, PageBase pageBase) {
		super(headerModel == null ? null : getPropertyOrReferenceForHeaderWrapper(headerModel, name, pageBase),
				pageBase);
	}
	
	public LinkPrismPropertyColumn(IModel<PropertyOrReferenceWrapper> headerModel, PageBase pageBase) {
		super(headerModel, pageBase);
	}
	
	@Override
	public void populateItem(Item<ICellPopulator<ContainerValueWrapper<C>>> cellItem, String componentId,
			IModel<ContainerValueWrapper<C>> rowModel) {
		PropertyOrReferenceWrapperFromContainerModel property = new PropertyOrReferenceWrapperFromContainerModel<>(rowModel.getObject(), getqNameOfItem());
		cellItem.add(new StaticItemWrapperColumnPanel(componentId, property, new Form("form"), null, getPageBase()) {
			@Override
			protected Component getDisplayComponent(String componentId, IModel model) {
				return new LinkPanel(componentId, model) {
		        	private static final long serialVersionUID = 1L;

		            @Override
		            public void onClick(AjaxRequestTarget target) {
		            	LinkPrismPropertyColumn.this.onClick(target, rowModel);
		            }

		            @Override
		            public boolean isEnabled() {
		                return LinkPrismPropertyColumn.this.isEnabled(rowModel);
		            }
		        };
			}
			
			@Override
			protected IModel populatePropertyItem(ValueWrapper object) {
				return LinkPrismPropertyColumn.this.populatePropertyItem(object);
			}
			
			@Override
			protected IModel populateReferenceItem(ValueWrapper object) {
				return LinkPrismPropertyColumn.this.populateReferenceItem(object);
			}
		});
	}
	
	protected IModel<String> populatePropertyItem(ValueWrapper object) {
    	return new ItemRealValueModel<String>(object);
	}
	
	protected IModel<String> populateReferenceItem(ValueWrapper object) {
    	return new IModel<String>() {

			@Override
			public String getObject() {
				return WebComponentUtil.getReferencedObjectDisplayNamesAndNames(
						new ItemRealValueModel<Referencable>(object).getObject(), false);
			}
			
		};
	}

	protected boolean isEnabled(IModel<ContainerValueWrapper<C>> rowModel) {
		return true;
	}

	protected void onClick(AjaxRequestTarget target, IModel<ContainerValueWrapper<C>> rowModel) {
	}

	@Override
	public IModel<?> getDataModel(IModel<ContainerValueWrapper<C>> rowModel) {
		return new PropertyOrReferenceWrapperFromContainerModel<>(rowModel.getObject(), getqNameOfItem());
	}

}



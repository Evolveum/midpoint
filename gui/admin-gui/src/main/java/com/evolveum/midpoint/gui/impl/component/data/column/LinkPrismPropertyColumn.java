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

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.export.IExportableColumn;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.PrismContainerWrapper;
import com.evolveum.midpoint.gui.impl.factory.ItemRealValueModel;
import com.evolveum.midpoint.gui.impl.prism.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.prism.PrismPropertyValueWrapper;
import com.evolveum.midpoint.gui.impl.prism.PrismValueWrapper;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.web.component.data.column.LinkPanel;

/**
 * @author skublik
 */
public class LinkPrismPropertyColumn<C extends Containerable, T> extends PrismPropertyColumn<C, T> implements IExportableColumn<PrismContainerValueWrapper<C>, String>{


	private static final long serialVersionUID = 1L;
	private static final String ID_LABEL = "label";
	
	public LinkPrismPropertyColumn(IModel<PrismContainerWrapper<C>> mainModel, ItemPath itemName, PageBase pageBase) {
		super(mainModel, itemName, pageBase, false);
	}
	

	protected void onClick(AjaxRequestTarget target, IModel<PrismContainerValueWrapper<C>> rowModel) {
	}
	
	
	@Override
	protected void populate(ListItem<PrismPropertyValueWrapper<T>> item, IModel<?> rowModel) {
		LinkPanel linkPanel = new LinkPanel(ID_LABEL,
				new ItemRealValueModel(item.getModel())) {
			private static final long serialVersionUID = 1L;

			@Override
			public void onClick(AjaxRequestTarget target) {
				LinkPrismPropertyColumn.this.onClick(target, (IModel<PrismContainerValueWrapper<C>>) rowModel);
			}

			@Override
			public boolean isEnabled() {
				return !isReadOnly();
			}
		};
		item.add(linkPanel);
	}
	

//		PropertyOrReferenceWrapperFromContainerModel property = new PropertyOrReferenceWrapperFromContainerModel<>(rowModel.getObject(), getqNameOfItem());
//		cellItem.add(new StaticItemWrapperColumnPanel(componentId, property, new Form("form"), null) {
//			@Override
//			protected Component getDisplayComponent(String componentId, IModel model) {
//				return new LinkPanel(componentId, model) {
//		        	private static final long serialVersionUID = 1L;
//
//		            @Override
//		            public void onClick(AjaxRequestTarget target) {
//		            	LinkPrismPropertyColumn.this.onClick(target, rowModel);
//		            }
//
//		            @Override
//		            public boolean isEnabled() {
//		                return LinkPrismPropertyColumn.this.isEnabled(rowModel);
//		            }
//		        };
//			}
//			
//			@Override
//			protected IModel populatePropertyItem(ValueWrapperOld object) {
//				return LinkPrismPropertyColumn.this.populatePropertyItem(object);
//			}
//			
//			@Override
//			protected IModel populateReferenceItem(ValueWrapperOld object) {
//				return LinkPrismPropertyColumn.this.populateReferenceItem(object);
//			}
//		});Pri

	

}



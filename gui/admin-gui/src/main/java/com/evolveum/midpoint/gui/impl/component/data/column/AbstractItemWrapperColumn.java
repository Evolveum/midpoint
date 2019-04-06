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

import javax.xml.namespace.QName;

import org.apache.commons.lang.Validate;
import org.apache.wicket.Component;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.export.IExportableColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

import com.evolveum.midpoint.gui.api.factory.GuiComponentFactory;
import com.evolveum.midpoint.gui.api.factory.RealValuable;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.ItemWrapperOld;
import com.evolveum.midpoint.gui.api.prism.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.component.prism.PrismPropertyHeaderPanel;
import com.evolveum.midpoint.gui.impl.prism.ContainerWrapperImpl;
import com.evolveum.midpoint.gui.impl.prism.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.prism.PrismValueWrapper;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.web.component.prism.ContainerValueWrapper;
import com.evolveum.midpoint.web.component.prism.PropertyOrReferenceWrapper;

/**
 * @author skublik
 */
public abstract class AbstractItemWrapperColumn<C extends Containerable, VW extends PrismValueWrapper> extends AbstractColumn<PrismContainerValueWrapper<C>, String> implements IExportableColumn<PrismContainerValueWrapper<C>, String>{

	protected PageBase pageBase;
	protected ItemPath itemName;
	
	private boolean readOnly;
	
	private static final String ID_VALUE = "value";
	
	
	private IModel<PrismContainerWrapper<C>> mainModel = null;
	
	AbstractItemWrapperColumn(IModel<PrismContainerWrapper<C>> mainModel, ItemPath itemName, PageBase pageBase, boolean readonly) {
		super(null);
		Validate.notNull(mainModel, "no model");
		Validate.notNull(mainModel.getObject(), "no ContainerWrappe from model");
		Validate.notNull(itemName, "no qName");
		this.pageBase = pageBase;
		this.mainModel = mainModel;
		this.itemName = itemName;
		this.readOnly = readonly;
//		this.headerModel = headerModel;
//		this.headerModel = new IModel<ItemWrapperOld>() {
//			private static final long serialVersionUID = 1L;
//
//			@Override
//			public ItemWrapperOld getObject() {
//				if(headerModel.getObject().getValues().size() < 1) {
//		    		ContainerValueWrapper<C> value = WebModelServiceUtils.createNewItemContainerValueWrapper(pageBase, headerModel);
//		    		return value.findItemWrapper(name);
//		    	} else {
//		    		return headerModel.getObject().getValues().get(0).findItemWrapper(name);
//		    	}
//			}
//			
//		};
//		qNameOfItem = name;
	}
	
//	public AbstractItemWrapperColumn(final IModel<PrismContainerWrapper<C>> headerModel, PageBase pageBase) {
//		super(null);
//		this.pageBase = pageBase;
//		Validate.notNull(headerModel, "no model");
//		Validate.notNull(headerModel.getObject(), "no ContainerWrappe from model");
//		this.headerModel = headerModel;
//		qNameOfItem = headerModel.getObject().getName();
//	}
	
	@Override
	public Component getHeader(String componentId) {
		return createHeader(componentId, mainModel);
//		PrismPropertyHeaderPanel<ItemWrapperOld> header = new PrismPropertyHeaderPanel<ItemWrapperOld>(componentId, headerModel, getPageBase()) {
//			@Override
//			public String getContainerLabelCssClass() {
//				return " col-xs-12 ";
//			}
//		};
//		return header;
	}
	
	/* (non-Javadoc)
	 * @see org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator#populateItem(org.apache.wicket.markup.repeater.Item, java.lang.String, org.apache.wicket.model.IModel)
	 */
	@Override
	public void populateItem(Item<ICellPopulator<PrismContainerValueWrapper<C>>> cellItem, String componentId,
			IModel<PrismContainerValueWrapper<C>> rowModel) {
		
		ListView<VW> listView = new ListView<VW>(componentId, new PropertyModel<>(getDataModel(rowModel), "values")) {

			@Override
			protected void populateItem(ListItem<VW> item) {
				populate(item, getDataModel(rowModel));
				
			}
		};
		listView.setReuseItems(true);
		listView.setOutputMarkupId(true);
		
		cellItem.add(listView);
		
	}
	
	protected void populate(ListItem<VW> item, IModel<?> rowModel) {
		if (readOnly) {
			Label label = new Label(ID_VALUE, createLabel(item.getModelObject()));
			item.add(label);
			return;
		}
		
		item.add(createValuePanel(rowModel, item.getModelObject()));
	}
	
	
//	protected abstract IModel<IW> getItemWrapper(IModel<PrismContainerValueWrapper<C>> rowModel, ItemName itemName);
	
	
	protected abstract String createLabel(VW object);
	protected abstract Panel createValuePanel(IModel<?> headerModel, VW object);
	
	protected abstract Component createHeader(String componentId, IModel<PrismContainerWrapper<C>> mainModel);

//	public QName getqNameOfItem() {
//		return qNameOfItem;
//	}
	
	public PageBase getPageBase() {
		return pageBase;
	}
	
	/**
	 * @return the readOnly
	 */
	public boolean isReadOnly() {
		return readOnly;
	}
	
	protected IModel<PrismContainerWrapper<C>> getMainModel() {
		return mainModel;
	}
	
	
//	protected static IModel<PropertyOrReferenceWrapper> getPropertyOrReferenceForHeaderWrapper(final IModel<ContainerWrapperImpl<Containerable>> model, ItemName name, PageBase pageBase){
//		Validate.notNull(model, "no model");
//		Validate.notNull(model.getObject(), "no model object");
//		return new IModel<PropertyOrReferenceWrapper>() {
//
//			@Override
//			public PropertyOrReferenceWrapper getObject() {
//				if(model.getObject().getValues().size() < 1) {
//		    		ContainerValueWrapper<Containerable> value = WebModelServiceUtils.createNewItemContainerValueWrapper(pageBase, model);
//		    		return value.findPropertyWrapperByName(name);
//		    	} else {
//		    		return model.getObject().getValues().get(0).findPropertyWrapperByName(name);
//		    	}
//			}
//			
//		};
//	}
}

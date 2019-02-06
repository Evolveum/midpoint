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
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.ItemWrapperOld;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.component.prism.PrismPropertyHeaderPanel;
import com.evolveum.midpoint.gui.impl.prism.ContainerWrapperImpl;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.web.component.prism.ContainerValueWrapper;
import com.evolveum.midpoint.web.component.prism.PropertyOrReferenceWrapper;

/**
 * @author skublik
 */
public abstract class AbstractItemWrapperColumn<C extends Containerable> extends AbstractColumn<ContainerValueWrapper<C>, String> {

	protected IModel<ItemWrapperOld> headerModel;
	protected PageBase pageBase;
	protected QName qNameOfItem;
	
	public AbstractItemWrapperColumn(final IModel<ContainerWrapperImpl<C>> headerModel, QName name, PageBase pageBase) {
		super(null);
		Validate.notNull(headerModel, "no model");
		Validate.notNull(headerModel.getObject(), "no ContainerWrappe from model");
		Validate.notNull(name, "no qName");
		this.pageBase = pageBase;
		this.headerModel = new IModel<ItemWrapperOld>() {
			private static final long serialVersionUID = 1L;

			@Override
			public ItemWrapperOld getObject() {
				if(headerModel.getObject().getValues().size() < 1) {
		    		ContainerValueWrapper<C> value = WebModelServiceUtils.createNewItemContainerValueWrapper(pageBase, headerModel);
		    		return value.findItemWrapper(name);
		    	} else {
		    		return headerModel.getObject().getValues().get(0).findItemWrapper(name);
		    	}
			}
			
		};
		qNameOfItem = name;
	}
	
	public <IW extends ItemWrapperOld> AbstractItemWrapperColumn(final IModel<IW> headerModel, PageBase pageBase) {
		super(null);
		this.pageBase = pageBase;
		Validate.notNull(headerModel, "no model");
		Validate.notNull(headerModel.getObject(), "no ContainerWrappe from model");
		this.headerModel = (IModel<ItemWrapperOld>) headerModel;
		qNameOfItem = headerModel.getObject().getName();
	}
	
	@Override
	public Component getHeader(String componentId) {
		PrismPropertyHeaderPanel<ItemWrapperOld> header = new PrismPropertyHeaderPanel<ItemWrapperOld>(componentId, headerModel, getPageBase()) {
			@Override
			public String getContainerLabelCssClass() {
				return " col-xs-12 ";
			}
		};
		return header;
	}

	public QName getqNameOfItem() {
		return qNameOfItem;
	}
	
	public PageBase getPageBase() {
		return pageBase;
	}
	
	protected static IModel<PropertyOrReferenceWrapper> getPropertyOrReferenceForHeaderWrapper(final IModel<ContainerWrapperImpl<Containerable>> model, ItemName name, PageBase pageBase){
		Validate.notNull(model, "no model");
		Validate.notNull(model.getObject(), "no model object");
		return new IModel<PropertyOrReferenceWrapper>() {

			@Override
			public PropertyOrReferenceWrapper getObject() {
				if(model.getObject().getValues().size() < 1) {
		    		ContainerValueWrapper<Containerable> value = WebModelServiceUtils.createNewItemContainerValueWrapper(pageBase, model);
		    		return value.findPropertyWrapperByName(name);
		    	} else {
		    		return model.getObject().getValues().get(0).findPropertyWrapperByName(name);
		    	}
			}
			
		};
	}
}

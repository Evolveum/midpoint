/*
 * Copyright (c) 2010-2017 Evolveum
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

package com.evolveum.midpoint.gui.impl.component.prism;

import com.evolveum.midpoint.gui.api.factory.RealValuable;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.factory.ItemRealValueModel;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismReferenceDefinition;
import com.evolveum.midpoint.prism.Referencable;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.prism.ContainerValueWrapper;
import com.evolveum.midpoint.web.component.prism.ItemVisibilityHandler;
import com.evolveum.midpoint.web.component.prism.ItemWrapper;
import com.evolveum.midpoint.web.component.prism.ValueWrapper;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.*;

/**
 * @author lazyman
 */
public class StaticItemWrapperColumnPanel<IW extends ItemWrapper> extends PrismPropertyColumnPanel<IW> {
	private static final long serialVersionUID = 1L;

	private static final Trace LOGGER = TraceManager.getTrace(StaticItemWrapperColumnPanel.class);

    public StaticItemWrapperColumnPanel(String id, IModel<IW> model, Form form, ItemVisibilityHandler visibilityHandler,
			PageBase pageBase) {
		super(id, model, form, visibilityHandler, pageBase);
	}

    @Override
    protected <T> WebMarkupContainer getValues(String idComponent, IModel<IW> model, Form form) {
    	
    	ListView<RealValuable> values = new ListView<RealValuable>(idComponent, new PropertyModel<>(model, "values")) {
    		private static final long serialVersionUID = 1L;

    		@Override
    		protected void populateItem(ListItem<RealValuable> item) {
    			IModel<String> value = null; 
    			if(item.getModelObject() instanceof ValueWrapper) {
    				if(((ValueWrapper)item.getModelObject()).getItem().getItemDefinition() instanceof PrismReferenceDefinition) {
    					value = populateReferenceItem((ValueWrapper)item.getModelObject());
	    		
    				} else if (((ValueWrapper)item.getModelObject()).getItem().getItemDefinition() instanceof PrismPropertyDefinition) {
    					value = populatePropertyItem((ValueWrapper)item.getModelObject());
    				}
    			} else if(item.getModelObject() instanceof ContainerValueWrapper) {
    				value = populateContainerItem((ContainerValueWrapper)item.getModelObject());
    			}
    			item.add(getDisplayComponent("value", value));
		       
    			item.add(AttributeModifier.append("class", " col-xs-12 "));
    		}
    	};
    	return values;
    }
    
    protected Component getDisplayComponent(String componentId, IModel<String> value) {
    	return new Label(componentId, value);
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
    
    protected IModel<String> populatePropertyItem(ValueWrapper object) {
    	return new ItemRealValueModel<String>(object);
	}
    
    protected IModel<String> populateContainerItem(ContainerValueWrapper object) {
    	return Model.of("");
	}
}

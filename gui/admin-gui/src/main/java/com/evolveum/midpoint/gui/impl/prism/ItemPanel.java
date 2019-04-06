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
package com.evolveum.midpoint.gui.impl.prism;

import java.util.List;

import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LambdaModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.factory.GuiComponentFactory;
import com.evolveum.midpoint.gui.api.prism.ItemWrapper;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismValue;

/**
 * @author katka
 *
 */
public abstract class ItemPanel<VW extends PrismValueWrapper, IW extends ItemWrapper> extends BasePanel<IW>{

	private static final long serialVersionUID = 1L;
	
	private static final String ID_HEADER = "header";
	private static final String ID_VALUES = "values";
	
	public ItemPanel(String id, IModel<IW> model) {
		super(id, model);
	}

	@Override
	protected void onInitialize() {
		super.onInitialize();
		initLayout();
	}
	
	private void initLayout() {
		
		Panel headerPanel = createHeaderPanel();
		add(headerPanel);
		
		ListView<VW> valuesPanel = createValuesPanel();
		add(valuesPanel);
		
	}
	
	 protected abstract Panel createHeaderPanel();
	 
	 protected ListView<VW> createValuesPanel() {
		 
		 ListView<VW> values = new ListView<VW>(ID_VALUES, new PropertyModel<>(getModel(), "values")) { 
		 
			private static final long serialVersionUID = 1L;

			@Override
			protected void populateItem(ListItem<VW> item) {
				 GuiComponentFactory componentFactory = getPageBase().getRegistry().findValuePanelFactory(ItemPanel.this.getModelObject());
				 
				 createValuePanel(item, componentFactory);
			}
		 
		 };
		 
//		        ListView<ValueWrapper<T>> values = new ListView<ValueWrapper<T>>(idComponent,
//		            new PropertyModel<>(model, "values")) {
//		        	private static final long serialVersionUID = 1L;
//
//		            @Override
//		            protected void populateItem(final ListItem<ValueWrapper<T>> item) {
//		            	
//		        		
//		        };
//		        values.add(new AttributeModifier("class", getValuesClass()));
		        values.setReuseItems(true);
		        return values;
		    }
		    
		    //VALUE REGION


	 protected abstract void createValuePanel(ListItem<VW> item, GuiComponentFactory componentFactory);
	 
}

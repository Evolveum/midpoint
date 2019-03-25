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

import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.factory.GuiComponentFactory;
import com.evolveum.midpoint.gui.api.prism.PrismContainerWrapper;
import com.evolveum.midpoint.gui.impl.factory.ItemPanelContext;
import com.evolveum.midpoint.gui.impl.factory.PrismContainerPanelContext;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;

/**
 * @author katka
 *
 */
public class PrismContainerPanel<C extends Containerable> extends ItemPanel<PrismContainerValueWrapper<C>, PrismContainerWrapper<C>>{

	private static final long serialVersionUID = 1L;
	
	private static final String ID_HEADER = "header";
	
	/**
	 * @param id
	 * @param model
	 */
	public PrismContainerPanel(String id, IModel<PrismContainerWrapper<C>> model) {
		super(id, model);
	}

	@Override
	protected Panel createHeaderPanel() {
		return new PrismContainerHeaderPanel<>(ID_HEADER, getModel());
	}

	@Override
	protected void createValuePanel(ListItem item, GuiComponentFactory componentFactory) {
		if (componentFactory == null) {
			PrismContainerValuePanel<C, PrismContainerValueWrapper<C>> valuePanel = new PrismContainerValuePanel<>("value", item.getModel());
			valuePanel.setOutputMarkupId(true);
			item.add(valuePanel);
			item.setOutputMarkupId(true);
			return;
		}
		
		
		PrismContainerPanelContext<C> panelCtx = new PrismContainerPanelContext<>(getModel());
		Panel panel = componentFactory.createPanel(panelCtx);
		item.add(panel);
		 
	}

}

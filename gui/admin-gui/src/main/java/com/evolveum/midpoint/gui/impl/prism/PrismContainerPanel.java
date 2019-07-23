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

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.factory.GuiComponentFactory;
import com.evolveum.midpoint.gui.api.prism.PrismContainerWrapper;
import com.evolveum.midpoint.gui.impl.factory.PrismContainerPanelContext;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

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
	public PrismContainerPanel(String id, IModel<PrismContainerWrapper<C>> model, ItemVisibilityHandler visibilitytHandler) {
		super(id, model, visibilitytHandler);
	}
	
	@Override
	protected void onInitialize() {
		super.onInitialize();
		
		
		add(AttributeModifier.append("class", () -> {
			String cssClasses = "";
			
			if (getModelObject() != null && getModelObject().isShowOnTopLevel()) {
				cssClasses = "top-level-prism-container";
			}
			
			if (getModelObject() != null && getModelObject().isMultiValue()) {
				cssClasses = " multivalue-container";
			}
			return cssClasses;
		}));
		
	}

	@Override
	protected Panel createHeaderPanel() {
		return new PrismContainerHeaderPanel<>(ID_HEADER, getModel());
	}
	
	@Override
	protected boolean getHeaderVisibility() {
		if(!super.getHeaderVisibility()) {
			return false;
		}
		return getModelObject() != null && getModelObject().isMultiValue();
	}

	@Override
	protected Component createValuePanel(ListItem<PrismContainerValueWrapper<C>> item, GuiComponentFactory componentFactory, ItemVisibilityHandler visibilityHandler) {
		if (componentFactory == null) {
			PrismContainerValuePanel<C, PrismContainerValueWrapper<C>> valuePanel = new PrismContainerValuePanel<C, PrismContainerValueWrapper<C>>("value", item.getModel(), getVisibilityHandler());
			valuePanel.setOutputMarkupId(true);
			valuePanel.add(new VisibleBehaviour(() -> getModelObject() != null && (getModelObject().isExpanded() || getModelObject().isSingleValue())));
			if(PrismContainerPanel.this.getModelObject() != null && PrismContainerPanel.this.getModelObject().isSingleValue()) {
				valuePanel.add(AttributeModifier.append("class", "singlevalue-container"));
			}
			item.add(valuePanel);
			item.setOutputMarkupId(true);
			return valuePanel;
		}
		
		
		PrismContainerPanelContext<C> panelCtx = new PrismContainerPanelContext<>(getModel());
		Panel panel = componentFactory.createPanel(panelCtx);
		panel.setOutputMarkupId(true);
		item.add(panel);
		return panel;
		 
	}
	
	@Override
	protected void createButtons(ListItem<PrismContainerValueWrapper<C>> item) {
		//nothing to do.. buttons are in the prism container panel header/ prism container value header
	}

}

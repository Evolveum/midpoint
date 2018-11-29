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
package com.evolveum.midpoint.gui.impl.factory;

import javax.annotation.PostConstruct;

import org.apache.wicket.markup.html.panel.Panel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.gui.api.component.path.ItemPathDto;
import com.evolveum.midpoint.gui.api.component.path.ItemPathPanel;
import com.evolveum.midpoint.gui.api.factory.AbstractGuiComponentFactory;
import com.evolveum.midpoint.gui.api.registry.GuiComponentRegistry;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.web.component.prism.ItemWrapper;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

/**
 * @author katka
 *
 */
@Component
public class ItemPathPanelFactory extends AbstractGuiComponentFactory {

	@Autowired private GuiComponentRegistry registry;

	@PostConstruct
	public void register() {
		registry.addToRegistry(this);
	}
	
	@Override
	public <T> boolean match(ItemWrapper itemWrapper) {
		return ItemPathType.COMPLEX_TYPE.equals(itemWrapper.getItemDefinition().getTypeName());
	}

	@Override
	public <T> Panel getPanel(PanelContext<T> panelCtx) {
		return new ItemPathPanel(panelCtx.getComponentId(), (ItemPathType) panelCtx.getRealValueModel().getObject()) {
			
			private static final long serialVersionUID = 1L;

			@Override
			protected void onUpdate(ItemPathDto itemPathDto) {
				((PrismPropertyValue<ItemPathType>) panelCtx.getBaseModel().getObject().getValue()).setValue(new ItemPathType(itemPathDto.toItemPath())); 
				
			}
		};

	}
	
	
	
}

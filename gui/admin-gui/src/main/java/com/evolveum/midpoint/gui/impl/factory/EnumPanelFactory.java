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
import javax.annotation.Priority;

import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.gui.api.factory.AbstractGuiComponentFactory;
import com.evolveum.midpoint.gui.api.registry.GuiComponentRegistry;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.web.component.prism.ItemWrapper;

/**
 * @author katka
 *
 */
@Component
public class EnumPanelFactory extends AbstractGuiComponentFactory {

	@Autowired GuiComponentRegistry registry;
	
	@PostConstruct
	public void register() {
		registry.addToRegistry(this);
	}
	
	@Override
	public <T> boolean match(ItemWrapper itemWrapper) {
		if (itemWrapper.getItem() instanceof PrismReference) {
			return false;
		}
		 return isEnum((PrismProperty) itemWrapper.getItem());
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.gui.impl.factory.GuiComponentFactory#createPanel(com.evolveum.midpoint.gui.impl.factory.PanelContext)
	 */
	@Override
	public <T> Panel getPanel(PanelContext<T> panelCtx) {
		Class clazz = panelCtx.getTypeClass();

		if (clazz != null) {
			return WebComponentUtil.createEnumPanel(clazz, panelCtx.getComponentId(), (IModel) panelCtx.getRealValueModel(),
					panelCtx.getParentComponent());
		}

		return WebComponentUtil.createEnumPanel(panelCtx.getAllowedValues(), panelCtx.getComponentId(),
				panelCtx.getRealValueModel(), panelCtx.getParentComponent());
	}
	
	private boolean isEnum(PrismProperty property) {
		PrismPropertyDefinition definition = property.getDefinition();
		if (definition == null) {
			return property.getValueClass().isEnum();
		}
		return (definition.getAllowedValues() != null && definition.getAllowedValues().size() > 0);
	}

}

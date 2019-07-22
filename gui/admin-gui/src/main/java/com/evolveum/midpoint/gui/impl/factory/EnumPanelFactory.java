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
import org.apache.wicket.model.IModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.gui.api.factory.AbstractGuiComponentFactory;
import com.evolveum.midpoint.gui.api.prism.ItemWrapper;
import com.evolveum.midpoint.gui.api.registry.GuiComponentRegistry;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.prism.PrismPropertyWrapper;

/**
 * @author katka
 *
 */
@Component
public class EnumPanelFactory<T extends Enum<?>> extends AbstractGuiComponentFactory<T> {

	private static final long serialVersionUID = 1L;
	
	@Autowired GuiComponentRegistry registry;
	
	@PostConstruct
	public void register() {
		registry.addToRegistry(this);
	}
	
	private boolean isEnum(ItemWrapper<?, ?, ?, ?> property) {
		
		if (!(property instanceof PrismPropertyWrapper)) {
			return false;
		}

		Class<T> valueType = property.getTypeClass();
		if (valueType == null) {
			valueType = property.getPrismContext() != null ?
					property.getPrismContext().getSchemaRegistry().getCompileTimeClass(property.getTypeName()) : null;
		}
		
		if (valueType != null) {
			return valueType.isEnum();
		}
		
		return (((PrismPropertyWrapper)property).getAllowedValues() != null && ((PrismPropertyWrapper)property).getAllowedValues().size() > 0);
	}

	@Override
	public <IW extends ItemWrapper> boolean match(IW wrapper) {
		return (isEnum(wrapper));
	}

	@Override
	protected Panel getPanel(PrismPropertyPanelContext<T> panelCtx) {
		Class<T> clazz = panelCtx.getTypeClass();

		if (clazz != null) {
			return WebComponentUtil.createEnumPanel(clazz, panelCtx.getComponentId(), (IModel<T>) panelCtx.getRealValueModel(),
					panelCtx.getParentComponent());
		}

		return WebComponentUtil.createEnumPanel(panelCtx.getAllowedValues(), panelCtx.getComponentId(),
				panelCtx.getRealValueModel(), panelCtx.getParentComponent());

	}

}

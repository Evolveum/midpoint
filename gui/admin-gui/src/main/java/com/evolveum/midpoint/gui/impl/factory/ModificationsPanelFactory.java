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
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.gui.api.factory.AbstractGuiComponentFactory;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.ItemWrapperOld;
import com.evolveum.midpoint.gui.api.registry.GuiComponentRegistry;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.component.model.delta.DeltaDto;
import com.evolveum.midpoint.web.component.model.delta.ModificationsPanel;
import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;

/**
 * @author katka
 *
 */
@Component
@Priority(1000)
public class ModificationsPanelFactory extends AbstractGuiComponentFactory {

	@Autowired private GuiComponentRegistry registry;
	
	@PostConstruct
	public void register() {
		registry.addToRegistry(this);
	}
	
	@Override
	public <T> boolean match(ItemWrapperOld itemWrapper) {
		return ObjectDeltaType.COMPLEX_TYPE.equals(itemWrapper.getItemDefinition().getTypeName());
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.gui.api.factory.GuiComponentFactory#createPanel(com.evolveum.midpoint.gui.impl.factory.PanelContext)
	 */
	@Override
	public <T> Panel getPanel(PanelContext<T> panelCtx) {
		return new ModificationsPanel(panelCtx.getComponentId(), () -> {
			ItemRealValueModel<T> model = panelCtx.getRealValueModel();
			if (model == null || model.getObject() == null) {
				return null;
			}
			
			PrismContext prismContext = panelCtx.getPrismContext();
			ObjectDeltaType objectDeltaType = (ObjectDeltaType) model.getObject();
			try {
				ObjectDelta delta = DeltaConvertor.createObjectDelta(objectDeltaType, prismContext);
				return new DeltaDto(delta);
			} catch (SchemaException e) {
				throw new IllegalStateException("Couldn't convert object delta: " + objectDeltaType);
			}
		});	
	}

}

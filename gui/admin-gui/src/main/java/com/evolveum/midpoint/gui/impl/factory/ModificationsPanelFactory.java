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

import com.evolveum.midpoint.gui.api.factory.AbstractGuiComponentFactory;
import com.evolveum.midpoint.gui.api.prism.ItemWrapper;
import com.evolveum.midpoint.gui.api.registry.GuiComponentRegistry;
import com.evolveum.midpoint.gui.impl.prism.PrismPropertyWrapper;
import com.evolveum.midpoint.prism.PrismContext;
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
public class ModificationsPanelFactory extends AbstractGuiComponentFactory<ObjectDeltaType> {

	private static final long serialVersionUID = 1L;
	
	@Autowired private GuiComponentRegistry registry;
	
	@PostConstruct
	public void register() {
		registry.addToRegistry(this);
	}
	
	@Override
	public boolean match(ItemWrapper<?, ?, ?, ?> wrapper) {
		return ObjectDeltaType.COMPLEX_TYPE.equals(wrapper.getTypeName());
	}

	@Override
	protected Panel getPanel(PrismPropertyPanelContext<ObjectDeltaType> panelCtx) {
		return new ModificationsPanel(panelCtx.getComponentId(), () -> {
			ItemRealValueModel<ObjectDeltaType> model = panelCtx.getRealValueModel();
			if (model == null || model.getObject() == null) {
				return null;
			}
			
			PrismContext prismContext = panelCtx.getPrismContext();
			ObjectDeltaType objectDeltaType = model.getObject();
			try {
				ObjectDelta<?> delta = DeltaConvertor.createObjectDelta(objectDeltaType, prismContext);
				return new DeltaDto(delta);
			} catch (SchemaException e) {
				throw new IllegalStateException("Couldn't convert object delta: " + objectDeltaType);
			}
		});	
	}

}

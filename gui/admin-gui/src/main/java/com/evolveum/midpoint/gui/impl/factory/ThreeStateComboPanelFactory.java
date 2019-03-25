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
import com.evolveum.midpoint.gui.api.registry.GuiComponentRegistry;
import com.evolveum.midpoint.gui.impl.prism.PrismPropertyWrapper;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.web.component.input.TriStateComboPanel;

/**
 * @author katka
 *
 */
@Component
public class ThreeStateComboPanelFactory extends AbstractGuiComponentFactory<Boolean> {

	private static final long serialVersionUID = 1L;
	
	@Autowired private GuiComponentRegistry registry;
	
	@PostConstruct
	public void register() {
		registry.addToRegistry(this);
	}
	@Override
	public boolean match(PrismPropertyWrapper<Boolean> wrapper) {
		return DOMUtil.XSD_BOOLEAN.equals(wrapper.getTypeName());
	}

	@Override
	protected Panel getPanel(PrismPropertyPanelContext<Boolean> panelCtx) {
		return new TriStateComboPanel(panelCtx.getComponentId(), (IModel<Boolean>) panelCtx.getRealValueModel());
	}
	
	
	
}

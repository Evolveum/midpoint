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
import org.apache.wicket.model.PropertyModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.gui.api.factory.AbstractGuiComponentFactory;
import com.evolveum.midpoint.gui.api.registry.GuiComponentRegistry;
import com.evolveum.midpoint.web.component.LockoutStatusPanel;
import com.evolveum.midpoint.web.component.prism.ItemWrapper;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;

@Component
public class LockoutStatusPanelFactory extends AbstractGuiComponentFactory {

	@Autowired GuiComponentRegistry registry;
	
//	@Override
	@PostConstruct
	public void register() {
		registry.addToRegistry(this);
	}

	@Override
	public <T> boolean match(ItemWrapper itemWrapper) {
		return ActivationType.F_LOCKOUT_STATUS.equals(itemWrapper.getItemDefinition().getName());
	}

	@Override
	public <T> Panel getPanel(PanelContext<T> panelCtx) {
		return new LockoutStatusPanel(panelCtx.getComponentId(), panelCtx.getBaseModel().getObject(), new PropertyModel<>(panelCtx.getBaseModel(), panelCtx.getBaseExpression()));
	}

}

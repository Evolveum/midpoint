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

import org.apache.wicket.feedback.ComponentFeedbackMessageFilter;
import org.apache.wicket.markup.html.panel.Panel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.gui.api.factory.GuiComponentFactory;
import com.evolveum.midpoint.gui.api.prism.ItemWrapper;
import com.evolveum.midpoint.gui.api.registry.GuiComponentRegistry;
import com.evolveum.midpoint.gui.impl.prism.PrismPropertyWrapper;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.web.component.LockoutStatusPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LockoutStatusType;

@Component
public class LockoutStatusPanelFactory implements GuiComponentFactory<PrismPropertyPanelContext<LockoutStatusType>> {

	private static final long serialVersionUID = 1L;
	
	@Autowired GuiComponentRegistry registry;
	
	@PostConstruct
	public void register() {
		registry.addToRegistry(this);
	}

	@Override
	public Integer getOrder() {
		return 1000;
	}

	@Override
	public <IW extends ItemWrapper> boolean match(IW wrapper) {
		return ActivationType.F_LOCKOUT_STATUS.equals(wrapper.getName());
	}

	@Override
	public Panel createPanel(PrismPropertyPanelContext<LockoutStatusType> panelCtx) {
		LockoutStatusPanel panel = new LockoutStatusPanel(panelCtx.getComponentId(), panelCtx.getRealValueModel());
		panelCtx.getFeedbackPanel().setFilter(new ComponentFeedbackMessageFilter(panel));
		return panel;
	}
	
}

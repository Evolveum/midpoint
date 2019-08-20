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

import java.io.Serializable;

import javax.annotation.PostConstruct;

import org.apache.wicket.markup.html.panel.Panel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.gui.api.factory.AbstractGuiComponentFactory;
import com.evolveum.midpoint.gui.api.prism.ItemWrapper;
import com.evolveum.midpoint.gui.api.registry.GuiComponentRegistry;
import com.evolveum.midpoint.gui.impl.prism.PrismPropertyWrapper;
import com.evolveum.midpoint.web.component.input.TextAreaPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CleanupPoliciesType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.prism.xml.ns._public.query_3.QueryType;

@Component
public class TextAreaPanelFactory<T extends Serializable> extends AbstractGuiComponentFactory<T> {

	private static final long serialVersionUID = 1L;
	
	@Autowired private GuiComponentRegistry registry;
	
	@PostConstruct
	public void register() {
		registry.addToRegistry(this);
	}

	@Override
	public <IW extends ItemWrapper> boolean match(IW wrapper) {
		return FocusType.F_DESCRIPTION.equals(wrapper.getItemName()) || QueryType.COMPLEX_TYPE.equals(wrapper.getTypeName()); // || CleanupPoliciesType.COMPLEX_TYPE.equals(wrapper.getTypeName());
	}

	@Override
	protected Panel getPanel(PrismPropertyPanelContext<T> panelCtx) {
		int size = 10;
		if (FocusType.F_DESCRIPTION.equals(panelCtx.getDefinitionName())) {
			size = 1;
		}
		return new TextAreaPanel<>(panelCtx.getComponentId(), panelCtx.getRealValueModel(), size);
	}



	
}

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

import java.util.List;

import javax.annotation.PostConstruct;
import javax.xml.namespace.QName;

import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.gui.api.factory.GuiComponentFactory;
import com.evolveum.midpoint.gui.api.prism.ItemWrapper;
import com.evolveum.midpoint.gui.api.registry.GuiComponentRegistry;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.page.admin.configuration.component.ProfilingLevel;
import com.evolveum.midpoint.gui.impl.prism.PrismPropertyWrapper;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.web.component.input.DropDownChoicePanel;
import com.evolveum.midpoint.web.component.input.QNameObjectTypeChoiceRenderer;
import com.evolveum.midpoint.web.page.admin.configuration.component.EmptyOnChangeAjaxFormUpdatingBehavior;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LoggingLevelType;

/**
 * @author katkav
 *
 */
@Component
public class DropDownChoicePanelFactory implements GuiComponentFactory<PrismPropertyPanelContext<QName>> {

	private static final long serialVersionUID = 1L;
	@Autowired GuiComponentRegistry registry;
	
	@PostConstruct
	public void register() {
		registry.addToRegistry(this);
	}
	@Override
	public <IW extends ItemWrapper> boolean match(IW wrapper) {
		return AssignmentType.F_FOCUS_TYPE.equals(wrapper.getName()) || DOMUtil.XSD_QNAME.equals(wrapper.getTypeName());
	}

		@Override
	public Panel createPanel(PrismPropertyPanelContext<QName> panelCtx) {
		List<QName> typesList = null;
		if (AssignmentType.F_FOCUS_TYPE.equals(panelCtx.getDefinitionName())){
			typesList = WebComponentUtil.createFocusTypeList();
		} else {
			typesList = WebComponentUtil.createObjectTypeList();
		}
		
		DropDownChoicePanel<QName> typePanel = new DropDownChoicePanel<QName>(panelCtx.getComponentId(), (IModel<QName>) panelCtx.getRealValueModel(),
				Model.ofList(typesList), new QNameObjectTypeChoiceRenderer(), true);
		typePanel.getBaseFormComponent().add(new EmptyOnChangeAjaxFormUpdatingBehavior());
		typePanel.setOutputMarkupId(true);
		return typePanel;
	}
		

	@Override
	public Integer getOrder() {
		return 10000;
	}

}

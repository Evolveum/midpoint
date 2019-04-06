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
import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.wicket.extensions.yui.calendar.DateTimeField;
import org.apache.wicket.markup.html.panel.Panel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.gui.api.factory.AbstractGuiComponentFactory;
import com.evolveum.midpoint.gui.api.prism.ItemWrapper;
import com.evolveum.midpoint.gui.api.registry.GuiComponentRegistry;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.prism.PrismPropertyWrapper;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.web.component.input.DatePanel;
import com.evolveum.midpoint.web.util.DateValidator;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;

/**
 * @author katka
 *
 */
@Component
public class DatePanelFactory extends AbstractGuiComponentFactory<XMLGregorianCalendar> {

	private static final long serialVersionUID = 1L;
	
	@Autowired GuiComponentRegistry registry;
	
	@PostConstruct
	public void register() {
		registry.addToRegistry(this);
	}
	@Override
	public <IW extends ItemWrapper> boolean match(IW wrapper) {
		return DOMUtil.XSD_DATETIME.equals(wrapper.getTypeName());
	}

	@Override
	protected Panel getPanel(PrismPropertyPanelContext<XMLGregorianCalendar> panelCtx) {
		DatePanel panel = new DatePanel(panelCtx.getComponentId(), panelCtx.getRealValueModel());
		
		DateValidator validator = WebComponentUtil.getRangeValidator(panelCtx.getForm(), SchemaConstants.PATH_ACTIVATION);
		if (ActivationType.F_VALID_FROM.equals(panelCtx.getDefinitionName())) {
			validator.setDateFrom((DateTimeField) panel.getBaseFormComponent());
		} else if (ActivationType.F_VALID_TO.equals(panelCtx.getDefinitionName())) {
			validator.setDateTo((DateTimeField) panel.getBaseFormComponent());
		} 
		
		return panel;
	}
	
	
}

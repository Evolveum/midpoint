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
import java.util.Iterator;

import javax.annotation.PostConstruct;
import javax.xml.namespace.QName;

import org.apache.wicket.markup.html.panel.Panel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.gui.api.component.autocomplete.AutoCompleteTextPanel;
import com.evolveum.midpoint.gui.api.factory.AbstractGuiComponentFactory;
import com.evolveum.midpoint.gui.api.prism.ItemWrapper;
import com.evolveum.midpoint.gui.api.registry.GuiComponentRegistry;
import com.evolveum.midpoint.gui.impl.prism.PrismPropertyWrapper;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.web.component.input.TextPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LookupTableType;

@Component
public class TextPanelFactory<T> extends AbstractGuiComponentFactory<T> implements Serializable {

	private static final long serialVersionUID = 1L;
	
	@Autowired transient GuiComponentRegistry registry;

	@PostConstruct
	public void register() {
		registry.addToRegistry(this);
	}
	@Override
	public <IW extends ItemWrapper> boolean match(IW wrapper) {
		QName type = wrapper.getTypeName();
		return SchemaConstants.T_POLY_STRING_TYPE.equals(type) || DOMUtil.XSD_STRING.equals(type) || DOMUtil.XSD_DURATION.equals(type)
				|| DOMUtil.XSD_ANYURI.equals(type) || DOMUtil.XSD_INT.equals(type);
	}

	@Override
	protected Panel getPanel(PrismPropertyPanelContext<T> panelCtx) {
		LookupTableType lookupTable = panelCtx.getPredefinedValues();
		if (lookupTable == null) {
			return new TextPanel<>(panelCtx.getComponentId(),
					panelCtx.getRealValueModel(), panelCtx.getTypeClass());
		}
		
		return new AutoCompleteTextPanel<T>(panelCtx.getComponentId(),
				panelCtx.getRealValueModel(), panelCtx.getTypeClass(), panelCtx.hasValueEnumerationRef(), lookupTable) {

			private static final long serialVersionUID = 1L;

			@Override
			public Iterator<T> getIterator(String input) {
				return (Iterator<T>) prepareAutoCompleteList(input, lookupTable).iterator();
			}
		};
	}

}

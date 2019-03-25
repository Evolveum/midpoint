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

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.gui.api.factory.AbstractGuiComponentFactory;
import com.evolveum.midpoint.gui.api.registry.GuiComponentRegistry;
import com.evolveum.midpoint.gui.impl.prism.PrismPropertyWrapper;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.page.admin.reports.component.AceEditorPanel;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;

@Component
public class SearchFilterPanelFactory extends AbstractGuiComponentFactory<SearchFilterType> {

	private static final long serialVersionUID = 1L;

	private static transient Trace LOGGER = TraceManager.getTrace(SearchFilterPanelFactory.class);
	
	@Autowired GuiComponentRegistry registry;
	
	@PostConstruct
	public void register() {
		registry.addToRegistry(this);
	}
	
	@Override
	public boolean match(PrismPropertyWrapper<SearchFilterType> wrapper) {
		return SearchFilterType.COMPLEX_TYPE.equals(wrapper.getTypeName());
	}

	@Override
	protected Panel getPanel(PrismPropertyPanelContext<SearchFilterType> panelCtx) {
		return new AceEditorPanel(panelCtx.getComponentId(), null, new SearchFilterTypeModel((IModel<SearchFilterType>) panelCtx.getRealValueModel(), panelCtx.getPrismContext()));
	}
	
	class SearchFilterTypeModel implements IModel<String> {
		
		private static final long serialVersionUID = 1L;
		
		private IModel<SearchFilterType> baseModel;
		private PrismContext prismCtx;
		
		public SearchFilterTypeModel(IModel<SearchFilterType> valueWrapper, PrismContext prismCtx) {
			this.baseModel = valueWrapper;
			this.prismCtx = prismCtx;
		}

		@Override
		public void detach() {
			// TODO Auto-generated method stub
			
		}

		@Override
		public String getObject() {
			try {
				SearchFilterType value = baseModel.getObject();
				if (value == null) {
					return null;
				}
				
				return prismCtx.xmlSerializer().serializeRealValue(value);
			} catch (SchemaException e) {
				// TODO handle!!!!
				LoggingUtils.logUnexpectedException(LOGGER, "Cannot serialize filter", e);
//				getSession().error("Cannot serialize filter");
			}
			return null;
		}

		@Override
		public void setObject(String object) {
			if (StringUtils.isBlank(object)) {
				return;
			}
			
			try {
				SearchFilterType filter = prismCtx.parserFor(object).parseRealValue(SearchFilterType.class);
				baseModel.setObject(filter);
			} catch (SchemaException e) {
				// TODO handle!!!!
				LoggingUtils.logUnexpectedException(LOGGER, "Cannot parse filter", e);
//				getSession().error("Cannot parse filter");
			}
			
		}
	}

	
}

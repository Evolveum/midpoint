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

import java.util.Arrays;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.annotation.Priority;
import javax.xml.namespace.QName;

import org.apache.wicket.feedback.ComponentFeedbackMessageFilter;
import org.apache.wicket.markup.html.panel.Panel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.gui.api.factory.GuiComponentFactory;
import com.evolveum.midpoint.gui.api.prism.ItemWrapper;
import com.evolveum.midpoint.gui.api.registry.GuiComponentRegistry;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.PrismReferenceDefinition;
import com.evolveum.midpoint.prism.Referencable;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.web.component.form.ValueChoosePanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractRoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;

@Component
@Priority(1000)
public class ReferencablePanelFactory<R extends Referencable> implements GuiComponentFactory<PrismReferencePanelContext<R>> {

	private static final long serialVersionUID = 1L;
	
	@Autowired GuiComponentRegistry registry;
	
	@PostConstruct
	public void register() {
		registry.addToRegistry(this);
	}
	
	@Override
	public Integer getOrder() {
		return null;
	}

	@Override
	public boolean match(ItemWrapper<?, ?, ?, ?> wrapper) {
		return wrapper.getDefinition() instanceof PrismReferenceDefinition;
	}

	@Override
	public Panel createPanel(PrismReferencePanelContext<R> panelCtx) {
		ValueChoosePanel panel = new ValueChoosePanel(panelCtx.getComponentId(), panelCtx.getRealValueModel()) {

			private static final long serialVersionUID = 1L;
			
			@Override
			protected ObjectFilter createCustomFilter() {
				return panelCtx.getFilter();
			}

			@Override
			protected boolean isEditButtonEnabled() {
				if (getModel() == null) {
					return true;
				}
				
				//TODO only is association
				return getModelObject() == null;
				
			}

			@Override
			public List<QName> getSupportedTypes() {
				List<QName> targetTypeList = panelCtx.getTargetTypes();
				if (targetTypeList == null || WebComponentUtil.isAllNulls(targetTypeList)) {
					return Arrays.asList(ObjectType.COMPLEX_TYPE);
				}
				return targetTypeList;
			}

			@Override
			protected Class getDefaultType(List supportedTypes) {
				if (AbstractRoleType.COMPLEX_TYPE.equals(panelCtx.getTargetTypeName())) {
					return RoleType.class;
				} else {
					return super.getDefaultType(supportedTypes);
				}
			}

		};
		
		panelCtx.getFeedbackPanel().setFilter(new ComponentFeedbackMessageFilter(panel));
		return panel;
	}

	
	
}

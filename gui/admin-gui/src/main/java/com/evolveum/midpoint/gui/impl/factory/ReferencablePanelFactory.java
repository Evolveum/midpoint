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
import javax.xml.namespace.QName;

import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.PropertyModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.gui.api.factory.AbstractGuiComponentFactory;
import com.evolveum.midpoint.gui.api.registry.GuiComponentRegistry;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.PrismReferenceDefinition;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.web.component.form.ValueChoosePanel;
import com.evolveum.midpoint.web.component.prism.ItemWrapper;
import com.evolveum.midpoint.web.component.prism.ReferenceWrapper;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractRoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;

@Component
public class ReferencablePanelFactory extends AbstractGuiComponentFactory {

	@Autowired GuiComponentRegistry registry;
	
	@PostConstruct
	public void register() {
		registry.addToRegistry(this);
	}
	
	@Override
	public <T> boolean match(ItemWrapper itemWrapper) {
		return itemWrapper.getItemDefinition() instanceof PrismReferenceDefinition;
	}

	@Override
	public <T> Panel getPanel(PanelContext<T> panelCtx) {
		return new ValueChoosePanel(panelCtx.getComponentId(), panelCtx.getRealValueModel()) {

			private static final long serialVersionUID = 1L;
			
			@Override
			protected ObjectFilter createCustomFilter() {
				ItemWrapper wrapper = panelCtx.getBaseModel().getObject().getItem();
				if (!(wrapper instanceof ReferenceWrapper)) {
					return null;
				}
				return ((ReferenceWrapper) wrapper).getFilter();
			}

			@Override
			protected boolean isEditButtonEnabled() {
				return panelCtx.getBaseModel().getObject().isEditEnabled();
			}

			@Override
			public List<QName> getSupportedTypes() {
				List<QName> targetTypeList = ((ReferenceWrapper) panelCtx.getBaseModel().getObject().getItem()).getTargetTypes();
				if (targetTypeList == null || WebComponentUtil.isAllNulls(targetTypeList)) {
					return Arrays.asList(ObjectType.COMPLEX_TYPE);
				}
				return targetTypeList;
			}

			@Override
			protected Class getDefaultType(List supportedTypes) {
					if (AbstractRoleType.COMPLEX_TYPE.equals(((PrismReference)panelCtx.getBaseModel().getObject().getItem().getItem()).getDefinition().getTargetTypeName())){
					return RoleType.class;
				} else {
					return super.getDefaultType(supportedTypes);
				}
			}

		};
	}

	
	
}

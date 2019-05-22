/*
 * Copyright (c) 2010-2019 Evolveum
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
package com.evolveum.midpoint.gui.impl.prism;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.prism.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.prism.ShadowWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.prism.ItemVisibility;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CredentialsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * @author skublik
 *
 */
public class ShadowPanel extends BasePanel<ShadowWrapper<ShadowType>> {

	private static final long serialVersionUID = 1L;
	
	private static final Trace LOGGER = TraceManager.getTrace(ShadowPanel.class);
	
	protected static final String ID_SHADOWS_CONTAINER = "shadowContainer";

	private final String ID_SHADOW = "shadow";

	public ShadowPanel(String id, IModel<ShadowWrapper<ShadowType>> model) {
		super(id, model);
	}
	
	@Override
	protected void onInitialize() {
		super.onInitialize();
		initLayout();
		setOutputMarkupId(true);
	}
	
	private void initLayout() {
		
		IModel<PrismObjectValueWrapper<ShadowType>> valueModel = new PropertyModel<>(getModel(), "value");
		
		List<? extends ItemWrapper<?, ?, ?, ?>> items = valueModel.getObject().getItems();
		
		List<PrismContainerWrapper<ShadowType>> containers = new ArrayList<PrismContainerWrapper<ShadowType>>();
		
		for(ItemWrapper<?, ?, ?, ?> item : items) {
			if(QNameUtil.match(item.getName(), ShadowType.F_ATTRIBUTES) ||
					QNameUtil.match(item.getName(), ShadowType.F_ACTIVATION)) {
				containers.add((PrismContainerWrapper<ShadowType>)item);
			}
			if(QNameUtil.match(item.getName(), ShadowType.F_ASSOCIATION) && 
					!((PrismContainerWrapper)item).getValues().isEmpty()) {
				containers.add((PrismContainerWrapper<ShadowType>)item);
			}
			
			if(QNameUtil.match(item.getName(), ShadowType.F_CREDENTIALS)) {
				try {
					containers.add(((PrismContainerWrapper<ShadowType>)item).findContainer(CredentialsType.F_PASSWORD));
				} catch (SchemaException e) {
					e.printStackTrace();
				}
			}
		}
		
		
		final ListView<PrismContainerWrapper<ShadowType>> shadowPanel = new ListView<PrismContainerWrapper<ShadowType>>(
				ID_SHADOW , Model.of((Collection)containers)) {
			private static final long serialVersionUID = 1L;

			@Override
			protected void populateItem(ListItem<PrismContainerWrapper<ShadowType>> item) {
				try {
		    		Panel shadowContainerPanel = getPageBase().initItemPanel(ID_SHADOWS_CONTAINER, item.getModelObject().getTypeName(), item.getModel(), 
		    				itemWrapper -> checkShadowContainerVisibility(itemWrapper, valueModel));
		    		item.add(shadowContainerPanel);
				} catch (SchemaException e) {
					LOGGER.error("Cannot create panel for logging: {}", e.getMessage(), e);
					getSession().error("Cannot create panle for logging");
				}
			}

		};
		add(shadowPanel);
	}
	
	private ItemVisibility checkShadowContainerVisibility(ItemWrapper itemWrapper, IModel<PrismObjectValueWrapper<ShadowType>> valueModel) {
		
		if(itemWrapper.getPath().equivalent(ItemPath.create(ShadowType.F_ASSOCIATION))) {
			if(!((PrismContainerWrapper)itemWrapper).isEmpty() ) {
				return ItemVisibility.AUTO;
			} else {
				return ItemVisibility.HIDDEN;
			}
		}
		
		return WebComponentUtil.checkShadowActivationAndPasswordVisibility(itemWrapper, valueModel);
	}
}

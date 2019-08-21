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

import com.evolveum.midpoint.xml.ns._public.common.common_3.VirtualContainersSpecificationType;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.prism.PrismObjectWrapper;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import java.util.List;

/**
 * @author katka
 *
 */
public class PrismObjectValuePanel<O extends ObjectType> extends BasePanel<PrismObjectWrapper<O>> {

	private static final long serialVersionUID = 1L;
	
	private static final String ID_VALUE = "value";
	private static final String ID_VIRTUAL_CONTAINERS = "virtualContainers";

	private ItemPanelSettings settings;

	public PrismObjectValuePanel(String id, IModel<PrismObjectWrapper<O>> model, ItemPanelSettings settings) {
		super(id, model);
		this.settings = settings;
	}
	
	@Override
	protected void onInitialize() {
		super.onInitialize();
		initLayout();
		setOutputMarkupId(true);
	}
	
	private void initLayout() {
		createValuePanel(ID_VALUE, new PropertyModel<>(getModel(), "value"));
	}

	protected void createValuePanel(String panelId, IModel<PrismObjectValueWrapper<O>> valueModel) {
		
		PrismContainerValuePanel<O, PrismObjectValueWrapper<O>> valueWrapper = new PrismContainerValuePanel<>(panelId, valueModel, settings.getVisibilityHandler());
		valueWrapper.setOutputMarkupId(true);
		add(valueWrapper);

	}




}

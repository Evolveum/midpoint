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
package com.evolveum.midpoint.gui.impl.prism;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.model.StringResourceModel;

import com.evolveum.midpoint.gui.api.prism.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.prism.PrismObjectWrapper;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismObjectValue;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * @author katka
 *
 */
public class PrismObjectValueWrapperImpl<O extends ObjectType> extends PrismContainerValueWrapperImpl<O> implements PrismObjectValueWrapper<O>{

	private static final long serialVersionUID = 1L;

	public PrismObjectValueWrapperImpl(PrismObjectWrapper<O> parent, PrismObjectValue<O> pcv, ValueStatus status) {
		super(parent, pcv, status);
	}
	
	@Override
	public <T extends Containerable> List<PrismContainerWrapper<T>> getContainers() {
		List<PrismContainerWrapper<T>> containers = new ArrayList<>();
		for (ItemWrapper<?, ?, ?, ?> container : getItems()) {

			collectExtensionItems(container, true, containers);

		}
		return containers;
		
	}

	@Override
	public String getDisplayName() {
		return new StringResourceModel("prismContainer.mainPanelDisplayName").getString();
	}
	
}

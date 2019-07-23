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

import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.VirtualContainerItemSpecificationType;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;
import org.apache.commons.collections4.CollectionUtils;
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

	private static final transient Trace LOGGER = TraceManager.getTrace(PrismObjectWrapperImpl.class);

	public PrismObjectValueWrapperImpl(PrismObjectWrapper<O> parent, PrismObjectValue<O> pcv, ValueStatus status) {
		super(parent, pcv, status);
	}
	
	@Override
	public <T extends Containerable> List<PrismContainerWrapper<T>> getContainers() {
		List<PrismContainerWrapper<T>> containers = new ArrayList<>();
		for (ItemWrapper<?, ?, ?, ?> container : getItems()) {

			collectExtensionItems(container, true, containers);

			if (container instanceof  PrismContainerWrapper && ((PrismContainerWrapper) container).isVirtual()) {
				((List)containers).add(container);
			}

		}



		return containers;
		
	}


	@Override
	public String getDisplayName() {
		return new StringResourceModel("prismContainer.mainPanelDisplayName").getString();
	}

	@Override
	public boolean isExpanded() {
		return true;
	}
}

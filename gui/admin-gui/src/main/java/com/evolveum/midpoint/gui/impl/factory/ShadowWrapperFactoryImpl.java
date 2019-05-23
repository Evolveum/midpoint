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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.prism.ShadowWrapper;
import com.evolveum.midpoint.gui.api.registry.GuiComponentRegistry;
import com.evolveum.midpoint.gui.impl.prism.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.prism.PrismObjectValuePanel;
import com.evolveum.midpoint.gui.impl.prism.ShadowWrapperImpl;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * @author katka
 *
 */
@Component
public class ShadowWrapperFactoryImpl extends PrismObjectWrapperFactoryImpl<ShadowType> {
	
	private static final transient Trace LOGGER = TraceManager.getTrace(ShadowWrapperFactoryImpl.class);
	
	@Autowired private GuiComponentRegistry registry;

	public ShadowWrapper createObjectWrapper(PrismObject<ShadowType> object, ItemStatus status, WrapperContext context) throws SchemaException {
		applySecurityConstraints(object, context);
		
		ShadowWrapperImpl shadowWrapper = new ShadowWrapperImpl(object, status);
		context.setShowEmpty(ItemStatus.ADDED == status ? true : false);
		PrismContainerValueWrapper<ShadowType> valueWrapper = createValueWrapper(shadowWrapper, object.getValue(), ItemStatus.ADDED == status ? ValueStatus.ADDED : ValueStatus.NOT_CHANGED, context);
		shadowWrapper.getValues().add(valueWrapper);
		
		registry.registerWrapperPanel(object.getDefinition().getTypeName(), PrismObjectValuePanel.class);
		return shadowWrapper;
		
	}
	
	@Override
	public boolean match(ItemDefinition<?> def) {
		return def instanceof PrismObjectDefinition && QNameUtil.match(def.getTypeName(), ShadowType.COMPLEX_TYPE);
	}

	@Override
	public int getOrder() {
		return 99;
	}

}

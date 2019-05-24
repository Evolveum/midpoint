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

import java.util.List;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.gui.api.prism.ItemWrapper;
import com.evolveum.midpoint.gui.api.registry.GuiComponentRegistry;
import com.evolveum.midpoint.gui.impl.prism.PrismContainerValueWrapper;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MetadataType;

/**
 * @author katka
 *
 */
@Component
public class MetadataWrapperFactory extends PrismContainerWrapperFactoryImpl<MetadataType>{

	
	@Autowired private GuiComponentRegistry registry;

	@Override
	public boolean match(ItemDefinition<?> def) {
		return QNameUtil.match(MetadataType.COMPLEX_TYPE, def.getTypeName());
	}

	@PostConstruct
	@Override
	public void register() {
		registry.addToRegistry(this);
	}

	@Override
	public int getOrder() {
		return 10;
	}
	
	protected void addItemWrapper(ItemDefinition<?> def, PrismContainerValueWrapper<?> containerValueWrapper,
			WrapperContext context, List<ItemWrapper<?,?,?,?>> wrappers) throws SchemaException {
		
		ItemWrapperFactory<?, ?, ?> factory = registry.findWrapperFactory(def);
		
		ItemWrapper<?,?,?,?> wrapper = factory.createWrapper(containerValueWrapper, def, context);
		wrapper.setReadOnly(true);
		wrappers.add(wrapper);
	}
	
	@Override
	public boolean skipCreateWrapper(ItemDefinition<?> def, WrapperContext wrapperContext) {
		return false;
	}
	
}

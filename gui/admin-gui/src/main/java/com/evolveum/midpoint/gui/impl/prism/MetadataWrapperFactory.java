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

import org.springframework.beans.factory.annotation.Autowired;

import com.evolveum.midpoint.gui.impl.factory.ContainerWrapperFactoryImpl;
import com.evolveum.midpoint.gui.impl.registry.GuiComponentRegistryImpl;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MetadataType;
import com.lowagie.text.Meta;

/**
 * @author katka
 *
 */
public class MetadataWrapperFactory extends ContainerWrapperFactoryImpl<MetadataType>{

	
	@Autowired private GuiComponentRegistryImpl registry;
	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.gui.impl.factory.WrapperFactory#match(com.evolveum.midpoint.prism.ItemDefinition)
	 */
	@Override
	public boolean match(ItemDefinition def) {
		return QNameUtil.match(MetadataType.COMPLEX_TYPE, def.getTypeName());
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.gui.impl.factory.WrapperFactory#register()
	 */
	@Override
	public void register() {
		registry.addToRegistry(this);
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.gui.impl.factory.WrapperFactory#getOrder()
	 */
	@Override
	public int getOrder() {
		return 10;
	}

	
}

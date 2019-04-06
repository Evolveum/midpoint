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

package com.evolveum.midpoint.gui.api.registry;

import java.io.Serializable;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.gui.api.factory.GuiComponentFactory;
import com.evolveum.midpoint.gui.api.prism.ItemWrapper;
import com.evolveum.midpoint.gui.impl.factory.ItemWrapperFactory;
import com.evolveum.midpoint.gui.impl.factory.PrismObjectWrapperFactory;
import com.evolveum.midpoint.gui.impl.prism.PrismValueWrapper;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

public interface GuiComponentRegistry extends Serializable {

	public void addToRegistry(GuiComponentFactory factory);
	
	public <T> GuiComponentFactory findValuePanelFactory(ItemWrapper itemWrapper);
	
	void registerWrapperPanel(QName typeName, Class<?> panelClass);
	
	Class<?> getPanelClass(QName typeName);
	
	<IW extends ItemWrapper, VW extends PrismValueWrapper, PV extends PrismValue> ItemWrapperFactory<IW, VW, PV> findWrapperFactory(ItemDefinition<?> def);
	
	<O extends ObjectType> PrismObjectWrapperFactory<O> getObjectWrapperFactory(PrismObjectDefinition<O> objectDef);
	
	void addToRegistry(ItemWrapperFactory factory);
	
	
}

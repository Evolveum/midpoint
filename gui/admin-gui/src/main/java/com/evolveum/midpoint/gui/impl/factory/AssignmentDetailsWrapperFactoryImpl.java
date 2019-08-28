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
package com.evolveum.midpoint.gui.impl.factory;

import javax.xml.namespace.QName;

import org.springframework.stereotype.Component;

import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConstructionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PersonaConstructionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyRuleType;

/**
 * @author katka
 *
 */
@Component
public class AssignmentDetailsWrapperFactoryImpl<C extends Containerable> extends PrismContainerWrapperFactoryImpl<C> {
	
	@Override
	public boolean match(ItemDefinition<?> def) {
		QName typeName = def.getTypeName();
		return PersonaConstructionType.COMPLEX_TYPE.equals(typeName) || MappingsType.COMPLEX_TYPE.equals(typeName) || PolicyRuleType.COMPLEX_TYPE.equals(typeName);
	}

	@Override
	protected boolean canCreateWrapper(ItemDefinition<?> def, ItemStatus status, WrapperContext context, boolean isEmptyValue) {
		if (!isEmptyValue){
			return true;
		}
		return false;
	}

	@Override
	public int getOrder() {
		return super.getOrder() - 10;
	}
}

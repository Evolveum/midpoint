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

import org.springframework.stereotype.Component;

import com.evolveum.midpoint.gui.api.prism.PrismContainerWrapper;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AppenderConfigurationType;

/**
 * @author skublik
 *
 */
@Component
public class AppendersWrapperFactoryImpl<C extends Containerable> extends PrismContainerWrapperFactoryImpl<C>{

	private static final transient Trace LOGGER = TraceManager.getTrace(AppendersWrapperFactoryImpl.class);
	
	@Override
	public boolean match(ItemDefinition<?> def) {
		return QNameUtil.match(def.getTypeName(), AppenderConfigurationType.COMPLEX_TYPE);
	}

	@Override
	public int getOrder() {
		return 10;
	}

	@Override
	protected List<? extends ItemDefinition> getItemDefinitions(PrismContainerWrapper<C> parent,
			PrismContainerValue<C> value) {
		if(value != null && value.getComplexTypeDefinition() != null
				&& value.getComplexTypeDefinition().getDefinitions() != null) {
			return value.getComplexTypeDefinition().getDefinitions();
		}
		return parent.getDefinitions();
	}
}

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

import java.util.Collection;

import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.impl.prism.PrismPropertyWrapper;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.util.DisplayableValue;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LookupTableType;

/**
 * @author katka
 *
 */
public class PrismPropertyPanelContext<T> extends ItemPanelContext<T, PrismPropertyWrapper<T>>{
	
	
	public PrismPropertyPanelContext(IModel<PrismPropertyWrapper<T>> itemWrapper) {
		super(itemWrapper);
	}

	
	public Collection<? extends DisplayableValue<T>> getAllowedValues() {
		return unwrapWrapperModel().getAllowedValues();
	}

	public LookupTableType getPredefinedValues() {
		return unwrapWrapperModel().getPredefinedValues();
	}
	
	public boolean hasValueEnumarationRef() {
		return unwrapWrapperModel().getValueEnumerationRef() != null;
	}
}

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

import java.util.Collection;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.MutablePrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.DisplayableValue;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LookupTableType;

/**
 * @author katka
 *
 */
public class PrismPropertyWrapperImpl<T> extends ItemWrapperImpl<PrismPropertyValue<T>, PrismProperty<T>, PrismPropertyDefinition<T>, PrismPropertyValueWrapper<T>> implements PrismPropertyWrapper<T> {

	private static final long serialVersionUID = 1L;

	private LookupTableType predefinedValues;

	public PrismPropertyWrapperImpl(PrismContainerValueWrapper<?> parent, PrismProperty<T> item, ItemStatus status) {
		super(parent, item, status);
	}

//	public PropertyDelta<T> getDelta() {
//		
//	}
	
	@Override
	public Collection<? extends DisplayableValue<T>> getAllowedValues() {
		return getItemDefinition().getAllowedValues();
	}

	@Override
	public T defaultValue() {
		return getItemDefinition().defaultValue();
	}

	@Override
	@Deprecated
	public QName getValueType() {
		return getItemDefinition().getValueType();
	}

	@Override
	public Boolean isIndexed() {
		return getItemDefinition().isIndexed();
	}

	@Override
	public QName getMatchingRuleQName() {
		return getItemDefinition().getMatchingRuleQName();
	}

	
	@Override
	public PropertyDelta<T> createEmptyDelta(ItemPath path) {
		return getItemDefinition().createEmptyDelta(path);
	}

	@Override
	public PrismPropertyDefinition<T> clone() {
		return getItemDefinition().clone();
	}

	@Override
	public MutablePrismPropertyDefinition<T> toMutable() {
		return getItemDefinition().toMutable();
	}

	@Override
	public PrismProperty<T> instantiate() {
		return getItemDefinition().instantiate();
	}
	
	@Override
	public PrismProperty<T> instantiate(QName name) {
		return getItemDefinition().instantiate(name);
	}

	@Override
	public LookupTableType getPredefinedValues() {
		return getPredefinedValues();
	}
}

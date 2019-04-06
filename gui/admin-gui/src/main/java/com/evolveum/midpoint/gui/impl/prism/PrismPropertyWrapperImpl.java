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

	/**
	 * @param parent
	 * @param item
	 * @param status
	 * @param fullPath
	 * @param prismContext
	 */
	public PrismPropertyWrapperImpl(PrismContainerValueWrapper<?> parent, PrismProperty<T> item, ItemStatus status) {
		super(parent, item, status);
		// TODO Auto-generated constructor stub
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.gui.api.prism.ItemWrapper#isStripe()
	 */
	@Override
	public boolean isStripe() {
		// TODO Auto-generated method stub
		return false;
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.prism.PrismPropertyDefinition#getAllowedValues()
	 */
	@Override
	public Collection<? extends DisplayableValue<T>> getAllowedValues() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.prism.PrismPropertyDefinition#defaultValue()
	 */
	@Override
	public T defaultValue() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.prism.PrismPropertyDefinition#getValueType()
	 */
	@Override
	public QName getValueType() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.prism.PrismPropertyDefinition#isIndexed()
	 */
	@Override
	public Boolean isIndexed() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.prism.PrismPropertyDefinition#getMatchingRuleQName()
	 */
	@Override
	public QName getMatchingRuleQName() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.prism.PrismPropertyDefinition#createEmptyDelta(com.evolveum.midpoint.prism.path.ItemPath)
	 */
	@Override
	public PropertyDelta<T> createEmptyDelta(ItemPath path) {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.prism.PrismPropertyDefinition#clone()
	 */
	@Override
	public PrismPropertyDefinition<T> clone() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.prism.PrismPropertyDefinition#toMutable()
	 */
	@Override
	public MutablePrismPropertyDefinition<T> toMutable() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.gui.impl.prism.ItemWrapperImpl#instantiate()
	 */
	@Override
	public PrismProperty<T> instantiate() {
		return null;
	}
	
	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.gui.impl.prism.ItemWrapperImpl#instantiate(javax.xml.namespace.QName)
	 */
	@Override
	public PrismProperty<T> instantiate(QName name) {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.gui.impl.prism.PrismPropertyWrapper#getPredefinedValues()
	 */
	@Override
	public LookupTableType getPredefinedValues() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.gui.api.prism.ItemWrapper#setReadOnly()
	 */
	@Override
	public void setReadOnly() {
		// TODO Auto-generated method stub
		
	}	

}

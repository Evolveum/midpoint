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

import java.io.Serializable;

import com.evolveum.midpoint.gui.api.prism.ItemWrapper;
import com.evolveum.midpoint.prism.PrismValue;

/**
 * @author katka
 *
 */
public class ValueWrapperImpl<T, V extends PrismValue> implements PrismValueWrapper<T> {

	private static final long serialVersionUID = 1L;
	
	private ItemWrapper<V, ?, ?> parent;
	
	private V oldValue;
	private V newValue;
	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.gui.impl.prism.PrismValueWrapper#getRealValue()
	 */
	@Override
	public T getRealValue() {
		return newValue.getRealValue();
	}
	

	
}

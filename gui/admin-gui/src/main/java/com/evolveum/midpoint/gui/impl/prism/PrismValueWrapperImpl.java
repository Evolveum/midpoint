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

import com.evolveum.midpoint.gui.api.prism.ItemWrapper;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.equivalence.EquivalenceStrategy;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.component.prism.ValueStatus;

/**
 * @author katka
 *
 */
public abstract class PrismValueWrapperImpl<T, V extends PrismValue> implements PrismValueWrapper<T, V> {

	private static final long serialVersionUID = 1L;
	
	private ItemWrapper<?,?,?,?> parent;
	
	private V oldValue;
	private V newValue;
	
	private ValueStatus status;
	
	
	PrismValueWrapperImpl(ItemWrapper<?, ?, ?, ?> parent, V value, ValueStatus status) {
		this.parent = parent;
		this.newValue = value;
		this.oldValue = (V) value.clone();
		this.status = status;
	}
	
	@Override
	public <D extends ItemDelta<V, ID>, ID extends ItemDefinition> void addToDelta(D delta) throws SchemaException {
		switch (status) {
			case ADDED:
				if (newValue.isEmpty()) {
					break;
				}
				if (parent.isSingleValue()) {
					delta.addValueToReplace((V) newValue.clone());
				} else {
					delta.addValueToAdd((V) newValue.clone());
				}
				break;
			case NOT_CHANGED:
				if (!isChanged()) {
					break;
				}
			case MODIFIED:				
				if (parent.isSingleValue()) {
					if (newValue.isEmpty())  {
						delta.addValueToReplace(null);
					} else {
						delta.addValueToReplace((V) newValue.clone());
					}
					break;
				}
				
				delta.addValueToAdd((V) newValue.clone());
				if (!oldValue.isEmpty()) {
					delta.addValueToDelete((V) oldValue.clone());
				}
				break;
			case DELETED:
				delta.addValueToDelete((V) oldValue.clone());
				break;
			default:
				break;
		}
		
//		parent.applyDelta((ItemDelta) delta);
	}
	
	
	private boolean isChanged() {
		return !oldValue.equals(newValue, EquivalenceStrategy.REAL_VALUE);
	}
	
	@Override
	public T getRealValue() {
		return newValue.getRealValue();
	}
	
	
	@Override
	public V getNewValue() {
		return newValue;
	}
	
	@Override
	public V getOldValue() {
		return oldValue;
	}
	
	@Override
	public <IW extends ItemWrapper> IW getParent() {
		return (IW) parent;
	}
	
	@Override
	public ValueStatus getStatus() {
		return status;
	}
	
	@Override
	public void setStatus(ValueStatus status) {
		this.status = status;
	}

	@Override
	public String debugDump(int indent) {
		StringBuilder sb = new StringBuilder();
		sb.append("Status: ").append(status).append("\n");
		sb.append("New value: ").append(newValue.debugDump()).append("\n");
		sb.append("Old value: ").append(oldValue.debugDump()).append("\n");
		return sb.toString();
	}
	
}

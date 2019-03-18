/**
 * Copyright (c) 2019 Evolveum
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
package com.evolveum.midpoint.schema.expression;

import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.util.ShortDumpable;

/**
 * Value and definition pair. E.g. used in expression variable maps.
 * We need to have explicit type here. It may happen that there will be
 * variables without any value. But we need to know the type of the
 * variable to compile the scripts properly.
 * 
 * @author Radovan Semancik
 */
public class TypedValue<T> implements ShortDumpable {
	
	/**
	 * Value may be null. This means variable without a value.
	 * But even in that case definition should be provided.
	 * The value is not T, it is Object. The value may not be in its
	 * final form yet. It may get converted later.
	 */
	private Object value;
	
	/**
	 * Definition should be filled in for all value that can be described using Prism definitions.
	 */
	private ItemDefinition<?> definition;
	
	/**
	 * Type class. Should be filled in for values that are not prism values.
	 */
	private Class<T> typeClass;
	
	public TypedValue() {
		super();
	}

	public TypedValue(Item<?, ?> prismItem) {
		super();
		this.value = (T) prismItem;
		this.definition = prismItem.getDefinition();
		if (definition == null) {
			throw new IllegalArgumentException("No definition when setting variable value to "+prismItem);
		}
	}
	
	public TypedValue(Object value, ItemDefinition<?> definition) {
		super();
		this.value = value;
		this.definition = definition;
	}
	
	public TypedValue(Object value, Class<T> typeClass) {
		super();
		this.value = value;
		this.typeClass = typeClass;
	}

	public Object getValue() {
		return value;
	}

	public void setValue(Object value) {
		this.value = value;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public <D extends ItemDefinition> D getDefinition() {
		return (D) definition;
	}

	public void setDefinition(ItemDefinition<?> definition) {
		this.definition = definition;
	}

	public Class<T> getTypeClass() {
		return typeClass;
	}

	public void setTypeClass(Class<T> typeClass) {
		this.typeClass = typeClass;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((definition == null) ? 0 : definition.hashCode());
		result = prime * result + ((typeClass == null) ? 0 : typeClass.hashCode());
		result = prime * result + ((value == null) ? 0 : value.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		TypedValue other = (TypedValue) obj;
		if (definition == null) {
			if (other.definition != null) {
				return false;
			}
		} else if (!definition.equals(other.definition)) {
			return false;
		}
		if (typeClass == null) {
			if (other.typeClass != null) {
				return false;
			}
		} else if (!typeClass.equals(other.typeClass)) {
			return false;
		}
		if (value == null) {
			if (other.value != null) {
				return false;
			}
		} else if (!value.equals(other.value)) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("TypedValue(");
		shortDump(sb);
		sb.append(")");
		return sb.toString();
	}

	@Override
	public void shortDump(StringBuilder sb) {
		sb.append(value);
		if (definition != null) {
			sb.append(", definition=");
			definition.debugDumpShortToString(sb);
		}
		if (typeClass != null) {
			sb.append(", class=").append(typeClass.getSimpleName());
		}
		if (definition == null && typeClass == null) {
			sb.append("definition/class=null");
		}
	}
	
	
}

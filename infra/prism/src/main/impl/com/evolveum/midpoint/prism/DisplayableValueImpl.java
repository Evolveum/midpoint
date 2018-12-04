/**
 * Copyright (c) 2013-2015 Evolveum
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
package com.evolveum.midpoint.prism;

import java.io.Serializable;

import com.evolveum.midpoint.util.DisplayableValue;

public class DisplayableValueImpl<T> implements DisplayableValue<T>, Serializable{

	private T value;
	private String label;
	private String description;

	public DisplayableValueImpl(T value, String label, String description) {
		this.label = label;
		this.value = value;
		this.description = description;
	}

	@Override
	public T getValue() {
		return value;
	}

	@Override
	public String getLabel() {
		return label;
	}

	@Override
	public String getDescription() {
		return description;
	}

	@Override
	public String toString() {
		return "DisplayableValueImpl(" + value + ": " + label + " (" + description
				+ "))";
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		DisplayableValueImpl<?> that = (DisplayableValueImpl<?>) o;

		if (value != null ? !value.equals(that.value) : that.value != null) return false;
		if (label != null ? !label.equals(that.label) : that.label != null) return false;
		return !(description != null ? !description.equals(that.description) : that.description != null);

	}

	@Override
	public int hashCode() {
		int result = value != null ? value.hashCode() : 0;
		result = 31 * result + (label != null ? label.hashCode() : 0);
		result = 31 * result + (description != null ? description.hashCode() : 0);
		return result;
	}
}

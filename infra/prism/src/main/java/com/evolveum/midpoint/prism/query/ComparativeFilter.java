/*
 * Copyright (c) 2010-2014 Evolveum
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

package com.evolveum.midpoint.prism.query;

import java.util.List;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.path.ItemPath;

public abstract class ComparativeFilter<T extends Object> extends PropertyValueFilter<PrismPropertyValue<T>> {

	private boolean equals;
	
	ComparativeFilter(ItemPath path, PrismPropertyDefinition definition, PrismPropertyValue<T> value, boolean equals) {
		super(path, definition, value);
		this.equals = equals;
	}

	public <T> ComparativeFilter(ItemPath path, PrismPropertyDefinition<T> definition, ItemPath rightSidePath, ItemDefinition rightSideDefinition, boolean equals) {
		super(path, definition, null, rightSidePath, rightSideDefinition);
		this.equals = equals;
	}

	public boolean isEquals() {
		return equals;
	}

	public void setEquals(boolean equals) {
		this.equals = equals;
	}
	
	static <T> PrismPropertyValue<T> createPropertyValue(PrismPropertyDefinition itemDefinition, T realValue){
		List<PrismPropertyValue<T>> values = realValueToPropertyList(itemDefinition, realValue);
		if (values == null || values.isEmpty()){
			return null;
		}
		
		if (values.size() > 1 ){
			throw new UnsupportedOperationException("Greater filter with more than one value is not supported");
		}
		
		return values.iterator().next();
		
	}

	@Override
	public boolean equals(Object o, boolean exact) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		if (!super.equals(o, exact))
			return false;

		ComparativeFilter<?> that = (ComparativeFilter<?>) o;

		return equals == that.equals;

	}

	@Override
	public int hashCode() {
		int result = super.hashCode();
		result = 31 * result + (equals ? 1 : 0);
		return result;
	}
}

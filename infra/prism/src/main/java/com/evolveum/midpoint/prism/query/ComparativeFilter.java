/*
 * Copyright (c) 2010-2013 Evolveum
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

import javax.xml.namespace.QName;

import org.w3c.dom.Element;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.exception.SchemaException;

public abstract class ComparativeFilter<T extends Object> extends PropertyValueFilter<PrismPropertyValue<T>>{

	private boolean equals;
	
	public ComparativeFilter() {
		// TODO Auto-generated constructor stub
	}
	
	ComparativeFilter(ItemPath path, PrismPropertyDefinition definition, PrismPropertyValue<T> value, boolean equals) {
		super(path, definition, value);
		this.equals = equals;
	}

	public boolean isEquals() {
		return equals;
	}

	public void setEquals(boolean equals) {
		this.equals = equals;
	}
	
	static <T> PrismPropertyValue<T> createPropertyValue(PrismPropertyDefinition itemDefinition, T realValue){
		List<PrismPropertyValue<T>> values = createPropertyList(itemDefinition, realValue);
		if (values == null || values.isEmpty()){
			return null;
		}
		
		if (values.size() > 1 ){
			throw new UnsupportedOperationException("Greater filter with more than one value is not supported");
		}
		
		return values.iterator().next();
		
	}
	
//	public static <F extends PropertyValueFilter, T> ComparativeFilter createComparativeFilter(Class<F> filterClass, ItemPath parentPath, PrismPropertyDefinition item, T realValue, boolean equals) throws SchemaException {
//		ComparativeFilter comparativeFilter = (ComparativeFilter) createPropertyFilter(filterClass, parentPath, item, null, realValue);
//		comparativeFilter.setEquals(equals);
//		return comparativeFilter;
//	}
//	
//	public static <F extends PropertyValueFilter, O extends Objectable, T> ComparativeFilter createComparativeFilter(Class<F> filterClass, ItemPath parentPath, PrismObjectDefinition<O> containerDef,
//			PrismPropertyValue<T> value, boolean equals) throws SchemaException {
//		ComparativeFilter comparativeFilter = (ComparativeFilter) createPropertyFilter(filterClass, parentPath, containerDef, value);
//		comparativeFilter.setEquals(equals);
//		return comparativeFilter;
//	}
//	
//	public static <F extends PropertyValueFilter, O extends Objectable, T> ComparativeFilter createComparativeFilter(Class<F> filterClass, ItemPath parentPath, PrismObjectDefinition<O> containerDef,
//			T realValue, boolean equals) throws SchemaException {
//		ComparativeFilter comparativeFilter = (ComparativeFilter) createPropertyFilter(filterClass, parentPath, containerDef, realValue);
//		comparativeFilter.setEquals(equals);
//		return comparativeFilter;
//	}
//
//	public static <F extends PropertyValueFilter, O extends Objectable, T> ComparativeFilter createComparativeFilter(Class<F> filterClass, Class<O> type, PrismContext prismContext, QName propertyName, T realValue, boolean equals)
//			throws SchemaException {
//		ComparativeFilter comparativeFilter = (ComparativeFilter) createPropertyFilter(filterClass, type, prismContext, propertyName, realValue);
//		comparativeFilter.setEquals(equals);
//		return comparativeFilter;
//	}
}

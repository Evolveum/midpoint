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

import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;

import org.w3c.dom.Element;

import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.SchemaException;

public class RefFilter extends PropertyValueFilter{
	
	RefFilter(ItemPath path, ItemDefinition definition, String matchingRule, List<PrismReferenceValue> values) {
		super(path, definition, matchingRule, values);
	}
		
	RefFilter(ItemPath path, ItemDefinition definition, PrismReferenceValue value) {
		super(path, definition, value);
	}
	
	RefFilter(ItemPath path, ItemDefinition definition, Element expression) {
		super(path, definition, expression);
	}
	
	public static RefFilter createReferenceEqual(ItemPath path, ItemDefinition definition, List<PrismReferenceValue> values){
		return new RefFilter(path, definition, null, values);
	}
	
	public static RefFilter createReferenceEqual(ItemPath path, ItemDefinition definition, Element expression){
		return new RefFilter(path, definition, expression);
	}
	
	public static RefFilter createReferenceEqual(ItemPath path, ItemDefinition definition, PrismReferenceValue value){
		return new RefFilter(path, definition, value);
	}
	
	public static RefFilter createReferenceEqual(ItemPath path, ItemDefinition item, String oid) {
		PrismReferenceValue value = new PrismReferenceValue(oid);
		return createReferenceEqual(path, item, value);
	}

	public static RefFilter createReferenceEqual(Class type, QName propertyName, PrismContext prismContext,
			String oid) throws SchemaException {
		PrismObjectDefinition objDef = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(type);
		return createReferenceEqual(null, objDef, propertyName, oid);

	}

	public static RefFilter createReferenceEqual(ItemPath path, PrismContainerDefinition containerDef,
			QName propertyName, String realValue) throws SchemaException {
		ItemDefinition itemDef = containerDef.findItemDefinition(propertyName);
		if (itemDef == null) {
			throw new SchemaException("No definition for item " + propertyName + " in container definition "
					+ containerDef);
		}
		return createReferenceEqual(path, itemDef, realValue);
	}
	
	public static RefFilter createReferenceEqual(Class type, QName propertyName, PrismObject<? extends Objectable> targetObject) throws SchemaException {
		PrismObjectDefinition objDef = targetObject.getPrismContext().getSchemaRegistry().findObjectDefinitionByCompileTimeClass(type);
		return createReferenceEqual(null, objDef, propertyName, targetObject.getOid());

	}

	@Override
	public RefFilter clone() {
		RefFilter clone = new RefFilter(getParentPath(), getDefinition(), getMatchingRule(), (List<PrismReferenceValue>) getValues());
		cloneValues(clone);
		return clone;
	}

	@Override
	public String dump() {
		return debugDump(0);
	}

	@Override
	public String debugDump() {
		return debugDump(0);
	}

	@Override
	public String debugDump(int indent) {
		StringBuilder sb = new StringBuilder();
		DebugUtil.indentDebugDump(sb, indent);
		sb.append("REF:");
		
		if (getParentPath() != null){
			sb.append("\n");
			DebugUtil.indentDebugDump(sb, indent+1);
			sb.append("PATH: ");
			sb.append(getParentPath().toString());
		} 
		sb.append("\n");
		DebugUtil.indentDebugDump(sb, indent+1);
		sb.append("DEF: ");
		if (getDefinition() != null) {
			sb.append(getDefinition().debugDump(0));
		} else {
			DebugUtil.indentDebugDump(sb, indent);
			sb.append("null");
		}
		sb.append("\n");
		DebugUtil.indentDebugDump(sb, indent+1);
		sb.append("VALUE: ");
		if (getValues() != null) {
			for (PrismValue val : getValues()) {
				sb.append("\n");
				sb.append(val.debugDump(indent + 2));
			}
		} else {
			DebugUtil.indentDebugDump(sb, indent);
			sb.append("null");
		}
		return sb.toString();

	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("REF: ");
		if (getParentPath() != null){
			sb.append(getParentPath().toString());
			sb.append(", ");
		}
		if (getDefinition() != null){
			sb.append(getDefinition().getName().getLocalPart());
			sb.append(", ");
		}
		if (getValues() != null){
			for (int i = 0; i< getValues().size(); i++){
				sb.append(getValues().get(i).toString());
				if ( i != getValues().size() -1){
					sb.append(", ");
				}
			}
		}
		return sb.toString();
	}

	@Override
	public <T extends Objectable> boolean match(PrismObject<T> object) {
//		ItemPath path = null;
//		if (getParentPath() != null) {
//			path = new ItemPath(getParentPath(), getDefinition().getName());
//		} else {
//			path = new ItemPath(getDefinition().getName());
//		}
//
//		Item<?> item = object.findItem(path);
//
//		if (!(item.getValue(0) instanceof PrismReferenceValue)) {
//			throw new IllegalStateException(
//					"Could not match object for ref filter. Values are not a prism reference values");
//		}
//
//		Item filterItem = getDefinition().instantiate();
//		try {
//			filterItem.addAll(getValues());
//		} catch (SchemaException e) {
//			throw new IllegalArgumentException(e.getMessage(), e);
//		}
//
//		return item.equals(filterItem);
		return super.match(object);
	}


}

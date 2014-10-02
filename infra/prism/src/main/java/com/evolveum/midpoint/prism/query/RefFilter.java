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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.xml.namespace.QName;

import org.apache.commons.lang.Validate;
import org.w3c.dom.Element;

import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.PrismReferenceDefinition;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.match.MatchingRuleRegistry;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.xnode.MapXNode;
import com.evolveum.midpoint.prism.xnode.XNode;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.SchemaException;

public class RefFilter extends PropertyValueFilter<PrismReferenceValue> {
	private static final long serialVersionUID = 1L;

	RefFilter(ItemPath path, PrismReferenceDefinition definition, ExpressionWrapper expression, List<PrismReferenceValue> values) {
		super(path, definition, expression, values);
	}
		
	RefFilter(ItemPath path, PrismReferenceDefinition definition, ExpressionWrapper expression) {
		super(path, definition, expression);
	}
	
	
	public static RefFilter createReferenceEqual(ItemPath path, PrismReference item){
		return new RefFilter(path, item.getDefinition(), null, item.getValues());
	}
		
	public static RefFilter createReferenceEqual(ItemPath path, PrismReferenceDefinition definition, PrismReferenceValue... values){
		if (values == null){
			createNullRefFilter(path, definition);
		}		
		return new RefFilter(path, definition, null, Arrays.asList(values));
	}
	
	public static RefFilter createReferenceEqual(ItemPath path, PrismReference item, ExpressionWrapper expression){
		return new RefFilter(path, item.getDefinition(), expression, item.getValues());
	}
	
	public static RefFilter createReferenceEqual(ItemPath path, PrismReferenceDefinition definition, ExpressionWrapper expression){
		return new RefFilter(path, definition, expression);
	}
		
	public static RefFilter createReferenceEqual(ItemPath path, PrismReferenceDefinition referenceDefinition, String... oids) {
		Validate.notNull(referenceDefinition, "Reference definition must not be null.");
		Validate.notNull(path, "Path must not be null.");
		if (oids == null){
			createNullRefFilter(path, referenceDefinition);
		}
		
		List<PrismReferenceValue> refValues = new ArrayList<PrismReferenceValue>(oids.length);
		for (String oid : oids){
			refValues.add(new PrismReferenceValue(oid));
		}
		
	
		return new RefFilter(path, referenceDefinition, null, refValues);
	}

	public static <O extends Objectable> RefFilter createReferenceEqual(QName propertyName, Class<O> type, PrismContext prismContext,
			String... oids) throws SchemaException {
		ItemPath path = new ItemPath(propertyName);
		PrismReferenceDefinition refDefinition = (PrismReferenceDefinition) findItemDefinition(path, type, prismContext);
		return createReferenceEqual(path, refDefinition, oids);

	}

	public static RefFilter createReferenceEqual(ItemPath path, PrismContainerDefinition containerDef, String... oids) {
		ItemDefinition itemDef = findItemDefinition(path, containerDef);
		
		if (!(itemDef instanceof PrismReferenceDefinition)){
			throw new IllegalStateException("Bad item definition. Expected that the definition will be instance of prism refenrence definition, but found " + itemDef);					
		}
		
		return createReferenceEqual(path, (PrismReferenceDefinition) itemDef, oids);
	}
	
	public  static <O extends Objectable> RefFilter createReferenceEqual(QName propertyName, Class type, O targetObject) {
		return createReferenceEqual(propertyName, type, targetObject.asPrismObject());
	}
	
	public static <O extends Objectable> RefFilter createReferenceEqual(QName propertyName, Class type, PrismObject<O> targetObject) {
		Validate.notNull(targetObject, "Target object must not be null");

		ItemPath path = new ItemPath(propertyName);
		
		ItemDefinition itemDef = findItemDefinition(path, type, targetObject.getPrismContext());
		
		if (!(itemDef instanceof PrismReferenceDefinition)){
			throw new IllegalStateException("Bad item definition. Expected that the definition will be instance of prism refenrence definition, but found " + itemDef);					
		}
		
		return createReferenceEqual(new ItemPath(propertyName), (PrismReferenceDefinition) itemDef, targetObject.getOid());

	}
	
	private static RefFilter createNullRefFilter(ItemPath path, PrismReferenceDefinition refDef){
		return new RefFilter(path, refDef, null, null);
	}

	@Override
	public RefFilter clone() {
		RefFilter clone = new RefFilter(getFullPath(), (PrismReferenceDefinition) getDefinition(), getExpression(), (List<PrismReferenceValue>) getValues());
		cloneValues(clone);
		return clone;
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
		
		return debugDump(indent, sb);
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("REF: ");
		return toString(sb);
	}

	@Override
	public <T extends Objectable> boolean match(PrismObject<T> object, MatchingRuleRegistry matchingRuleRegistry) throws SchemaException{
		Item item = getObjectItem(object);
		Item filterItem = getFilterItem();		
		return item.match(filterItem);
	}

	@Override
	public QName getElementName() {
		return getDefinition().getName();
	}

	@Override
	public PrismContext getPrismContext() {
		return getDefinition().getPrismContext();
	}

	@Override
	public ItemPath getPath() {
		return getFullPath();
	}
	
	@Override
	public PrismReferenceDefinition getDefinition() {
		// TODO Auto-generated method stub
		return (PrismReferenceDefinition) super.getDefinition();
	}


}

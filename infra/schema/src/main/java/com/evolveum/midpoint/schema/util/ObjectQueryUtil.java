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

package com.evolveum.midpoint.schema.util;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.match.PolyStringOrigMatchingRule;
import org.apache.commons.lang.Validate;
import org.apache.commons.lang.mutable.MutableBoolean;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.AndFilter;
import com.evolveum.midpoint.prism.query.EqualsFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.OrgFilter;
import com.evolveum.midpoint.prism.query.RefFilter;
import com.evolveum.midpoint.prism.query.ValueFilter;
import com.evolveum.midpoint.prism.query.Visitor;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ShadowType;
import com.evolveum.prism.xml.ns._public.query_2.QueryType;
import com.evolveum.prism.xml.ns._public.types_2.PolyStringType;

public class ObjectQueryUtil {

	public static ObjectQuery createNameQuery(String name, PrismContext prismContext) throws SchemaException {
    	PolyString polyName = new PolyString(name);
    	return createNameQuery(polyName, prismContext);
    }
    
    public static ObjectQuery createNameQuery(PolyStringType name, PrismContext prismContext) throws SchemaException {
    	return createNameQuery(name.toPolyString(), prismContext);
    }

    public static ObjectQuery createOrigNameQuery(PolyStringType name, PrismContext prismContext) throws SchemaException {
        return createOrigNameQuery(name.toPolyString(), prismContext);
    }

    public static ObjectQuery createNameQuery(PolyString name, PrismContext prismContext) throws SchemaException {
        EqualsFilter filter = EqualsFilter.createEqual(ObjectType.F_NAME, ObjectType.class, prismContext, null, name);
        return ObjectQuery.createObjectQuery(filter);
	}

    public static ObjectQuery createOrigNameQuery(PolyString name, PrismContext prismContext) throws SchemaException {
        EqualsFilter filter = EqualsFilter.createEqual(ObjectType.F_NAME, ObjectType.class, prismContext, PolyStringOrigMatchingRule.NAME, name);
        return ObjectQuery.createObjectQuery(filter);
    }

    public static ObjectQuery createNameQuery(ObjectType object) throws SchemaException {
		return createNameQuery(object.getName(), object.asPrismObject().getPrismContext());
	}
	
	public static <O extends ObjectType> ObjectQuery createNameQuery(PrismObject<O> object) throws SchemaException {
		return createNameQuery(object.asObjectable().getName(), object.getPrismContext());
	}
	
	public static ObjectQuery createResourceAndAccountQuery(String resourceOid, QName objectClass, PrismContext prismContext) throws SchemaException {
		Validate.notNull(resourceOid, "Resource where to search must not be null.");
		Validate.notNull(objectClass, "Object class to search must not be null.");
		Validate.notNull(prismContext, "Prism context must not be null.");
		AndFilter and = AndFilter.createAnd(
				createResourceFilter(resourceOid, prismContext), 
				createObjectClassFilter(objectClass, prismContext));
		return ObjectQuery.createObjectQuery(and);
	}
	
	public static ObjectFilter createResourceFilter(String resourceOid, PrismContext prismContext) throws SchemaException {
		return RefFilter.createReferenceEqual(ShadowType.F_RESOURCE_REF, ShadowType.class, prismContext, resourceOid);
	}
	
	public static ObjectFilter createObjectClassFilter(QName objectClass, PrismContext prismContext) {
		return EqualsFilter.createEqual(ShadowType.F_OBJECT_CLASS, ShadowType.class, prismContext, null, objectClass);
	}
	
	public static <T extends ObjectType> ObjectQuery createNameQuery(Class<T> clazz, PrismContext prismContext, String name) throws SchemaException{
		PolyString namePolyString = new PolyString(name);
		EqualsFilter equal = EqualsFilter.createEqual(ObjectType.F_NAME, clazz, prismContext, null, namePolyString);
		return ObjectQuery.createObjectQuery(equal);
	}
	
	public static ObjectQuery createRootOrgQuery(PrismContext prismContext) throws SchemaException {
		ObjectQuery objectQuery = ObjectQuery.createObjectQuery(OrgFilter.createRootOrg());
		return objectQuery;
	}
	
	public static boolean hasAllDefinitions(ObjectQuery query) {
		return hasAllDefinitions(query.getFilter());
	}

	
	public static boolean hasAllDefinitions(ObjectFilter filter) {
		final MutableBoolean hasAllDefinitions = new MutableBoolean(true);
		Visitor visitor = new Visitor() {
			@Override
			public void visit(ObjectFilter filter) {
				if (filter instanceof ValueFilter) {
					ItemDefinition definition = ((ValueFilter<?>)filter).getDefinition();
					if (definition == null) {
						hasAllDefinitions.setValue(false);
					}
				}
			}
		};
		filter.accept(visitor);
		return hasAllDefinitions.booleanValue();
	}

	public static String dump(QueryType query) {
		if (query == null) {
			return "null";
		}
		StringBuilder sb = new StringBuilder("Query(");
		sb.append(query.getDescription()).append("):\n");
		if (query.getFilter() != null)
			sb.append(DOMUtil.serializeDOMToString(query.getFilter()));
		else
			sb.append("(no filter)");
		return sb.toString();
	}
}

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

package com.evolveum.midpoint.schema.util;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.match.PolyStringOrigMatchingRule;

import org.apache.commons.lang.Validate;
import org.apache.commons.lang.mutable.MutableBoolean;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.AllFilter;
import com.evolveum.midpoint.prism.query.AndFilter;
import com.evolveum.midpoint.prism.query.EqualFilter;
import com.evolveum.midpoint.prism.query.InOidFilter;
import com.evolveum.midpoint.prism.query.NoneFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.OrFilter;
import com.evolveum.midpoint.prism.query.OrgFilter;
import com.evolveum.midpoint.prism.query.RefFilter;
import com.evolveum.midpoint.prism.query.ValueFilter;
import com.evolveum.midpoint.prism.query.Visitor;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.prism.xml.ns._public.query_3.QueryType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

public class ObjectQueryUtil {

	public static ObjectQuery createNameQuery(String name, PrismContext prismContext) throws SchemaException {
    	PolyString polyName = new PolyString(name);
    	return createNameQuery(polyName, prismContext);
    }

    public static ObjectQuery createOrigNameQuery(String name, PrismContext prismContext) throws SchemaException {
        PolyString polyName = new PolyString(name);
        return createOrigNameQuery(polyName, prismContext);
    }

    public static ObjectQuery createNameQuery(PolyStringType name, PrismContext prismContext) throws SchemaException {
    	return createNameQuery(name.toPolyString(), prismContext);
    }

    public static ObjectQuery createOrigNameQuery(PolyStringType name, PrismContext prismContext) throws SchemaException {
        return createOrigNameQuery(name.toPolyString(), prismContext);
    }

    public static ObjectQuery createNameQuery(PolyString name, PrismContext prismContext) throws SchemaException {
        EqualFilter filter = EqualFilter.createEqual(ObjectType.F_NAME, ObjectType.class, prismContext, null, name);
        return ObjectQuery.createObjectQuery(filter);
	}

    public static ObjectQuery createOrigNameQuery(PolyString name, PrismContext prismContext) throws SchemaException {
        EqualFilter filter = EqualFilter.createEqual(ObjectType.F_NAME, ObjectType.class, prismContext, PolyStringOrigMatchingRule.NAME, name);
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
		return EqualFilter.createEqual(ShadowType.F_OBJECT_CLASS, ShadowType.class, prismContext, null, objectClass);
	}
	
	public static <T extends ObjectType> ObjectQuery createNameQuery(Class<T> clazz, PrismContext prismContext, String name) throws SchemaException{
		PolyString namePolyString = new PolyString(name);
		EqualFilter equal = EqualFilter.createEqual(ObjectType.F_NAME, clazz, prismContext, null, namePolyString);
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
	
	public static void assertPropertyOnly(ObjectFilter filter, final String message) {
		Visitor visitor = new Visitor() {
			@Override
			public void visit(ObjectFilter filter) {
				if (filter instanceof OrgFilter) {
					if (message == null) {
						throw new IllegalArgumentException(filter.toString());
					} else {
						throw new IllegalArgumentException(message+": "+filter);
					}
				}
			}
		};
		filter.accept(visitor);
	}
	
	public static void assertNotRaw(ObjectFilter filter, final String message) {
		Visitor visitor = new Visitor() {
			@Override
			public void visit(ObjectFilter filter) {
				if (filter instanceof ValueFilter && ((ValueFilter)filter).isRaw()) {
					if (message == null) {
						throw new IllegalArgumentException(filter.toString());
					} else {
						throw new IllegalArgumentException(message+": "+filter);
					}
				}
			}
		};
		filter.accept(visitor);
	}

	public static String dump(QueryType query) throws SchemaException {
		if (query == null) {
			return "null";
		}
		StringBuilder sb = new StringBuilder("Query(");
		sb.append(query.getDescription()).append("):\n");
		if (query.getFilter() != null && query.getFilter().containsFilterClause())
			sb.append(DOMUtil.serializeDOMToString(query.getFilter().getFilterClauseAsElement()));
		else
			sb.append("(no filter)");
		return sb.toString();
	}

	/**
	 * Merges the two provided arguments into one AND filter in the most efficient way. 
	 */
	public static ObjectFilter filterAnd(ObjectFilter origFilter, ObjectFilter additionalFilter) {
		if (origFilter == additionalFilter) {
			// AND with itself
			return origFilter;
		}
		if (origFilter == null) {
			return additionalFilter;
		}
		if (additionalFilter == null) {
			return origFilter;
		}
		if (origFilter instanceof NoneFilter) {
			return origFilter;
		}
		if (additionalFilter instanceof NoneFilter) {
			return additionalFilter;
		}
		if (origFilter instanceof AllFilter) {
			return additionalFilter;
		}
		if (additionalFilter instanceof AllFilter) {
			return origFilter;
		}
		if (origFilter instanceof AndFilter) {
			if (!((AndFilter)origFilter).contains(additionalFilter)) {
				((AndFilter)origFilter).addCondition(additionalFilter);
			}
			return origFilter;
		}
		return AndFilter.createAnd(origFilter, additionalFilter);
	}
	
	/**
	 * Merges the two provided arguments into one OR filter in the most efficient way. 
	 */
	public static ObjectFilter filterOr(ObjectFilter origFilter, ObjectFilter additionalFilter) {
		if (origFilter == additionalFilter) {
			// OR with itself
			return origFilter;
		}
		if (origFilter == null) {
			return additionalFilter;
		}
		if (additionalFilter == null) {
			return origFilter;
		}
		if (origFilter instanceof AllFilter) {
			return origFilter;
		}
		if (additionalFilter instanceof AllFilter) {
			return additionalFilter;
		}
		if (origFilter instanceof NoneFilter) {
			return additionalFilter;
		}
		if (additionalFilter instanceof NoneFilter) {
			return origFilter;
		}
		if (origFilter instanceof OrFilter) {
			if (!((OrFilter)origFilter).contains(additionalFilter)) {
				((OrFilter)origFilter).addCondition(additionalFilter);
			}
			return origFilter;
		}
		return OrFilter.createOr(origFilter, additionalFilter);
	}

	public static boolean isAll(ObjectFilter filter) {
		return filter == null || filter instanceof AllFilter;
	}
	
	public static boolean isNone(ObjectFilter filter) {
		return filter != null && filter instanceof NoneFilter;
	}
}

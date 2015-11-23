/*
 * Copyright (c) 2010-2015 Evolveum
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

import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.match.PolyStringNormMatchingRule;
import com.evolveum.midpoint.prism.match.PolyStringOrigMatchingRule;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyStringNormalizer;
import com.evolveum.midpoint.prism.polystring.PrismDefaultPolyStringNormalizer;
import com.evolveum.midpoint.prism.query.NaryLogicalFilter;
import com.evolveum.midpoint.schema.ResourceShadowDiscriminator;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
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
import com.evolveum.midpoint.prism.query.LogicalFilter;
import com.evolveum.midpoint.prism.query.NoneFilter;
import com.evolveum.midpoint.prism.query.UndefinedFilter;
import com.evolveum.midpoint.prism.query.NotFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.OrFilter;
import com.evolveum.midpoint.prism.query.OrgFilter;
import com.evolveum.midpoint.prism.query.RefFilter;
import com.evolveum.midpoint.prism.query.TypeFilter;
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

    public static ObjectQuery createNormNameQuery(String name, PrismContext prismContext) throws SchemaException {
        PolyString polyName = new PolyString(name);
        return createNormNameQuery(polyName, prismContext);
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

    public static ObjectQuery createNormNameQuery(PolyString name, PrismContext prismContext) throws SchemaException {
        PolyStringNormalizer normalizer = new PrismDefaultPolyStringNormalizer();
        name.recompute(normalizer);

        EqualFilter filter = EqualFilter.createEqual(ObjectType.F_NAME, ObjectType.class, prismContext, PolyStringNormMatchingRule.NAME, name);
        return ObjectQuery.createObjectQuery(filter);
    }

    public static ObjectQuery createNameQuery(ObjectType object) throws SchemaException {
		return createNameQuery(object.getName(), object.asPrismObject().getPrismContext());
	}
	
	public static <O extends ObjectType> ObjectQuery createNameQuery(PrismObject<O> object) throws SchemaException {
		return createNameQuery(object.asObjectable().getName(), object.getPrismContext());
	}
	
	public static ObjectQuery createResourceAndObjectClassQuery(String resourceOid, QName objectClass, PrismContext prismContext) throws SchemaException {
		return ObjectQuery.createObjectQuery(createResourceAndObjectClassFilter(resourceOid, objectClass, prismContext));
	}
	
	public static ObjectFilter createResourceAndObjectClassFilter(String resourceOid, QName objectClass, PrismContext prismContext) throws SchemaException {
		Validate.notNull(resourceOid, "Resource where to search must not be null.");
		Validate.notNull(objectClass, "Object class to search must not be null.");
		Validate.notNull(prismContext, "Prism context must not be null.");
		AndFilter and = AndFilter.createAnd(
				createResourceFilter(resourceOid, prismContext), 
				createObjectClassFilter(objectClass, prismContext));
		return and;
	}

    public static ObjectQuery createResourceQuery(String resourceOid, PrismContext prismContext) throws SchemaException {
        Validate.notNull(resourceOid, "Resource where to search must not be null.");
        Validate.notNull(prismContext, "Prism context must not be null.");
        return ObjectQuery.createObjectQuery(createResourceFilter(resourceOid, prismContext));
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

	public static ObjectFilter simplify(ObjectFilter filter) {
		if (filter == null) {
			return null;
		}
		if (filter instanceof AndFilter) {
			List<ObjectFilter> conditions = ((AndFilter)filter).getConditions();
			AndFilter simplifiedFilter = ((AndFilter)filter).cloneEmpty();
			for (ObjectFilter subfilter: conditions) {
				if (subfilter instanceof NoneFilter) {
					// AND with "false"
					return NoneFilter.createNone();
				} else if (subfilter instanceof AllFilter || subfilter instanceof UndefinedFilter) {
					// AND with "true", just skip it
				} else {
					ObjectFilter simplifiedSubfilter = simplify(subfilter);
					simplifiedFilter.addCondition(simplifiedSubfilter);
				}
			}
			if (simplifiedFilter.isEmpty()) {
				return AllFilter.createAll();
			}
			
			if (simplifiedFilter.getConditions().size() == 1){
				return simplifiedFilter.getConditions().iterator().next();
			}
			
			return simplifiedFilter;
			
		} else if (filter instanceof OrFilter) {
			List<ObjectFilter> conditions = ((OrFilter)filter).getConditions();
			OrFilter simplifiedFilter = ((OrFilter)filter).cloneEmpty();
			for (ObjectFilter subfilter: conditions) {
				if (subfilter instanceof NoneFilter || subfilter instanceof UndefinedFilter) {
					// OR with "false", just skip it
				} else if (subfilter instanceof AllFilter) {
					// OR with "true"
					return AllFilter.createAll();
				} else {
					ObjectFilter simplifiedSubfilter = simplify(subfilter);
					simplifiedFilter.addCondition(simplifiedSubfilter);
				}
			}
			if (simplifiedFilter.isEmpty()) {
				return AllFilter.createAll();
			}
			
			if (simplifiedFilter.getConditions().size() == 1){
				return simplifiedFilter.getConditions().iterator().next();
			}
			
			return simplifiedFilter;
 
		} else if (filter instanceof NotFilter) {
			ObjectFilter subfilter = ((NotFilter)filter).getFilter();
			ObjectFilter simplifiedSubfilter = simplify(subfilter);
			if (subfilter instanceof UndefinedFilter){
				return null;
			} else if (subfilter instanceof NoneFilter) {
				return AllFilter.createAll();
			} else if (subfilter instanceof AllFilter) {
				return NoneFilter.createNone();
			} else {
				NotFilter simplifiedFilter = ((NotFilter)filter).cloneEmpty();
				simplifiedFilter.setFilter(simplifiedSubfilter);
				return simplifiedFilter;
			}
		} else if (filter instanceof TypeFilter) {
			ObjectFilter subFilter = ((TypeFilter) filter).getFilter();
			ObjectFilter simplifiedSubfilter = simplify(subFilter);
			TypeFilter simplifiedFilter = (TypeFilter) ((TypeFilter) filter).clone();
			simplifiedFilter.setFilter(simplifiedSubfilter);
			return simplifiedFilter;
		} else if (filter instanceof UndefinedFilter || filter instanceof AllFilter) {
			return null;
		} else {
			// Cannot simplify
			return filter.clone();
		}
 	}

	@SuppressWarnings("rawtypes")
	public static <T> T getValueFromFilter(List<? extends ObjectFilter> conditions, QName propertyName)
			throws SchemaException {
		ItemPath propertyPath = new ItemPath(propertyName);
		for (ObjectFilter f : conditions) {
			if (f instanceof EqualFilter && propertyPath.equivalent(((EqualFilter) f).getFullPath())) {
				List<? extends PrismValue> values = ((EqualFilter) f).getValues();
				if (values.size() > 1) {
					throw new SchemaException("More than one " + propertyName
							+ " defined in the search query.");
				}
				if (values.size() < 1) {
					throw new SchemaException("Search query does not have specified " + propertyName + ".");
				}

				return (T) ((PrismPropertyValue) values.get(0)).getValue();
			}
			if (NaryLogicalFilter.class.isAssignableFrom(f.getClass())) {
				T value = getValueFromFilter(((NaryLogicalFilter) f).getConditions(), propertyName);
				if (value != null) {
					return value;
				}
			}
		}

		return null;
	}

	@SuppressWarnings("unchecked")
	public static String getResourceOidFromFilter(List<? extends ObjectFilter> conditions)
			throws SchemaException {

		for (ObjectFilter f : conditions) {
			if (f instanceof RefFilter
					&& ShadowType.F_RESOURCE_REF.equals(((RefFilter) f).getDefinition().getName())) {
				List<PrismReferenceValue> values = (List<PrismReferenceValue>) ((RefFilter) f).getValues();
				if (values.size() > 1) {
					throw new SchemaException(
							"More than one resource references defined in the search query.");
				}
				if (values.size() < 1) {
					throw new SchemaException("Search query does not have specified resource reference.");
				}
				return values.get(0).getOid();
			}
			if (NaryLogicalFilter.class.isAssignableFrom(f.getClass())) {
				String resourceOid = getResourceOidFromFilter(((NaryLogicalFilter) f).getConditions());
				if (resourceOid != null) {
					return resourceOid;
				}
			}
		}
		return null;
	}

	public static ResourceShadowDiscriminator getCoordinates(ObjectFilter filter) throws SchemaException {
		String resourceOid = null;
        QName objectClass = null;
        ShadowKindType kind = null;
        String intent = null;

        if (filter instanceof AndFilter) {
            List<? extends ObjectFilter> conditions = ((AndFilter) filter).getConditions();
            resourceOid = getResourceOidFromFilter(conditions);
            objectClass = getValueFromFilter(conditions, ShadowType.F_OBJECT_CLASS);
            kind = getValueFromFilter(conditions, ShadowType.F_KIND);
			intent = getValueFromFilter(conditions, ShadowType.F_INTENT);
        }

        if (resourceOid == null) {
            throw new SchemaException("Resource not defined in a search query");
        }
        if (objectClass == null && kind == null) {
        	throw new SchemaException("Neither objectclass not kind is specified in a search query");
        }

        ResourceShadowDiscriminator coordinates = new ResourceShadowDiscriminator(resourceOid, kind, intent, false);
        coordinates.setObjectClass(objectClass);
        return coordinates;
	}
}

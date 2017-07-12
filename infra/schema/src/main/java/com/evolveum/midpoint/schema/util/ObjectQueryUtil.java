/*
 * Copyright (c) 2010-2017 Evolveum
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

import java.util.*;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.query.*;
import com.evolveum.midpoint.prism.query.Visitor;
import com.evolveum.midpoint.prism.query.builder.QueryBuilder;
import com.evolveum.midpoint.prism.query.builder.S_AtomicFilterExit;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.Validate;
import org.apache.commons.lang.mutable.MutableBoolean;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.polystring.PolyStringNormalizer;
import com.evolveum.midpoint.prism.polystring.PrismDefaultPolyStringNormalizer;
import com.evolveum.midpoint.schema.ResourceShadowDiscriminator;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.prism.xml.ns._public.query_3.QueryType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import org.jetbrains.annotations.NotNull;

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
		return QueryBuilder.queryFor(ObjectType.class, prismContext)
				.item(ObjectType.F_NAME).eq(name)
				.build();
	}

    public static ObjectQuery createOrigNameQuery(PolyString name, PrismContext prismContext) throws SchemaException {
		return QueryBuilder.queryFor(ObjectType.class, prismContext)
				.item(ObjectType.F_NAME).eq(name).matchingOrig()
				.build();
    }

    public static ObjectQuery createNormNameQuery(PolyString name, PrismContext prismContext) throws SchemaException {
        PolyStringNormalizer normalizer = new PrismDefaultPolyStringNormalizer();
        name.recompute(normalizer);
		return QueryBuilder.queryFor(ObjectType.class, prismContext)
				.item(ObjectType.F_NAME).eq(name).matchingNorm()
				.build();
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

	public static S_AtomicFilterExit createResourceAndObjectClassFilterPrefix(String resourceOid, QName objectClass, PrismContext prismContext) throws SchemaException {
		Validate.notNull(resourceOid, "Resource where to search must not be null.");
		Validate.notNull(objectClass, "Object class to search must not be null.");
		Validate.notNull(prismContext, "Prism context must not be null.");
		return QueryBuilder.queryFor(ShadowType.class, prismContext)
				.item(ShadowType.F_RESOURCE_REF).ref(resourceOid)
				.and().item(ShadowType.F_OBJECT_CLASS).eq(objectClass);
	}

	public static ObjectQuery createResourceAndKindIntent(String resourceOid, ShadowKindType kind, String intent, PrismContext prismContext) throws SchemaException {
		return ObjectQuery.createObjectQuery(createResourceAndKindIntentFilter(resourceOid, kind, intent, prismContext));
	}
	
	public static ObjectQuery createResourceAndKind(String resourceOid, ShadowKindType kind, PrismContext prismContext) throws SchemaException {
		return ObjectQuery.createObjectQuery(createResourceAndKindFilter(resourceOid, kind, prismContext));
	}
	
	public static ObjectFilter createResourceAndKindIntentFilter(String resourceOid, ShadowKindType kind, String intent, PrismContext prismContext) throws SchemaException {
		Validate.notNull(resourceOid, "Resource where to search must not be null.");
		Validate.notNull(kind, "Kind to search must not be null.");
		Validate.notNull(prismContext, "Prism context must not be null.");
		return QueryBuilder.queryFor(ShadowType.class, prismContext)
				.item(ShadowType.F_RESOURCE_REF).ref(resourceOid)
				.and().item(ShadowType.F_KIND).eq(kind)
				.and().item(ShadowType.F_INTENT).eq(intent)
				.buildFilter();
	}
	
	private static ObjectFilter createResourceAndKindFilter(String resourceOid, ShadowKindType kind, PrismContext prismContext) throws SchemaException {
		Validate.notNull(resourceOid, "Resource where to search must not be null.");
		Validate.notNull(kind, "Kind to search must not be null.");
		Validate.notNull(prismContext, "Prism context must not be null.");
		return QueryBuilder.queryFor(ShadowType.class, prismContext)
				.item(ShadowType.F_RESOURCE_REF).ref(resourceOid)
				.and().item(ShadowType.F_KIND).eq(kind)
				.buildFilter();
	}

    public static ObjectQuery createResourceQuery(String resourceOid, PrismContext prismContext) throws SchemaException {
        Validate.notNull(resourceOid, "Resource where to search must not be null.");
        Validate.notNull(prismContext, "Prism context must not be null.");
        return ObjectQuery.createObjectQuery(createResourceFilter(resourceOid, prismContext));
    } 

    public static ObjectFilter createResourceFilter(String resourceOid, PrismContext prismContext) throws SchemaException {
		return QueryBuilder.queryFor(ShadowType.class, prismContext)
				.item(ShadowType.F_RESOURCE_REF).ref(resourceOid)
				.buildFilter();
	}
	
	public static ObjectFilter createObjectClassFilter(QName objectClass, PrismContext prismContext) {
		return QueryBuilder.queryFor(ShadowType.class, prismContext)
				.item(ShadowType.F_OBJECT_CLASS).eq(objectClass)
				.buildFilter();
	}
	
	public static <T extends ObjectType> ObjectQuery createNameQuery(Class<T> clazz, PrismContext prismContext, String name) throws SchemaException {
		return QueryBuilder.queryFor(clazz, prismContext)
				.item(ObjectType.F_NAME).eqPoly(name)
				.build();
	}
	
	public static ObjectQuery createRootOrgQuery(PrismContext prismContext) throws SchemaException {
		return QueryBuilder.queryFor(ObjectType.class, prismContext).isRoot().build();
	}
	
	public static boolean hasAllDefinitions(ObjectQuery query) {
		return hasAllDefinitions(query.getFilter());
	}

	
	public static boolean hasAllDefinitions(ObjectFilter filter) {
		final MutableBoolean hasAllDefinitions = new MutableBoolean(true);
		Visitor visitor = f -> {
			if (f instanceof ValueFilter) {
				ItemDefinition definition = ((ValueFilter<?,?>) f).getDefinition();
				if (definition == null) {
					hasAllDefinitions.setValue(false);
				}
			}
		};
		filter.accept(visitor);
		return hasAllDefinitions.booleanValue();
	}

	// TODO what about OidIn here?
	public static void assertPropertyOnly(ObjectFilter filter, final String message) {
		Visitor visitor = f -> {
			if (f instanceof OrgFilter) {
				if (message == null) {
					throw new IllegalArgumentException(f.toString());
				} else {
					throw new IllegalArgumentException(message+": "+ f);
				}
			}
		};
		filter.accept(visitor);
	}
	
	public static void assertNotRaw(ObjectFilter filter, final String message) {
		Visitor visitor = f -> {
			if (f instanceof ValueFilter && ((ValueFilter) f).isRaw()) {
				if (message == null) {
					throw new IllegalArgumentException(f.toString());
				} else {
					throw new IllegalArgumentException(message+": "+ f);
				}
			}
		};
		filter.accept(visitor);
	}

	public static String dump(QueryType query, @NotNull PrismContext prismContext) throws SchemaException {
		if (query == null) {
			return "null";
		}
		StringBuilder sb = new StringBuilder("Query(");
		sb.append(query.getDescription()).append("):\n");
		if (query.getFilter() != null && query.getFilter().containsFilterClause())
			sb.append(DOMUtil.serializeDOMToString(query.getFilter().getFilterClauseAsElement(prismContext)));
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
		return filter instanceof NoneFilter;
	}

	// returns ALL, NONE only at the top level (never inside the filter)
	// never returns UNDEFINED
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
					if (simplifiedSubfilter instanceof NoneFilter) {
						return NoneFilter.createNone();
					} else if (simplifiedSubfilter == null || simplifiedSubfilter instanceof AllFilter) {
						// skip
					} else {
						simplifiedFilter.addCondition(simplifiedSubfilter);
					}
				}
			}
			if (simplifiedFilter.isEmpty()) {
				return AllFilter.createAll();
			} else if (simplifiedFilter.getConditions().size() == 1) {
				return simplifiedFilter.getConditions().iterator().next();
			} else {
				return simplifiedFilter;
			}
			
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
					if (simplifiedSubfilter instanceof NoneFilter) {
						// skip
					} else if (simplifiedSubfilter == null || simplifiedSubfilter instanceof AllFilter) {
						return NoneFilter.createNone();
					} else {
						simplifiedFilter.addCondition(simplifiedSubfilter);
					}
				}
			}
			if (simplifiedFilter.isEmpty()) {
				return NoneFilter.createNone();
			} else if (simplifiedFilter.getConditions().size() == 1) {
				return simplifiedFilter.getConditions().iterator().next();
			} else {
				return simplifiedFilter;
			}

		} else if (filter instanceof NotFilter) {
			ObjectFilter subfilter = ((NotFilter)filter).getFilter();
			ObjectFilter simplifiedSubfilter = simplify(subfilter);
			if (simplifiedSubfilter instanceof NoneFilter) {
				return AllFilter.createAll();
			} else if (simplifiedSubfilter == null || simplifiedSubfilter instanceof AllFilter) {
				return NoneFilter.createNone();
			} else {
				NotFilter simplifiedFilter = ((NotFilter)filter).cloneEmpty();
				simplifiedFilter.setFilter(simplifiedSubfilter);
				return simplifiedFilter;
			}
		} else if (filter instanceof TypeFilter) {
			ObjectFilter subFilter = ((TypeFilter) filter).getFilter();
			ObjectFilter simplifiedSubfilter = simplify(subFilter);
			if (simplifiedSubfilter instanceof AllFilter) {
				simplifiedSubfilter = null;
			} else if (simplifiedSubfilter instanceof NoneFilter) {
				return NoneFilter.createNone();
			}
			TypeFilter simplifiedFilter = ((TypeFilter) filter).cloneEmpty();
			simplifiedFilter.setFilter(simplifiedSubfilter);
			return simplifiedFilter;
		} else if (filter instanceof ExistsFilter) {
			ObjectFilter subFilter = ((ExistsFilter) filter).getFilter();
			ObjectFilter simplifiedSubfilter = simplify(subFilter);
			if (simplifiedSubfilter instanceof AllFilter) {
				simplifiedSubfilter = null;
			} else if (simplifiedSubfilter instanceof NoneFilter) {
				return NoneFilter.createNone();
			}
			ExistsFilter simplifiedFilter = ((ExistsFilter) filter).cloneEmpty();
			simplifiedFilter.setFilter(simplifiedSubfilter);
			return simplifiedFilter;
		} else if (filter instanceof UndefinedFilter || filter instanceof AllFilter) {
			return null;
		} else {
			// Cannot simplify
			return filter.clone();
		}
 	}

	public static PrismValue getValueFromQuery(ObjectQuery query, QName itemName) throws SchemaException {
		if (query != null) {
			return getValueFromFilter(query.getFilter(), itemName);
		} else {
			return null;
		}
	}

	public static <T extends PrismValue> Collection<T> getValuesFromQuery(ObjectQuery query, QName itemName) throws SchemaException {
		if (query != null) {
			return getValuesFromFilter(query.getFilter(), itemName);
		} else {
			return null;
		}
	}

	private static PrismValue getValueFromFilter(ObjectFilter filter, QName itemName) throws SchemaException {
		Collection<PrismValue> values = getValuesFromFilter(filter, itemName);
		if (values == null || values.size() == 0) {
			return null;
		} else if (values.size() > 1) {
			throw new SchemaException("More than one " + itemName + " defined in the search query.");
		} else {
			return values.iterator().next();
		}
	}

	private static <T extends PrismValue> Collection<T> getValuesFromFilter(ObjectFilter filter, QName itemName) throws SchemaException {
		ItemPath propertyPath = new ItemPath(itemName);
		if (filter instanceof EqualFilter && propertyPath.equivalent(((EqualFilter) filter).getFullPath())) {
			return ((EqualFilter) filter).getValues();
		} else if (filter instanceof RefFilter && propertyPath.equivalent(((RefFilter) filter).getFullPath())) {
			return (Collection<T>) ((RefFilter) filter).getValues();
		} else if (filter instanceof AndFilter) {
			return getValuesFromFilter(((NaryLogicalFilter) filter).getConditions(), itemName);
		} else if (filter instanceof TypeFilter) {
			return getValuesFromFilter(((TypeFilter) filter).getFilter(), itemName);
		} else {
			return null;
		}
	}

	private static <T extends PrismValue> Collection<T> getValuesFromFilter(List<? extends ObjectFilter> conditions, QName propertyName)
			throws SchemaException {
		for (ObjectFilter f : conditions) {
			Collection<T> values = getValuesFromFilter(f, propertyName);
			if (values != null) {
				return values;
			}
		}
		return null;
	}

	private static String getResourceOidFromFilter(ObjectFilter filter) throws SchemaException {
		PrismReferenceValue referenceValue = (PrismReferenceValue) getValueFromFilter(filter, ShadowType.F_RESOURCE_REF);
		return referenceValue != null ? referenceValue.getOid() : null;
	}

	private static <T> T getPropertyRealValueFromFilter(ObjectFilter filter, QName propertyName) throws SchemaException {
		PrismPropertyValue<T> propertyValue = (PrismPropertyValue<T>) getValueFromFilter(filter, propertyName);
		return propertyValue != null ? propertyValue.getValue() : null;
	}

	public static ResourceShadowDiscriminator getCoordinates(ObjectFilter filter) throws SchemaException {
		String resourceOid = getResourceOidFromFilter(filter);
        QName objectClass = getPropertyRealValueFromFilter(filter, ShadowType.F_OBJECT_CLASS);
        ShadowKindType kind = getPropertyRealValueFromFilter(filter, ShadowType.F_KIND);
        String intent = getPropertyRealValueFromFilter(filter, ShadowType.F_INTENT);

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

	public static FilterComponents factorOutQuery(ObjectQuery query, QName... names) {
		return factorOutQuery(query, ItemPath.asPathArray(names));
	}

	public static FilterComponents factorOutQuery(ObjectQuery query, ItemPath... paths) {
		return factorOutFilter(query != null ? query.getFilter() : null, paths);
	}

	public static FilterComponents factorOutFilter(ObjectFilter filter, ItemPath... paths) {
		FilterComponents components = new FilterComponents();
		factorOutFilter(components, simplify(filter), Arrays.asList(paths), true);
		return components;
	}

	// TODO better API
	public static FilterComponents factorOutOrFilter(ObjectFilter filter, ItemPath... paths) {
		FilterComponents components = new FilterComponents();
		factorOutFilter(components, simplify(filter), Arrays.asList(paths), false);
		return components;
	}

	private static void factorOutFilter(FilterComponents filterComponents, ObjectFilter filter, List<ItemPath> paths, boolean connectedByAnd) {
		if (filter instanceof EqualFilter) {
			EqualFilter equalFilter = (EqualFilter) filter;
			if (ItemPath.containsEquivalent(paths, equalFilter.getPath())) {
				filterComponents.addToKnown(equalFilter.getPath(), equalFilter.getValues());
			} else {
				filterComponents.addToRemainder(equalFilter);
			}
		} else if (filter instanceof RefFilter) {
			RefFilter refFilter = (RefFilter) filter;
			if (ItemPath.containsEquivalent(paths, refFilter.getPath())) {
				filterComponents.addToKnown(refFilter.getPath(), refFilter.getValues());
			} else {
				filterComponents.addToRemainder(refFilter);
			}
		} else if (connectedByAnd && filter instanceof AndFilter) {
			for (ObjectFilter condition : ((AndFilter) filter).getConditions()) {
				factorOutFilter(filterComponents, condition, paths, true);
			}
		} else if (!connectedByAnd && filter instanceof OrFilter) {
			for (ObjectFilter condition : ((OrFilter) filter).getConditions()) {
				factorOutFilter(filterComponents, condition, paths, false);
			}
		} else if (filter instanceof TypeFilter) {
			// this is a bit questionable...
			factorOutFilter(filterComponents, ((TypeFilter) filter).getFilter(), paths, connectedByAnd);
		} else if (filter != null) {
			filterComponents.addToRemainder(filter);
		} else {
			// nothing to do with a null filter
		}
	}

	public static class FilterComponents {
		private Map<ItemPath,Collection<? extends PrismValue>> knownComponents = new HashMap<>();
		private List<ObjectFilter> remainderClauses = new ArrayList<>();

		public Map<ItemPath, Collection<? extends PrismValue>> getKnownComponents() {
			return knownComponents;
		}

		public ObjectFilter getRemainder() {
			if (remainderClauses.size() == 0) {
				return null;
			} else if (remainderClauses.size() == 1) {
				return remainderClauses.get(0);
			} else {
				return AndFilter.createAnd(remainderClauses);
			}
		}

		public void addToKnown(ItemPath path, List values) {
			Map.Entry<ItemPath, Collection<? extends PrismValue>> entry = getKnownComponent(path);
			if (entry != null) {
				entry.setValue(CollectionUtils.intersection(entry.getValue(), values));
			} else {
				knownComponents.put(path, values);
			}
		}

		public Map.Entry<ItemPath, Collection<? extends PrismValue>> getKnownComponent(ItemPath path) {
			for (Map.Entry<ItemPath, Collection<? extends PrismValue>> entry : knownComponents.entrySet()) {
				if (path.equivalent(entry.getKey())) {
					return entry;
				}
			}
			return null;
		}

		public void addToRemainder(ObjectFilter filter) {
			remainderClauses.add(filter);
		}

		public boolean hasRemainder() {
			return !remainderClauses.isEmpty();
		}

		public List<ObjectFilter> getRemainderClauses() {
			return remainderClauses;
		}

	}
}

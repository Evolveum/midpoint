/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.util;

import static java.util.Collections.singleton;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.xml.namespace.QName;

import org.apache.commons.lang.Validate;
import org.apache.commons.lang.mutable.MutableBoolean;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.impl.polystring.AlphanumericPolyStringNormalizer;
import com.evolveum.midpoint.prism.impl.query.PagingConvertor;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.polystring.PolyStringNormalizer;
import com.evolveum.midpoint.prism.query.Visitor;
import com.evolveum.midpoint.prism.query.*;
import com.evolveum.midpoint.prism.query.builder.S_AtomicFilterExit;
import com.evolveum.midpoint.prism.xnode.RootXNode;
import com.evolveum.midpoint.schema.RelationRegistry;
import com.evolveum.midpoint.schema.ResourceShadowDiscriminator;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.query_3.PagingType;
import com.evolveum.prism.xml.ns._public.query_3.QueryType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

public class ObjectQueryUtil {

    public static ObjectQuery createNameQuery(String name, PrismContext prismContext) throws SchemaException {
        PolyString polyName = new PolyString(name);
        return createNameQuery(polyName, prismContext);
    }

    public static ObjectQuery createOrigNameQuery(String name, PrismContext prismContext) {
        PolyString polyName = new PolyString(name);
        return createOrigNameQuery(polyName, prismContext);
    }

    public static ObjectQuery createNameQuery(PolyStringType name, PrismContext prismContext) throws SchemaException {
        return createNameQuery(name.toPolyString(), prismContext);
    }

    public static ObjectQuery createNameQuery(PolyString name, PrismContext prismContext) throws SchemaException {
        return prismContext.queryFor(ObjectType.class)
                .item(ObjectType.F_NAME).eq(name)
                .build();
    }

    public static <O extends ObjectType> ObjectQuery createOidQuery(PrismObject<O> object) throws SchemaException {
        return createOidQuery(object.getOid(), object.getPrismContext());
    }

    public static ObjectQuery createOidQuery(String oid, PrismContext prismContext) {
        return prismContext.queryFor(ObjectType.class)
                .id(oid)
                .build();
    }

    public static ObjectQuery createOrigNameQuery(PolyString name, PrismContext prismContext) {
        return prismContext.queryFor(ObjectType.class)
                .item(ObjectType.F_NAME).eq(name).matchingOrig()
                .build();
    }

    public static ObjectQuery createNormNameQuery(PolyString name, PrismContext prismContext) {
        PolyStringNormalizer normalizer = new AlphanumericPolyStringNormalizer();
        name.recompute(normalizer);
        return prismContext.queryFor(ObjectType.class)
                .item(ObjectType.F_NAME).eq(name).matchingNorm()
                .build();
    }

    public static ObjectQuery createNameQuery(ObjectType object) throws SchemaException {
        return createNameQuery(object.getName(), object.asPrismObject().getPrismContext());
    }

    public static <O extends ObjectType> ObjectQuery createNameQuery(PrismObject<O> object) throws SchemaException {
        return createNameQuery(object.asObjectable().getName(), object.getPrismContext());
    }

    public static ObjectQuery createResourceAndObjectClassQuery(String resourceOid, QName objectClass, PrismContext prismContext) {
        return prismContext.queryFactory().createQuery(createResourceAndObjectClassFilter(resourceOid, objectClass, prismContext));
    }

    public static ObjectFilter createResourceAndObjectClassFilter(String resourceOid, QName objectClass, PrismContext prismContext) {
        Validate.notNull(resourceOid, "Resource where to search must not be null.");
        Validate.notNull(objectClass, "Object class to search must not be null.");
        Validate.notNull(prismContext, "Prism context must not be null.");
        return prismContext.queryFactory().createAnd(
                createResourceFilter(resourceOid, prismContext),
                createObjectClassFilter(objectClass, prismContext));
    }

    public static S_AtomicFilterExit createResourceAndObjectClassFilterPrefix(String resourceOid, QName objectClass, PrismContext prismContext) throws SchemaException {
        Validate.notNull(resourceOid, "Resource where to search must not be null.");
        Validate.notNull(objectClass, "Object class to search must not be null.");
        Validate.notNull(prismContext, "Prism context must not be null.");
        return prismContext.queryFor(ShadowType.class)
                .item(ShadowType.F_RESOURCE_REF).ref(resourceOid)
                .and().item(ShadowType.F_OBJECT_CLASS).eq(objectClass);
    }

    public static ObjectQuery createResourceAndKindIntent(String resourceOid, ShadowKindType kind, String intent, PrismContext prismContext) throws SchemaException {
        return prismContext.queryFactory().createQuery(createResourceAndKindIntentFilter(resourceOid, kind, intent, prismContext));
    }

    public static ObjectQuery createResourceAndKind(String resourceOid, ShadowKindType kind, PrismContext prismContext) throws SchemaException {
        return prismContext.queryFactory().createQuery(createResourceAndKindFilter(resourceOid, kind, prismContext));
    }

    public static ObjectFilter createResourceAndKindIntentFilter(String resourceOid, ShadowKindType kind, String intent, PrismContext prismContext) {
        Validate.notNull(resourceOid, "Resource where to search must not be null.");
        Validate.notNull(kind, "Kind to search must not be null.");
        Validate.notNull(prismContext, "Prism context must not be null.");
        return prismContext.queryFor(ShadowType.class)
                .item(ShadowType.F_RESOURCE_REF).ref(resourceOid)
                .and().item(ShadowType.F_KIND).eq(kind)
                .and().item(ShadowType.F_INTENT).eq(intent)
                .buildFilter();
    }

    private static ObjectFilter createResourceAndKindFilter(String resourceOid, ShadowKindType kind, PrismContext prismContext) {
        Validate.notNull(resourceOid, "Resource where to search must not be null.");
        Validate.notNull(kind, "Kind to search must not be null.");
        Validate.notNull(prismContext, "Prism context must not be null.");
        return prismContext.queryFor(ShadowType.class)
                .item(ShadowType.F_RESOURCE_REF).ref(resourceOid)
                .and().item(ShadowType.F_KIND).eq(kind)
                .buildFilter();
    }

    public static ObjectQuery createResourceQuery(String resourceOid, PrismContext prismContext) {
        Validate.notNull(resourceOid, "Resource where to search must not be null.");
        Validate.notNull(prismContext, "Prism context must not be null.");
        return prismContext.queryFactory().createQuery(createResourceFilter(resourceOid, prismContext));
    }

    public static ObjectFilter createResourceFilter(String resourceOid, PrismContext prismContext) {
        return prismContext.queryFor(ShadowType.class)
                .item(ShadowType.F_RESOURCE_REF).ref(resourceOid)
                .buildFilter();
    }

    public static ObjectFilter createObjectClassFilter(QName objectClass, PrismContext prismContext) {
        return prismContext.queryFor(ShadowType.class)
                .item(ShadowType.F_OBJECT_CLASS).eq(objectClass)
                .buildFilter();
    }

    public static <T extends ObjectType> ObjectQuery createNameQuery(Class<T> clazz, PrismContext prismContext, String name) throws SchemaException {
        return prismContext.queryFor(clazz)
                .item(ObjectType.F_NAME).eqPoly(name)
                .build();
    }

    public static ObjectQuery createRootOrgQuery(PrismContext prismContext) {
        return prismContext.queryFor(ObjectType.class).isRoot().build();
    }

    public static boolean hasAllDefinitions(ObjectQuery query) {
        return hasAllDefinitions(query.getFilter());
    }

    public static boolean hasAllDefinitions(ObjectFilter filter) {
        final MutableBoolean hasAllDefinitions = new MutableBoolean(true);
        if (filter == null) {
            return hasAllDefinitions.booleanValue();
        }
        Visitor visitor = f -> {
            if (f instanceof ValueFilter) {
                ItemDefinition<?> definition = ((ValueFilter<?, ?>) f).getDefinition();
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
                    throw new IllegalArgumentException(message + ": " + f);
                }
            }
        };
        filter.accept(visitor);
    }

    public static void assertNotRaw(ObjectFilter filter, final String message) {
        Visitor visitor = f -> {
            if (f instanceof ValueFilter && ((ValueFilter<?, ?>) f).isRaw()) {
                if (message == null) {
                    throw new IllegalArgumentException(f.toString());
                } else {
                    throw new IllegalArgumentException(message + ": " + f);
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
        if (query.getFilter() != null && query.getFilter().containsFilterClause()) {
            RootXNode clause = query.getFilter().getFilterClauseAsRootXNode();
            sb.append(prismContext.xmlSerializer().serialize(clause));
        } else {
            sb.append("(no filter)");
        }
        return sb.toString();
    }

    /**
     * Merges the two provided arguments into one AND filter in the most efficient way.
     * *Please note: If provided `origFilter` is {@link AndFilter} it will be modified!*
     * TODO consider moving to QueryFactory
     */
    public static ObjectFilter filterAnd(ObjectFilter origFilter, ObjectFilter additionalFilter,
            PrismContext prismContext) {
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
            if (!((AndFilter) origFilter).contains(additionalFilter)) {
                ((AndFilter) origFilter).addCondition(additionalFilter);
            }
            return origFilter;
        }
        return prismContext.queryFactory().createAnd(origFilter, additionalFilter);
    }

    /**
     * Merges the two provided arguments into one AND filter in an immutable way.
     * If provided `origFilter` is {@link AndFilter}, it will be cloned first, then modified.
     * Although input objects are not changed, there is no guarantee that new object is always returned.
     * For many simple and/or corner cases one of the parameters may be returned, so it may not
     * be safe to mutate the returned value.
     * TODO consider moving to QueryFactory
     */
    public static ObjectFilter filterAndImmutable(
            @Nullable ObjectFilter origFilter, @Nullable ObjectFilter additionalFilter) {
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
        // This branch could be skipped, but this is filter-optimizing and leaves multi-AND flat.
        if (origFilter instanceof AndFilter) {
            if (!((AndFilter) origFilter).contains(additionalFilter)) {
                AndFilter clonedOrigFilter = ((AndFilter) origFilter).clone();
                clonedOrigFilter.addCondition(additionalFilter);
                return clonedOrigFilter;
            }
            return origFilter;
        }
        return PrismContext.get().queryFactory().createAnd(origFilter, additionalFilter);
    }

    /**
     * Merges the two provided arguments into one OR filter in the most efficient way.
     * *Please note: If provided `origFilter` is {@link OrFilter} it will be modified!*
     * TODO consider moving to QueryFactory
     */
    public static ObjectFilter filterOr(ObjectFilter origFilter, ObjectFilter additionalFilter,
            PrismContext prismContext) {
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
            if (!((OrFilter) origFilter).contains(additionalFilter)) {
                ((OrFilter) origFilter).addCondition(additionalFilter);
            }
            return origFilter;
        }
        return prismContext.queryFactory().createOr(origFilter, additionalFilter);
    }

    /**
     * Merges the two provided arguments into one OR filter in an immutable way.
     * If provided `origFilter` is {@link OrFilter}, it will be cloned first, then modified.
     * Although input objects are not changed, there is no guarantee that new object is always returned.
     * For many simple and/or corner cases one of the parameters may be returned, so it may not
     * be safe to mutate the returned value.
     * TODO consider moving to QueryFactory
     */
    public static ObjectFilter filterOrImmutable(ObjectFilter origFilter, ObjectFilter additionalFilter) {
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
        // This branch could be skipped, but this is filter-optimizing and leaves multi-OR flat.
        if (origFilter instanceof OrFilter) {
            if (!((OrFilter) origFilter).contains(additionalFilter)) {
                OrFilter clonedOrigFilter = ((OrFilter) origFilter).clone();
                clonedOrigFilter.addCondition(additionalFilter);
                return clonedOrigFilter;
            }
            return origFilter;
        }
        return PrismContext.get().queryFactory().createOr(origFilter, additionalFilter);
    }

    public static boolean isAll(ObjectFilter filter) {
        return filter == null || filter instanceof AllFilter;
    }

    public static boolean isNone(ObjectFilter filter) {
        return filter instanceof NoneFilter;
    }

    /**
     * Returns ALL, NONE only at the top level (never inside the filter), never returns UNDEFINED.
     * This always returns cloned filter which can be freely modify later.
     */
    public static ObjectFilter simplify(ObjectFilter filter, PrismContext prismContext) {
        if (filter == null) {
            return null;
        }
        if (filter instanceof AndFilter) {
            List<ObjectFilter> conditions = ((AndFilter) filter).getConditions();
            AndFilter simplifiedFilter = ((AndFilter) filter).cloneEmpty();
            for (ObjectFilter subfilter : conditions) {
                if (subfilter instanceof NoneFilter) {
                    // AND with "false"
                    return FilterCreationUtil.createNone(prismContext);
                } else if (subfilter instanceof AllFilter || subfilter instanceof UndefinedFilter) {
                    // AND with "true", just skip it
                } else {
                    ObjectFilter simplifiedSubfilter = simplify(subfilter, prismContext);
                    if (simplifiedSubfilter instanceof AndFilter) {
                        // Unwrap AND filter to parent and
                        for (ObjectFilter condition : ((AndFilter) simplifiedSubfilter).getConditions()) {
                            simplifiedFilter.addCondition(condition);
                        }
                    } else if (simplifiedSubfilter instanceof NoneFilter) {
                        return FilterCreationUtil.createNone(prismContext);
                    } else if (simplifiedSubfilter == null || simplifiedSubfilter instanceof AllFilter) {
                        // skip
                    } else {
                        simplifiedFilter.addCondition(simplifiedSubfilter);
                    }
                }
            }
            if (simplifiedFilter.isEmpty()) {
                return FilterCreationUtil.createAll(prismContext);
            } else if (simplifiedFilter.getConditions().size() == 1) {
                return simplifiedFilter.getConditions().iterator().next();
            } else {
                return simplifiedFilter;
            }
        } else if (filter instanceof OrFilter) {
            List<ObjectFilter> conditions = ((OrFilter) filter).getConditions();
            OrFilter simplifiedFilter = ((OrFilter) filter).cloneEmpty();
            for (ObjectFilter subfilter : conditions) {
                if (subfilter instanceof NoneFilter || subfilter instanceof UndefinedFilter) {
                    // OR with "false", just skip it
                } else if (subfilter instanceof AllFilter) {
                    // OR with "true"
                    return FilterCreationUtil.createAll(prismContext);
                } else {
                    ObjectFilter simplifiedSubfilter = simplify(subfilter, prismContext);
                    if (simplifiedSubfilter instanceof NoneFilter) {
                        // skip
                    } else if (simplifiedSubfilter == null || simplifiedSubfilter instanceof AllFilter) {
                        return FilterCreationUtil.createNone(prismContext);
                    } else {
                        simplifiedFilter.addCondition(simplifiedSubfilter);
                    }
                }
            }
            if (simplifiedFilter.isEmpty()) {
                return FilterCreationUtil.createNone(prismContext);
            } else if (simplifiedFilter.getConditions().size() == 1) {
                return simplifiedFilter.getConditions().iterator().next();
            } else {
                return simplifiedFilter;
            }

        } else if (filter instanceof NotFilter) {
            ObjectFilter subfilter = ((NotFilter) filter).getFilter();
            ObjectFilter simplifiedSubfilter = simplify(subfilter, prismContext);
            if (simplifiedSubfilter instanceof NoneFilter) {
                return FilterCreationUtil.createAll(prismContext);
            } else if (simplifiedSubfilter == null || simplifiedSubfilter instanceof AllFilter) {
                return FilterCreationUtil.createNone(prismContext);
            } else {
                NotFilter simplifiedFilter = ((NotFilter) filter).cloneEmpty();
                simplifiedFilter.setFilter(simplifiedSubfilter);
                return simplifiedFilter;
            }
        } else if (filter instanceof TypeFilter) {
            ObjectFilter subFilter = ((TypeFilter) filter).getFilter();
            ObjectFilter simplifiedSubfilter = simplify(subFilter, prismContext);
            if (simplifiedSubfilter instanceof AllFilter) {
                simplifiedSubfilter = null;
            } else if (simplifiedSubfilter instanceof NoneFilter) {
                return FilterCreationUtil.createNone(prismContext);
            }
            TypeFilter simplifiedFilter = ((TypeFilter) filter).cloneEmpty();
            simplifiedFilter.setFilter(simplifiedSubfilter);
            return simplifiedFilter;
        } else if (filter instanceof ExistsFilter) {
            ObjectFilter subFilter = ((ExistsFilter) filter).getFilter();
            ObjectFilter simplifiedSubfilter = simplify(subFilter, prismContext);
            if (simplifiedSubfilter instanceof AllFilter) {
                simplifiedSubfilter = null;
            } else if (simplifiedSubfilter instanceof NoneFilter) {
                return FilterCreationUtil.createNone(prismContext);
            }
            ExistsFilter simplifiedFilter = ((ExistsFilter) filter).cloneEmpty();
            simplifiedFilter.setFilter(simplifiedSubfilter);
            return simplifiedFilter;
        } else if (filter instanceof AllFilter) {
            return filter;
        } else if (filter instanceof UndefinedFilter) {
            return null;
        } else if (filter instanceof InOidFilter) {
            if (isEmpty(((InOidFilter) filter).getOids())) {
                // (MID-4193) InOid filter with empty lists are not reasonably evaluable in HQL.
                // As a general rule we can assume that these filters would always yield zero records
                // so they can be replaced by None filter. Should this assumption turn out to be invalid,
                // remove this optimization and implement correct behavior in repo query interpreter.
                return FilterCreationUtil.createNone(prismContext);
            } else {
                return filter.clone();
            }
        } else {
            // Cannot simplify
            return filter.clone();
        }
    }

    private static PrismValue getValueFromFilter(ObjectFilter filter, QName itemName,
            PrismContext prismContext) throws SchemaException {
        Collection<PrismValue> values = getValuesFromFilter(filter, itemName, prismContext);
        if (values == null || values.size() == 0) {
            return null;
        } else if (values.size() > 1) {
            throw new SchemaException("More than one " + itemName + " defined in the search query.");
        } else {
            return values.iterator().next();
        }
    }

    private static <T extends PrismValue> Collection<T> getValuesFromFilter(ObjectFilter filter, QName itemName,
            PrismContext prismContext) throws SchemaException {
        ItemPath propertyPath = ItemName.fromQName(itemName);
        if (filter instanceof EqualFilter && propertyPath.equivalent(((EqualFilter) filter).getFullPath())) {
            return ((EqualFilter) filter).getValues();
        } else if (filter instanceof RefFilter && propertyPath.equivalent(((RefFilter) filter).getFullPath())) {
            return (Collection<T>) ((RefFilter) filter).getValues();
        } else if (filter instanceof AndFilter) {
            return getValuesFromFilter(((NaryLogicalFilter) filter).getConditions(), itemName, prismContext);
        } else if (filter instanceof TypeFilter) {
            return getValuesFromFilter(((TypeFilter) filter).getFilter(), itemName, prismContext);
        } else {
            return null;
        }
    }

    private static <T extends PrismValue> Collection<T> getValuesFromFilter(List<? extends ObjectFilter> conditions, QName propertyName, PrismContext prismContext)
            throws SchemaException {
        for (ObjectFilter f : conditions) {
            Collection<T> values = getValuesFromFilter(f, propertyName, prismContext);
            if (values != null) {
                return values;
            }
        }
        return null;
    }

    private static String getResourceOidFromFilter(ObjectFilter filter, PrismContext prismContext) throws SchemaException {
        PrismReferenceValue referenceValue = (PrismReferenceValue) getValueFromFilter(filter, ShadowType.F_RESOURCE_REF, prismContext);
        return referenceValue != null ? referenceValue.getOid() : null;
    }

    private static <T> T getPropertyRealValueFromFilter(ObjectFilter filter, QName propertyName, PrismContext prismContext) throws SchemaException {
        PrismPropertyValue<T> propertyValue = (PrismPropertyValue<T>) getValueFromFilter(filter, propertyName, prismContext);
        return propertyValue != null ? propertyValue.getValue() : null;
    }

    public static ResourceShadowDiscriminator getCoordinates(ObjectFilter filter,
            PrismContext prismContext) throws SchemaException {
        String resourceOid = getResourceOidFromFilter(filter, prismContext);
        QName objectClass = getPropertyRealValueFromFilter(filter, ShadowType.F_OBJECT_CLASS, prismContext);
        ShadowKindType kind = getKindFromFilter(filter, prismContext);
        String intent = getPropertyRealValueFromFilter(filter, ShadowType.F_INTENT, prismContext);
        String tag = getPropertyRealValueFromFilter(filter, ShadowType.F_TAG, prismContext);

        if (resourceOid == null) {
            throw new SchemaException("Resource not defined in a search query");
        }
        if (objectClass == null && kind == null) {
            throw new SchemaException("Neither objectclass not kind is specified in a search query");
        }

        ResourceShadowDiscriminator coordinates = new ResourceShadowDiscriminator(resourceOid, kind, intent, tag, false);
        coordinates.setObjectClass(objectClass);
        return coordinates;
    }

    private static ShadowKindType getKindFromFilter(ObjectFilter filter, PrismContext prismContext) throws SchemaException {
        return getPropertyRealValueFromFilter(filter, ShadowType.F_KIND, prismContext);
    }

    // Creates references for querying
    public static List<PrismReferenceValue> createReferences(String oid, RelationKindType kind,
            RelationRegistry relationRegistry) {
        return createReferences(singleton(oid), kind, relationRegistry);
    }

    public static List<PrismReferenceValue> createReferences(Collection<String> oids, RelationKindType kind,
            RelationRegistry relationRegistry) {
        List<PrismReferenceValue> rv = new ArrayList<>();
        for (QName relation : relationRegistry.getAllRelationsFor(kind)) {
            for (String oid : oids) {
                rv.add(new ObjectReferenceType().oid(oid).relation(relation).asReferenceValue());
            }
        }
        return rv;
    }

    public static ObjectQuery addConjunctions(ObjectQuery query, ObjectFilter... newConjunctionMembers) {
        return addConjunctions(query, PrismContext.get(), MiscUtil.asListTreatingNull(newConjunctionMembers));
    }

    public static ObjectQuery addConjunctions(ObjectQuery query, PrismContext prismContext,
            ObjectFilter... newConjunctionMembers) {
        return addConjunctions(query, prismContext, MiscUtil.asListTreatingNull(newConjunctionMembers));
    }

    public static ObjectQuery addConjunctions(ObjectQuery query, PrismContext prismContext,
            Collection<ObjectFilter> newConjunctionMembers) {

        if (newConjunctionMembers.isEmpty()) {
            return query;
        }
        List<ObjectFilter> allConjunctionMembers = mergeConjunctions(query, newConjunctionMembers);

        ObjectFilter updatedFilter;
        if (allConjunctionMembers.size() == 1) {
            updatedFilter = allConjunctionMembers.get(0);
        } else if (allConjunctionMembers.size() > 1) {
            updatedFilter = prismContext.queryFactory().createAnd(allConjunctionMembers);
        } else {
            throw new AssertionError();
        }
        return replaceFilter(query, updatedFilter);
    }

    // We intentionally copy new members even if there's no existing filter (to decouple from the original list)
    @NotNull
    private static List<ObjectFilter> mergeConjunctions(ObjectQuery query, Collection<ObjectFilter> newConjunctionMembers) {
        List<ObjectFilter> allConjunctionMembers = new ArrayList<>(newConjunctionMembers.size() + 1);

        ObjectFilter existingFilter = query != null ? query.getFilter() : null;
        if (existingFilter != null) {
            allConjunctionMembers.add(existingFilter);
        }
        allConjunctionMembers.addAll(newConjunctionMembers);
        return allConjunctionMembers;
    }

    public static boolean hasFilter(ObjectQuery query) {
        return query != null && query.getFilter() != null; // TODO and "filter is not empty"?
    }

    public static @NotNull ObjectQuery replaceFilter(ObjectQuery original, ObjectFilter newFilter) {
        ObjectQuery updatedQuery = original != null ? original.clone() : PrismContext.get().queryFactory().createQuery();
        updatedQuery.setFilter(newFilter);
        return updatedQuery;
    }

    public static ObjectPaging convertToObjectPaging(PagingType pagingType, PrismContext prismContext) {
        return PagingConvertor.createObjectPaging(pagingType, prismContext);
    }
}

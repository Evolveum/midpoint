/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale;

import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

import static com.evolveum.midpoint.repo.api.RepositoryService.OP_SEARCH_REFERENCES_ITERATIVE;
import static com.evolveum.midpoint.repo.api.RepositoryService.OP_SEARCH_REFERENCES_ITERATIVE_PAGE;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

import com.evolveum.midpoint.schema.util.ObjectQueryUtil;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.impl.query.OwnedByFilterImpl;
import com.evolveum.midpoint.prism.impl.query.RefFilterImpl;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.*;
import com.evolveum.midpoint.prism.query.builder.S_ConditionEntry;
import com.evolveum.midpoint.repo.sqale.filtering.RefItemFilterProcessor;
import com.evolveum.midpoint.repo.sqlbase.QueryException;
import com.evolveum.midpoint.repo.sqlbase.RepositoryException;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.ObjectHandler;
import com.evolveum.midpoint.schema.SearchResultMetadata;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

/**
 * Logic details for {@link SqaleRepositoryService#searchReferencesIterative}.
 *
 * [NOTE]
 * This class uses prism-impl, because query factory does not support owned-by filter.
 * Query factory is also mostly deprecated, but maybe it's not a bad idea to have it for cases like this.
 */
public class ReferenceIterativeSearch {

    private final SqaleRepositoryService repoService;

    public ReferenceIterativeSearch(SqaleRepositoryService sqaleRepositoryService) {
        this.repoService = sqaleRepositoryService;
    }

    public SearchResultMetadata execute(
            @NotNull ObjectQuery originalQuery,
            ObjectHandler<ObjectReferenceType> handler,
            Collection<SelectorOptions<GetOperationOptions>> options,
            OperationResult operationResult)
            throws RepositoryException, SchemaException, ObjectNotFoundException {
        try {
            ObjectPaging originalPaging = originalQuery.getPaging();
            // this is total requested size of the search
            Integer maxSize = originalPaging != null ? originalPaging.getMaxSize() : null;
            Integer offset = originalPaging != null ? originalPaging.getOffset() : null;

            List<? extends ObjectOrdering> providedOrdering = originalPaging != null
                    ? originalPaging.getOrderingInstructions()
                    : null;
            if (providedOrdering != null && providedOrdering.size() > 1) {
                throw new QueryException("searchReferencesIterative() does not support ordering"
                        + " by multiple paths (yet): " + providedOrdering);
            }

            ObjectQuery pagedQuery = queryFactory().createQuery();
            ObjectPaging paging = queryFactory().createPaging();
            if (originalPaging != null && originalPaging.getOrderingInstructions() != null) {
                originalPaging.getOrderingInstructions().forEach(o ->
                        paging.addOrderingInstruction(o.getOrderBy(), o.getDirection()));
            }
            // Order by the whole ref - this is a trick working only for repo and uses all PK columns.
            // We want to order the ref in the same direction as the provided ordering.
            // This is also reflected by GT/LT conditions in lastOidCondition() method.
            paging.addOrderingInstruction(ItemPath.create(PrismConstants.T_SELF),
                    providedOrdering != null && providedOrdering.size() == 1
                            && providedOrdering.get(0).getDirection() == OrderDirection.DESCENDING
                            ? OrderDirection.DESCENDING : OrderDirection.ASCENDING);
            pagedQuery.setPaging(paging);

            int pageSize = Math.min(
                    repoService.repositoryConfiguration().getIterativeSearchByPagingBatchSize(),
                    defaultIfNull(maxSize, Integer.MAX_VALUE));
            pagedQuery.getPaging().setMaxSize(pageSize);
            pagedQuery.getPaging().setOffset(offset);

            ObjectReferenceType lastProcessedRef = null;
            int handledObjectsTotal = 0;

            while (true) {
                if (maxSize != null && maxSize - handledObjectsTotal < pageSize) {
                    // relevant only for the last page
                    pagedQuery.getPaging().setMaxSize(maxSize - handledObjectsTotal);
                }

                //simplify prevents confusion over ownedBy filter not found
                pagedQuery.setFilter(ObjectQueryUtil.simplify(
                        lastRefFilter(lastProcessedRef, originalQuery.getFilter(), providedOrdering)));

                // we don't call public searchReferences to avoid subresults and query simplification
                repoService.logSearchInputParameters(ObjectReferenceType.class, pagedQuery, "Search object iterative page");
                List<ObjectReferenceType> objects = repoService.executeSearchReferences(
                        pagedQuery, options, OP_SEARCH_REFERENCES_ITERATIVE_PAGE);

                // process page results
                for (ObjectReferenceType object : objects) {
                    lastProcessedRef = object;
                    if (!handler.handle(object, operationResult)) {
                        return new SearchResultMetadata()
                                .approxNumberOfAllResults(handledObjectsTotal + 1)
                                .pagingCookie(pagingCookie(lastProcessedRef))
                                .partialResults(true);
                    }
                    handledObjectsTotal += 1;

                    if (maxSize != null && handledObjectsTotal >= maxSize) {
                        return new SearchResultMetadata()
                                .approxNumberOfAllResults(handledObjectsTotal)
                                .pagingCookie(pagingCookie(lastProcessedRef));
                    }
                }

                if (objects.isEmpty() || objects.size() < pageSize) {
                    return new SearchResultMetadata()
                            .approxNumberOfAllResults(handledObjectsTotal)
                            .pagingCookie(lastProcessedRef != null
                                    ? pagingCookie(lastProcessedRef) : null);
                }
                pagedQuery.getPaging().setOffset(null);
            }
        } finally {
            // This just counts the operation and adds zero/minimal time not to confuse user
            // with what could be possibly very long duration.
            long opHandle = repoService.registerOperationStart(OP_SEARCH_REFERENCES_ITERATIVE, ObjectReferenceType.class);
            repoService.registerOperationFinish(opHandle);
        }
    }

    private String pagingCookie(ObjectReferenceType lastProcessedRef) {
        return Objects.requireNonNull(PrismValueUtil.getParentObject(lastProcessedRef.asReferenceValue()))
                .getOid() + '|' + lastProcessedRef.getRelation() + '|' + lastProcessedRef.getOid();
    }

    /**
     * This one is more complicated than for container/object iterative searches, because:
     *
     * * If order by `@/name` (target reference item) is used, we need to add or modify existing REF filter
     * with appropriate LT/GT condition inside the nested target filter.
     * * If order by `../name` (owner item) is used, we need to modify existing OWNED-BY nested filter.
     *
     * Any other order is unlikely, because ref itself is not sortable from the user's perspective.
     * Also, getting the actual value for any of these items for the last row of previous page is nontrivial.
     */
    private ObjectFilter lastRefFilter(
            ObjectReferenceType lastProcessedRef,
            ObjectFilter originalFilter,
            List<? extends ObjectOrdering> providedOrdering) throws SchemaException, ObjectNotFoundException, QueryException {
        if (lastProcessedRef == null) {
            return originalFilter;
        }

        // We may modify the filter, so we rather clone it to preserve the original for the next loop.
        ObjectFilter filter = originalFilter.clone();

        // Special kind of value for ref filter comparison + the definition for filters:
        if (providedOrdering == null || providedOrdering.isEmpty()) {
            return repoService.prismContext().queryFor(ObjectType.class)
                    .filter(filter)
                    .and()
                    .item(ItemPath.SELF_PATH, lastProcessedRef.asReferenceValue().getDefinition())
                    .gt(lastProcessedRefToRowValue(lastProcessedRef))
                    .buildFilter();

        } else if (providedOrdering.size() == 1) {
            ObjectOrdering objectOrdering = providedOrdering.get(0);
            ItemPath orderByPath = objectOrdering.getOrderBy();
            if (orderByPath.startsWithObjectReference()) {
                return lastRefFilterWithTargetItemOrder(lastProcessedRef, filter, objectOrdering);
            } else if (orderByPath.startsWithParent()) {
                return processParentItemOrder(lastProcessedRef, filter, objectOrdering);
            } else {
                throw new QueryException("Reference search supports only sorting starting with @/ or ../");
            }
        } else {
            throw new QueryException("searchReferencesIterative() does not support ordering"
                    + " by multiple paths (yet): " + providedOrdering);
        }
    }

    private ObjectFilter lastRefFilterWithTargetItemOrder(
            ObjectReferenceType lastProcessedRef, ObjectFilter filter, ObjectOrdering objectOrdering)
            throws SchemaException, ObjectNotFoundException {
        PrismReferenceValue refValue = lastProcessedRef.asReferenceValue();
        PrismObject<?> target = refValue.getObject();
        if (target == null) {
            Class<? extends ObjectType> targetType = repoService.sqlRepoContext().qNameToSchemaClass(refValue.getTargetType());
            target = repoService.readByOid(targetType, SqaleUtils.oidToUuid(refValue.getOid()), null).asPrismObject();
        }

        ItemPath itemPathInTarget = objectOrdering.getOrderBy().rest(); // skipping the dereference (@) part
        Item<PrismValue, ItemDefinition<?>> orderByItem = target.findItem(itemPathInTarget);

        if (filter instanceof OwnedByFilter) {
            // There is no ref filter and we will add it to enforce strict ordering.
            return queryFactory().createAnd(filter,
                    // Not really equal in this case. ;-)
                    RefFilterImpl.createReferenceEqual(
                            ItemPath.SELF_PATH,
                            lastProcessedRef.asReferenceValue().getDefinition(),
                            (Collection<PrismReferenceValue>) null,
                            constructStrictReferenceOrderingCondition(
                                    lastProcessedRef, orderByItem, itemPathInTarget, objectOrdering)));
        } else if (filter instanceof AndFilter) {
            // There is/are some ref filter(s) and we will amend it/them to enforce strict ordering.
            AndFilter andFilter = queryFactory().createAnd();
            for (ObjectFilter condition : ((AndFilter) filter).getConditions()) {
                if (condition instanceof RefFilter) {
                    andFilter.addCondition(processRefForTargetItemOrder(
                            lastProcessedRef, (RefFilter) condition, objectOrdering, itemPathInTarget, orderByItem));
                } else {
                    andFilter.addCondition(condition);
                }
            }
            return andFilter;
        } else {
            throw new IllegalArgumentException(
                    "Filter for reference search iteration must by either OWNED-BY or AND."
                            + " Used filter: " + filter);
        }
    }

    private ObjectFilter processRefForTargetItemOrder(
            ObjectReferenceType lastProcessedRef, RefFilter filter, ObjectOrdering objectOrdering,
            ItemPath itemPathInTarget, Item<PrismValue, ItemDefinition<?>> orderByItem) {
        ObjectFilter targetFilter = filter.getFilter();
        ObjectFilter strictOrderingCondition = constructStrictReferenceOrderingCondition(
                lastProcessedRef, orderByItem, itemPathInTarget, objectOrdering);
        return RefFilterImpl.createReferenceEqual(filter.getPath(), filter.getDefinition(),
                PrismValueCollectionsUtil.cloneCollection(filter.getValues()), // clone to avoid parent reset error
                targetFilter == null
                        ? strictOrderingCondition
                        : queryFactory().createAnd(targetFilter, strictOrderingCondition));
    }

    private ObjectFilter processParentItemOrder(
            ObjectReferenceType lastProcessedRef, ObjectFilter filter, ObjectOrdering objectOrdering) {
        if (filter instanceof OwnedByFilter) {
            return processOwnedByForParentItemOrder(lastProcessedRef, (OwnedByFilter) filter, objectOrdering);
        } else if (filter instanceof AndFilter) {
            // Modify the owned-by filter just like above, the rest stays as-is.
            AndFilter andFilter = queryFactory().createAnd();
            for (ObjectFilter condition : ((AndFilter) filter).getConditions()) {
                if (condition instanceof OwnedByFilter) {
                    andFilter.addCondition(processOwnedByForParentItemOrder(
                            lastProcessedRef, (OwnedByFilter) condition, objectOrdering));
                } else {
                    andFilter.addCondition(condition);
                }
            }
            return andFilter;
        } else {
            throw new IllegalArgumentException(
                    "Filter for reference search iteration must by either OWNED-BY or AND."
                            + " Used filter: " + filter);
        }
    }

    private ObjectFilter processOwnedByForParentItemOrder(
            ObjectReferenceType lastProcessedRef, OwnedByFilter filter, ObjectOrdering objectOrdering) {
        ItemPath itemPathInOwner = objectOrdering.getOrderBy().rest();
        PrismObject<?> owner = Objects.requireNonNull(
                PrismValueUtil.getParentObject(lastProcessedRef.asReferenceValue()));
        Item<PrismValue, ItemDefinition<?>> orderByItem = owner.findItem(itemPathInOwner);

        ObjectFilter ownerFilter = filter.getFilter();
        if (ownerFilter == null) {
            // We will create new ownedBy filter with owner filter:
            return OwnedByFilterImpl.create(filter.getType(), filter.getPath(),
                    constructStrictReferenceOrderingCondition(
                            lastProcessedRef, orderByItem, itemPathInOwner, objectOrdering));
        } else {
            return OwnedByFilterImpl.create(filter.getType(), filter.getPath(),
                    queryFactory().createAnd(ownerFilter,
                            constructStrictReferenceOrderingCondition(
                                    lastProcessedRef, orderByItem, itemPathInOwner, objectOrdering)));
        }
    }

    private ObjectFilter constructStrictReferenceOrderingCondition(
            ObjectReferenceType lastProcessedRef,
            Item<PrismValue, ItemDefinition<?>> orderByItem,
            ItemPath orderByPath,
            ObjectOrdering objectOrdering) {
        RefItemFilterProcessor.ReferenceRowValue refComparableValue = lastProcessedRefToRowValue(lastProcessedRef);
        PrismReferenceDefinition refDef = lastProcessedRef.asReferenceValue().getDefinition();

        boolean isPolyString = QNameUtil.match(
                PolyStringType.COMPLEX_TYPE, orderByItem.getDefinition().getTypeName());
        Object realValue = orderByItem.getRealValue();
        S_ConditionEntry filterBuilder = repoService.prismContext().queryFor(ObjectType.class)
                .item(orderByPath, orderByItem.getDefinition());

        if (isPolyString) {
            // We need to use matchingOrig for polystring, see MID-7860
            if (objectOrdering.getDirection() != OrderDirection.DESCENDING) {
                return filterBuilder.gt(realValue).matchingOrig().or()
                        .block()
                        .item(orderByPath).eq(realValue).matchingOrig()
                        .and()
                        .item(ItemPath.SELF_PATH, refDef).gt(refComparableValue)
                        .endBlock()
                        .buildFilter();
            } else {
                return filterBuilder.lt(realValue).matchingOrig().or()
                        .block()
                        .item(orderByPath).eq(realValue).matchingOrig()
                        .and()
                        .item(ItemPath.SELF_PATH, refDef).lt(refComparableValue)
                        .endBlock()
                        .buildFilter();
            }
        } else {
            if (objectOrdering.getDirection() != OrderDirection.DESCENDING) {
                return filterBuilder.gt(realValue).or()
                        .block()
                        .item(orderByPath).eq(realValue)
                        .and()
                        .item(ItemPath.SELF_PATH, refDef).gt(refComparableValue)
                        .endBlock()
                        .buildFilter();
            } else {
                return filterBuilder.lt(realValue).or()
                        .block()
                        .item(orderByPath).eq(realValue)
                        .and()
                        .item(ItemPath.SELF_PATH, refDef).lt(refComparableValue)
                        .endBlock()
                        .buildFilter();
            }
        }
    }

    @NotNull
    private static RefItemFilterProcessor.ReferenceRowValue lastProcessedRefToRowValue(ObjectReferenceType lastProcessedRef) {
        RefItemFilterProcessor.ReferenceRowValue refComparableValue =
                new RefItemFilterProcessor.ReferenceRowValue(
                        Objects.requireNonNull(PrismValueUtil.getParentObject(lastProcessedRef.asReferenceValue()))
                                .getOid(),
                        lastProcessedRef.getRelation(),
                        lastProcessedRef.getOid());
        return refComparableValue;
    }

    @NotNull
    private QueryFactory queryFactory() {
        return repoService.prismContext().queryFactory();
    }
}

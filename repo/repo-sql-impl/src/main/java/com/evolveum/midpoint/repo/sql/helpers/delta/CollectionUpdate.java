/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.helpers.delta;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.equivalence.EquivalenceStrategy;
import com.evolveum.midpoint.repo.sql.helpers.modify.MapperContext;
import com.evolveum.midpoint.repo.sql.util.EntityState;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * "Update" operation on Hibernate collection.
 */
class CollectionUpdate<R, V extends PrismValue, I extends Item<V, ?>, ID extends ItemDelta<V, ?>> {

    private static final Trace LOGGER = TraceManager.getTrace(CollectionUpdate.class);

    /**
     * Collection that is to be updated.
     */
    private final Collection<R> targetCollection;

    /**
     * Owning object. (E.g. RUser if the collection is a set of assignments.)
     */
    private final Object collectionOwner;

    /**
     * Delta that is to be applied.
     */
    private final ID delta;

    /**
     * Existing item value (before delta application).
     */
    final I existingItem;

    /**
     * Type of objects in the collection.
     */
    private final Class<R> attributeValueType;

    final UpdateContext ctx;

    CollectionUpdate(Collection<R> targetCollection, Object collectionOwner,
            PrismObject<? extends ObjectType> prismObject, ID delta,
            Class<R> attributeValueType, UpdateContext ctx) {
        this.targetCollection = targetCollection;
        this.collectionOwner = collectionOwner;
        this.delta = delta;
        //noinspection unchecked
        this.existingItem = (I) prismObject.findItem(delta.getPath());
        this.attributeValueType = attributeValueType;
        this.ctx = ctx;
    }

    public void execute() {
        Collection<ItemModifyResult<V>> results = delta.applyResults();
        if (results == null) {
            return;
        }

        Map<ItemModifyResult.ActualApplyOperation, List<V>> changes = results.stream()
                .collect(Collectors.groupingBy(
                        r -> r.operation(),
                        Collectors.mapping(r -> r.finalValue, Collectors.toList())
                ));

        deleteValues(changes.get(ItemModifyResult.ActualApplyOperation.DELETED));
        addValues(changes.get(ItemModifyResult.ActualApplyOperation.ADDED));
        modifyValues(changes.get(ItemModifyResult.ActualApplyOperation.MODIFIED));
    }

    private void modifyValues(Collection<V> valuesToModify) {
        if (valuesToModify == null) {
            return;
        }

        for (V valueToModify : valuesToModify) {
            deleteExistingValue(valueToModify);
            addValues(List.of(valueToModify));
        }
    }

    private void addValues(Collection<V> valuesToAdd) {
        if (valuesToAdd == null) {
            return;
        }

        loadTargetCollection();
        for (V valueToAdd : valuesToAdd) {
            R repoValueToAdd = mapToRepo(valueToAdd, true);
            V existingPrismValue = findExistingValue(valueToAdd);
            R adaptedRepoValueToAdd = adaptValueBeforeAddition(repoValueToAdd, valueToAdd, existingPrismValue);

            LOGGER.trace("Adding value: {} / {}", adaptedRepoValueToAdd, valuesToAdd);
            targetCollection.add(adaptedRepoValueToAdd);
        }
    }

    /**
     * Really ugly hack. It looks like adding into the collection produces exceptions
     * if the collection was not loaded at least once.
     */
    private void loadTargetCollection() {
        LOGGER.trace("Size of target collection (just to load it): {}", targetCollection.size());
    }

    private void deleteValues(Collection<V> valuesToDelete) {
        if (valuesToDelete == null) {
            return;
        }

        for (V valueToDelete : valuesToDelete) {
            deleteExistingValue(valueToDelete);
        }
    }

    private void deleteExistingValue(V existingValue) {
        R repoValueToDelete = mapToRepo(existingValue, false);
        R existingRepoValue = targetCollection.stream()
                .filter(v -> v.equals(repoValueToDelete))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Repository tables inconsistency! Value " + existingValue
                        + " is present according to the full object data but was missing in the repository."
                        + " Existing repo values: " + targetCollection));

        LOGGER.trace("Deleting value: {} / {}", existingRepoValue, existingValue);
        targetCollection.remove(existingRepoValue);
    }

    private R mapToRepo(V value, boolean trans) {
        MapperContext context = new MapperContext();
        context.setRepositoryContext(ctx.beans.createRepositoryContext());
        context.setDelta(delta);
        context.setOwner(collectionOwner);

        R repo = ctx.beans.prismEntityMapper.mapPrismValue(value, attributeValueType, context);
        if (repo instanceof EntityState) {
            ((EntityState) repo).setTransient(trans);
        }
        return repo;
    }

    R adaptValueBeforeAddition(R repoValueToAdd, V valueToAdd, V existingValue) {
        if (existingValue != null && repoValueToAdd instanceof EntityState) {
            LOGGER.trace("Value to add already exists in the object. So merging it with the repo. Value: {}", repoValueToAdd);
            //noinspection unchecked
            return ctx.entityManager.merge(repoValueToAdd);
        } else {
            return repoValueToAdd;
        }
    }

    V findExistingValue(V value) {
        if (existingItem != null) {
            return existingItem.findValue(value, EquivalenceStrategy.REAL_VALUE);
        } else {
            return null;
        }
    }
}

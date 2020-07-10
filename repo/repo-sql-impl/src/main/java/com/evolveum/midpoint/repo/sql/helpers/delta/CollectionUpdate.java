/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.helpers.delta;

import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.equivalence.EquivalenceStrategy;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.sql.data.common.container.Container;
import com.evolveum.midpoint.repo.sql.helpers.modify.MapperContext;
import com.evolveum.midpoint.repo.sql.helpers.modify.PrismEntityPair;
import com.evolveum.midpoint.repo.sql.util.EntityState;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * "Update" operation on Hibernate collection.
 */
class CollectionUpdate<T, V extends PrismValue> {

    private static final Trace LOGGER = TraceManager.getTrace(GeneralUpdate.class);

    /**
     * Collection that is to be updated.
     */
    private final Collection<T> targetCollection;

    /**
     * Owning object. (E.g. RUser if the collection is a set of assignments.)
     */
    private final Object collectionOwner;

    /**
     * Delta that is to be applied.
     */
    private final ItemDelta<V, ?> delta;

    /**
     * Existing item value (before delta application).
     */
    private final Item<V, ?> existingItem;

    /**
     * Type of objects in the collection.
     */
    private final Class<T> attributeValueType;

    /**
     * Identifiers that were (originally) in the repo collection.
     *
     * They are used as an optimization tool: if we add an ID that is in this collection,
     * we should call session.merge beforehand, because we need to replace existing database object.
     */
    private final Set<Integer> idsInRepo;

    private final UpdateContext ctx;

    CollectionUpdate(Collection<T> targetCollection, Object collectionOwner,
            PrismObject<? extends ObjectType> prismObject, ItemDelta<V, ?> delta,
            Class<T> attributeValueType, UpdateContext ctx) {
        this.targetCollection = targetCollection;
        this.collectionOwner = collectionOwner;
        this.delta = delta;
        this.existingItem = prismObject.findItem(delta.getPath());
        this.attributeValueType = attributeValueType;
        this.ctx = ctx;
        this.idsInRepo = collectExistingIdFromRepo(targetCollection);
    }

    public void execute() {

        if (delta.isReplace()) {
            replaceValues(delta.getValuesToReplace());
        } else {
            if (delta.isDelete()) {
                deleteValues(delta.getValuesToDelete());
            }
            if (delta.isAdd()) {
                addValues(delta.getValuesToAdd());
            }
        }
    }

    private void addValues(Collection<V> valuesToAdd) {
        Collection<PrismEntityPair<T>> pairsToAdd = convertToPrismEntityPairs(valuesToAdd);
        markNewOnesTransientAndAddToExisting(targetCollection, pairsToAdd);
    }

    private Collection<PrismEntityPair<T>> convertToPrismEntityPairs(Collection<V> values) {
        Collection<PrismEntityPair<T>> pairs = new ArrayList<>();
        for (V value : MiscUtil.emptyIfNull(values)) {
            MapperContext context = new MapperContext();
            context.setRepositoryContext(ctx.beans.createRepositoryContext());
            context.setDelta(delta);
            context.setOwner(collectionOwner);

            T result = ctx.beans.prismEntityMapper.mapPrismValue(value, attributeValueType, context);
            pairs.add(new PrismEntityPair<>(value, result));
        }
        return pairs;
    }

    private void deleteValues(Collection<V> valuesToDelete) {
        if (targetCollection.isEmpty() || valuesToDelete.isEmpty()) {
            return;
        }

        Collection<PrismEntityPair<T>> pairsToDelete = convertToPrismEntityPairs(valuesToDelete);

        Collection<PrismEntityPair<T>> existingPairs = createExistingPairs();

        Collection<T> toDelete = new ArrayList<>();
        for (PrismEntityPair<T> toDeletePair : pairsToDelete) {
            if (toDeletePair.getRepository() instanceof EntityState) {
                ((EntityState) toDeletePair.getRepository()).setTransient(false);
            }
            PrismEntityPair<T> existingPair = findMatch(existingPairs, toDeletePair);
            if (existingPair != null) {
                toDelete.add(existingPair.getRepository());
            }
        }

        LOGGER.trace("deleteValues: Deleting {}", toDelete);
        targetCollection.removeAll(toDelete);
    }

    private PrismEntityPair<T> findMatch(Collection<PrismEntityPair<T>> collection, PrismEntityPair<T> pair) {
        boolean isContainer = pair.getRepository() instanceof Container;

        Object pairObject = pair.getRepository();

        for (PrismEntityPair<T> item : collection) {
            if (isContainer) {
                Container c = (Container) item.getRepository();
                Container pairContainer = (Container) pairObject;

                if (Objects.equals(c.getId(), pairContainer.getId())
                        || pair.getPrism().equals(item.getPrism(), EquivalenceStrategy.IGNORE_METADATA)) {  //todo
                    return item;
                }
            } else {
                // e.g. RObjectReference
                if (Objects.equals(item.getRepository(), pairObject)) {
                    return item;
                }
            }
        }

        return null;
    }

    private Collection<PrismEntityPair<T>> createExistingPairs() {
        Collection<PrismEntityPair<T>> pairs = new ArrayList<>();

        for (T obj : targetCollection) {
            if (obj instanceof Container) {
                Container container = (Container) obj;

                PrismValue value = (PrismValue) existingItem.find(ItemPath.create(container.getId()));

                pairs.add(new PrismEntityPair<>(value, obj));
            } else {
                // todo improve somehow
                pairs.add(new PrismEntityPair<>(null, obj));
            }
        }

        return pairs;
    }

    private void replaceValues(Collection<V> prismValuesToReplace) {

        Collection<PrismEntityPair<T>> pairsToReplace = convertToPrismEntityPairs(prismValuesToReplace);

        if (targetCollection.isEmpty()) {
            markNewOnesTransientAndAddToExisting(targetCollection, pairsToReplace);
            return;
        }

        Collection<PrismEntityPair<T>> existingPairs = createExistingPairs();

        Collection<T> skipAddingTheseObjects = new ArrayList<>();
        Collection<Integer> skipAddingTheseIds = new ArrayList<>();

        Collection<T> toDelete = new ArrayList<>();

        // mark existing object for deletion, skip if they would be replaced with the same value
        for (PrismEntityPair<T> existingPair : existingPairs) {
            PrismEntityPair<T> toReplacePair = findMatch(pairsToReplace, existingPair);
            if (toReplacePair == null) {
                toDelete.add(existingPair.getRepository());
            } else {
                T existingObject = existingPair.getRepository();
                if (existingObject instanceof Container) {
                    Container c = (Container) existingObject;
                    skipAddingTheseIds.add(c.getId());
                }
                skipAddingTheseObjects.add(existingObject);
            }
        }
        LOGGER.trace("replaceValues: Removing these values: {}", toDelete);
        targetCollection.removeAll(toDelete);

        Iterator<PrismEntityPair<T>> iterator = pairsToReplace.iterator();
        while (iterator.hasNext()) {
            PrismEntityPair<T> pair = iterator.next();
            T obj = pair.getRepository();
            if (obj instanceof Container) {
                Container container = (Container) obj;
                if (container.getId() == null && skipAddingTheseObjects.contains(obj)) {
                    iterator.remove();
                    continue;
                }

                if (skipAddingTheseIds.contains(container.getId())) {
                    iterator.remove();
                }
            } else {
                if (skipAddingTheseObjects.contains(obj)) {
                    iterator.remove();
                }
            }
        }

        markNewOnesTransientAndAddToExisting(targetCollection, pairsToReplace);
    }


    private void markNewOnesTransientAndAddToExisting(Collection collectionToUpdate, Collection<PrismEntityPair<T>> newOnes) {
        for (PrismEntityPair<T> newValue : newOnes) {
            //noinspection unchecked
            V newPrismValue = (V) newValue.getPrism();
            T newRepoValue = newValue.getRepository();

            if (newRepoValue instanceof EntityState) {
                ((EntityState) newRepoValue).setTransient(true);
            }

            if (newRepoValue instanceof Container) {
                PrismContainerValue newPrismContainerValue = (PrismContainerValue) newPrismValue;
                Container newRepoContainerValue = (Container) newRepoValue;

                if (newPrismContainerValue.getId() != null) {
                    Integer clientSuppliedId = newPrismContainerValue.getId().intValue();
                    if (idsInRepo.contains(clientSuppliedId)) {
                        LOGGER.trace("markNewOnesTransientAndAddToExisting: Found conflicting value with client-supplied ID {}",
                                clientSuppliedId);
                        // We do merge only if we know there is a conflict. This is to eliminate needless SQL SELECTs.
                        //noinspection unchecked
                        newRepoValue = (T) ctx.session.merge(newRepoValue);
                    }
                    LOGGER.trace("markNewOnesTransientAndAddToExisting: Setting client-supplied ID {} to {}", clientSuppliedId, newRepoContainerValue);
                    newRepoContainerValue.setId(clientSuppliedId);
                } else {
                    long nextId = ctx.idGenerator.nextId();
                    LOGGER.trace("markNewOnesTransientAndAddToExisting: Setting newly-generated ID {} to {}", nextId, newRepoContainerValue);
                    newRepoContainerValue.setId((int) nextId);
                    newPrismContainerValue.setId(nextId);
                }
            }

            LOGGER.trace("markNewOnesTransientAndAddToExisting: Adding value: {}", newRepoValue);

            //noinspection unchecked
            collectionToUpdate.add(newRepoValue);
        }
    }

    @NotNull
    private Set<Integer> collectExistingIdFromRepo(Collection<T> existing) {
        Set<Integer> existingIds = new HashSet<>();
        for (Object obj : existing) {
            if (obj instanceof Container) {
                Integer id = ((Container) obj).getId();
                if (id != null) {
                    existingIds.add(id);
                }
            }
        }
        return existingIds;
    }
}

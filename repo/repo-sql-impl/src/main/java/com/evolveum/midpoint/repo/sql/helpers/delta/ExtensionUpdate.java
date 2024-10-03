/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.helpers.delta;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

import jakarta.persistence.EntityManager;
import org.hibernate.collection.spi.PersistentCollection;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.repo.api.RepoModifyOptions;
import com.evolveum.midpoint.repo.sql.data.common.RObject;
import com.evolveum.midpoint.repo.sql.data.common.any.*;
import com.evolveum.midpoint.repo.sql.data.common.type.RObjectExtensionType;
import com.evolveum.midpoint.repo.sql.helpers.modify.PrismEntityPair;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;

/**
 * Handles updates in extension (in objects and assignments), and shadow attributes.
 *
 * "Whole container" updates are treated by static methods, translating such requests into sets
 * of item deltas. "Single item" updates are primary focus of this class, see {@link #handleItemDelta()}.
 */
abstract class ExtensionUpdate<E, ET> extends BaseUpdate {

    final E extension;
    final ET extensionType;

    ExtensionUpdate(RObject object, ItemDelta<?, ?> delta, UpdateContext ctx, E extension, ET extensionType) {
        super(object, delta, ctx);
        this.extension = extension;
        this.extensionType = extensionType;
    }

    /**
     * Main entry point.
     */
    void handleItemDelta() throws SchemaException {
        ItemDefinition definition = delta.getDefinition();
        if (definition == null) {
            // todo consider simply returning with no action
            throw new IllegalStateException("Cannot process definition-less extension item: " + delta);
        }
        // TODO is this correct for assignment extension?
        boolean dynamicsIndexed = !(extensionType instanceof RObjectExtensionType)
                || RAnyConverter.areDynamicsOfThisKindIndexed((RObjectExtensionType) extensionType);

        RAnyConverter.ValueType valueType = RAnyConverter.getValueType(definition, definition.getItemName(),
                dynamicsIndexed, beans.prismContext);
        if (valueType == null) {
            return;
        }

        Integer itemId = beans.extItemDictionary.createOrFindItemDefinition(definition).getId();

        if (delta.getValuesToReplace() != null) {
            processExtensionDeltaValueSet(delta.getValuesToReplace(), itemId, valueType,
                    (dbCollection, pairsFromDelta) -> replaceValues(definition, dbCollection, pairsFromDelta));
        } else {
            processExtensionDeltaValueSet(delta.getValuesToDelete(), itemId, valueType, this::deleteValues);
            processExtensionDeltaValueSet(delta.getValuesToAdd(), itemId, valueType, this::addValues);
        }
    }

    @FunctionalInterface
    interface RepositoryUpdater {
        /**
         * Updates repository collection with the pairs (acquired from the delta).
         */
        void update(Collection<? extends RAnyValue<?>> repoCollection, Collection<PrismEntityPair<RAnyValue<?>>> pairs);
    }

    private void processExtensionDeltaValueSet(Collection<? extends PrismValue> prismValuesFromDelta,
            Integer itemId, RAnyConverter.ValueType valueType, RepositoryUpdater repositoryUpdater) {

        if (prismValuesFromDelta != null) {
            RAnyConverter converter = new RAnyConverter(beans.prismContext, beans.extItemDictionary);
            try {
                Collection<PrismEntityPair<RAnyValue<?>>> rValuesFromDelta = new ArrayList<>();
                for (PrismValue prismValueFromDelta : prismValuesFromDelta) {
                    RAnyValue<?> rValueFromDelta = convertToRValue(itemId, converter, prismValueFromDelta);
                    rValuesFromDelta.add(new PrismEntityPair<>(prismValueFromDelta, rValueFromDelta));
                }

                processExtensionValues(valueType, rValuesFromDelta, repositoryUpdater);
            } catch (SchemaException ex) {
                throw new SystemException("Couldn't process extension attributes", ex);
            }
        }
    }

    @NotNull
    abstract RAnyValue<?> convertToRValue(Integer itemId, RAnyConverter converter, PrismValue prismValueFromDelta)
            throws SchemaException;

    abstract void processExtensionValues(RAnyConverter.ValueType valueType,
            Collection<PrismEntityPair<RAnyValue<?>>> pairsFromDelta,
            RepositoryUpdater repositoryUpdater);

    private void addValues(Collection<? extends RAnyValue<?>> dbCollection,
            Collection<PrismEntityPair<RAnyValue<?>>> pairsFromDelta) {
        markNewValuesTransientAndAddToExistingNoFetch(dbCollection, pairsFromDelta, ctx);
    }

    /**
     * Similar to DeltaUpdaterUtils.markNewValuesTransientAndAddToExisting but avoids fetching the whole collection content.
     * See MID-5558.
     */
    private static void markNewValuesTransientAndAddToExistingNoFetch(Collection<? extends RAnyValue<?>> dbCollection,
            Collection<PrismEntityPair<RAnyValue<?>>> newValues, UpdateContext ctx) {
        for (PrismEntityPair<RAnyValue<?>> item : newValues) {
            RAnyValue<?> rValue = item.getRepository();
            boolean exists;
            // TODO temporary measure: when dealing with value metadata we never skip existence check, see MID-6450
            if (!item.getPrism().hasValueMetadata() && Boolean.TRUE.equals(RepoModifyOptions.getUseNoFetchExtensionValuesInsertion(ctx.options))) {
                exists = false; // skipping the check
                ctx.attemptContext.noFetchExtensionValueInsertionAttempted = true; // to know that CVE can be caused because of this
            } else {
                Serializable id = rValue.createId();
                exists = ctx.entityManager.find(rValue.getClass(), id) != null;
            }
            if (!exists) {
                //noinspection unchecked
                ((Collection) dbCollection).add(rValue);
                ctx.entityManager.persist(rValue); // it looks that in this way we avoid SQL SELECT (at the cost of .persist that can be sometimes more costly)
            }
        }
    }

    /**
     * Similar to DeltaUpdaterUtils.markNewValuesTransientAndAddToExisting but simpler.
     * <p>
     * We rely on the fact that SAVE/UPDATE is now cascaded to extension items.
     */
    private static void markNewValuesTransientAndAddToExistingNoFetchNoPersist(Collection<? extends RAnyValue<?>> dbCollection,
            Collection<PrismEntityPair<RAnyValue<?>>> newValues) {
        for (PrismEntityPair<RAnyValue<?>> item : newValues) {
            RAnyValue<?> rValue = item.getRepository();
            //noinspection unchecked
            ((Collection) dbCollection).add(rValue);
        }
    }

    private void deleteValues(Collection<? extends RAnyValue<?>> dbCollection,
            Collection<PrismEntityPair<RAnyValue<?>>> pairsFromDelta) {
        if (pairsFromDelta.isEmpty()) {
            return;
        }
        Collection<RAnyValue<?>> rValuesToDelete = pairsFromDelta.stream()
                .map(PrismEntityPair::getRepository)
                .collect(Collectors.toList());

        boolean collectionLoaded = dbCollection instanceof PersistentCollection &&
                ((PersistentCollection) dbCollection).wasInitialized();

        // Note: as for 4.0 "no fetch" deletion is available only for ROExtString (MID-5558).
        boolean noFetchDeleteSupported =
                Boolean.TRUE.equals(RepoModifyOptions.getUseNoFetchExtensionValuesDeletion(ctx.options)) &&
                        rValuesToDelete.stream().allMatch(rValue -> rValue instanceof ROExtString);

        //System.out.println("Collection loaded = " + collectionLoaded + ", noFetchDeleteSupported = " + noFetchDeleteSupported + " for " + rValuesToDelete);
        if (!collectionLoaded && noFetchDeleteSupported) {
            // We are quite sure we are deleting detached value that has NOT been loaded in this session.
            // But we don't generally use session.delete, because it first loads the transient/detached entity.
            //
            // Of course, we must NOT call dbCollection.remove(...) here. It causes all collection values to be fetched
            // from the database, contradicting MID-5558.

            boolean bulkDelete = false;
            //noinspection ConstantConditions
            if (bulkDelete) {
                List<Serializable> rValueIdList = rValuesToDelete.stream()
                        .map(RAnyValue::createId)
                        .collect(Collectors.toList());
                // This translates to potentially large "OR" query like
                // Query:["delete from m_object_ext_string where
                //           item_id=? and owner_oid=? and ownerType=? and stringValue=? or
                //           item_id=? and owner_oid=? and ownerType=? and stringValue=?"],
                // Params:[(5,6a39f871-4e8c-4c24-bfc1-abebc4a66ee6,0,weapon1,
                //          5,6a39f871-4e8c-4c24-bfc1-abebc4a66ee6,0,weapon2)]
                // I think we should avoid it.
                //
                // We could group related objects (same ownerOid, ownerType, itemId) into one DELETE operation,
                // but that's perhaps too much effort for too little gain.
                //
                // We could try batching but I don't know how to do this in Hibernate. Using native queries is currently
                // too complicated.
                ctx.entityManager.createQuery("delete from ROExtString where id in (:id)")
                        .setParameter("id", rValueIdList)
                        .executeUpdate();
            } else {
                for (RAnyValue<?> value : rValuesToDelete) {
                    ROExtString s = (ROExtString) value;
                    ctx.entityManager.createQuery("delete from ROExtString where ownerOid = :ownerOid and "
                                    + "ownerType = :ownerType and itemId = :itemId and value = :value")
                            .setParameter("ownerOid", s.getOwnerOid())
                            .setParameter("itemId", s.getItemId())
                            .setParameter("ownerType", s.getOwnerType())
                            .setParameter("value", s.getValue())
                            .executeUpdate();
                }
            }
        } else {
            // Traditional deletion
            dbCollection.removeAll(rValuesToDelete);

            // This approach works as well but is needlessly complicated:
//            dbCollection.size();            // to load the collection if it was not loaded before
//            for (RAnyValue<?> rValue : rValuesToDelete) {
//                RAnyValue<?> fromSession = ctx.session.get(rValue.getClass(), rValue.createId());    // there's no additional cost here as the value is already loaded
//                if (fromSession != null) {
//                    dbCollection.remove(fromSession);
//                    ctx.session.delete(fromSession);
//                }
//            }
        }
    }

    private void replaceValues(ItemDefinition definition, Collection<? extends RAnyValue<?>> dbCollection,
            Collection<PrismEntityPair<RAnyValue<?>>> pairsFromDelta) {
        Collection<? extends RAnyValue<?>> relevantInDb = getMatchingValues(dbCollection, definition);

        if (pairsFromDelta.isEmpty()) {
            // if there are not new values, we just remove existing ones
            deleteFromCollectionAndDb(dbCollection, relevantInDb, ctx.entityManager);
            return;
        }

        Collection<RAnyValue<?>> rValuesToDelete = new ArrayList<>();
        Collection<PrismEntityPair<RAnyValue<?>>> pairsToAdd = new ArrayList<>();

        // BEWARE - this algorithm does not work for RAnyValues that have equal "value" but differ in other components
        // (e.g. references: OID vs. relation/type; poly strings: orig vs. norm)
        Set<Object> realValuesToAdd = new HashSet<>();
        for (PrismEntityPair<RAnyValue<?>> pair : pairsFromDelta) {
            realValuesToAdd.add(pair.getRepository().getValue());
        }

        for (RAnyValue value : relevantInDb) {
            if (realValuesToAdd.contains(value.getValue())) {
                // do not replace with the same one - don't touch
                realValuesToAdd.remove(value.getValue());
            } else {
                rValuesToDelete.add(value);
            }
        }

        for (PrismEntityPair<RAnyValue<?>> pair : pairsFromDelta) {
            if (realValuesToAdd.contains(pair.getRepository().getValue())) {
                pairsToAdd.add(pair);
            }
        }

        deleteFromCollectionAndDb(dbCollection, rValuesToDelete, ctx.entityManager);
        markNewValuesTransientAndAddToExistingNoFetchNoPersist(dbCollection, pairsToAdd);
    }

    private void deleteFromCollectionAndDb(Collection<? extends RAnyValue<?>> dbCollection,
            Collection<? extends RAnyValue<?>> valuesToDelete, EntityManager em) {
        //noinspection SuspiciousMethodCalls
        dbCollection.removeAll(valuesToDelete);     // do NOT use for handling regular ADD/DELETE value (fetches the whole collection)
        valuesToDelete.forEach(em::remove);
    }

    // assignmentExtensionType will be used later
    private Collection<RAnyValue<?>> getMatchingValues(Collection<? extends RAnyValue<?>> existing, ItemDefinition def) {

        Collection<RAnyValue<?>> filtered = new ArrayList<>();
        RExtItem extItemDefinition = beans.extItemDictionary.findItemByDefinition(def);
        if (extItemDefinition == null) {
            return filtered;
        }
        for (RAnyValue<?> value : existing) {
            if (value.getItemId() == null) {
                continue;       // suspicious
            }
            if (!value.getItemId().equals(extItemDefinition.getId())) {
                continue;
            }
            if (value instanceof ROExtValue) {
                ROExtValue oValue = (ROExtValue) value;
                if (extensionType != oValue.getOwnerType()) {
                    continue;
                }
            } else if (value instanceof RAExtValue) {
                // we cannot filter on assignmentExtensionType because it is not present in database (yet)
//                RAExtValue aValue = (RAExtValue) value;
//                if (!assignmentExtensionType.equals(aValue.getExtensionType())) {
//                    continue;
//                }
            }

            filtered.add(value);
        }

        return filtered;
    }
}

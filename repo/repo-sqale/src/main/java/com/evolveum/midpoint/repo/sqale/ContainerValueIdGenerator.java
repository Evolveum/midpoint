/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale;

import java.util.*;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.util.exception.SchemaException;

/**
 * Generator assigning missing IDs to PCVs of multi-value containers.
 */
public class ContainerValueIdGenerator {

    private final PrismObject<?> object;
    private final Set<Long> usedIds = new HashSet<>();
    private final List<PrismContainerValue<?>> pcvsWithoutId = new ArrayList<>();

    private long maxUsedId = 0; // tracks max CID (set to duplicate CID if found)

    public ContainerValueIdGenerator(@NotNull PrismObject<?> object) {
        this.object = object;
    }

    /**
     * Method inserts IDs for prism container values without IDs and returns highest CID.
     */
    public long generateForNewObject() throws SchemaException {
        checkExistingContainers(object);
        assignMissingContainerIds();
        return maxUsedId;
    }

    /** Initializes the generator for modify object and checks that no previous CIDs are missing. */
    public ContainerValueIdGenerator forModifyObject() throws SchemaException {
        checkExistingContainers(object);
        if (!pcvsWithoutId.isEmpty()) {
            throw new SchemaException("Stored prism object with OID " + object.getOid()
                    + " has missing CIDs for these container values: " + pcvsWithoutId);
        }
        return this;
    }

    public void processModification(ItemDelta<?, ?> modification) throws SchemaException {
        processModificationValues(modification.getValuesToAdd());
        processModificationValues(modification.getValuesToReplace());
        // values to delete are irrelevant

        assignMissingContainerIds();
    }

    private void processModificationValues(Collection<? extends PrismValue> values)
            throws SchemaException {
        if (values != null) {
            for (PrismValue prismValue : values) {
                if (prismValue instanceof PrismContainerValue) {

                    checkExistingContainers(prismValue);
                }
            }
        }
    }

    /**
     * Checks the provided container (possibly the whole object) and finds values requiring CID.
     */
    private void checkExistingContainers(Visitable<?> object) throws SchemaException {
        try {
            // to cover modification values which are PCV and visitor would skip it
            if (object instanceof PrismContainerValue
                    && ((PrismContainerValue<?>) object).getDefinition().isMultiValue()) {
                processContainerValue((PrismContainerValue<?>) object);
            }

            // now let's go deeper
            object.accept(visitable -> {
                if (visitable instanceof PrismContainer
                        && !(visitable instanceof PrismObject)
                        && ((PrismContainer<?>) visitable).getDefinition().isMultiValue()) {
                    processContainer((PrismContainer<?>) visitable);
                }
            });
        } catch (DuplicateContainerIdException e) {
            throw new SchemaException(
                    "CID " + maxUsedId + " is used repeatedly in the object: " + object);
        }
    }

    private void processContainer(PrismContainer<?> container) {
        for (PrismContainerValue<?> val : container.getValues()) {
            processContainerValue(val);
        }
    }

    private void processContainerValue(PrismContainerValue<?> val) {
        if (val.getId() != null) {
            Long cid = val.getId();
            if (!usedIds.add(cid)) {
                maxUsedId = cid;
                throw DuplicateContainerIdException.INSTANCE;
            }
            maxUsedId = Math.max(maxUsedId, cid);
        } else {
            pcvsWithoutId.add(val);
        }
    }

    /** Generates container IDs for {@link #pcvsWithoutId} and clears the list. */
    private void assignMissingContainerIds() {
        for (PrismContainerValue<?> val : pcvsWithoutId) {
            val.setId(nextId());
        }
        pcvsWithoutId.clear();
    }

    public long nextId() {
        maxUsedId++;
        return maxUsedId;
    }

    public long lastUsedId() {
        return maxUsedId;
    }

    private static class DuplicateContainerIdException extends RuntimeException {
        static final DuplicateContainerIdException INSTANCE = new DuplicateContainerIdException();

        private DuplicateContainerIdException() {
            super(null, null, false, false);
        }
    }
}

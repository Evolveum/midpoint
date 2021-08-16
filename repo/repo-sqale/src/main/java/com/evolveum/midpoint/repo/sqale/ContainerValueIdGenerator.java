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
import com.evolveum.midpoint.prism.equivalence.EquivalenceStrategy;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * Generator assigning missing IDs to PCVs of multi-value containers.
 */
public class ContainerValueIdGenerator {

    private static final Trace LOGGER = TraceManager.getTrace(ContainerValueIdGenerator.class);

    private final PrismObject<? extends ObjectType> object;
    private final Set<Long> usedIds = new HashSet<>();
    private final Set<Long> overwrittenIds = new HashSet<>(); // ids of PCV overwritten by ADD
    private final List<PrismContainerValue<?>> pcvsWithoutId = new ArrayList<>();

    private long maxUsedId = 0; // tracks max CID (set to duplicate CID if found)

    public <S extends ObjectType> ContainerValueIdGenerator(@NotNull PrismObject<S> object) {
        this.object = object;
    }

    /**
     * Method inserts IDs for prism container values without IDs and returns highest CID.
     * This directly changes the object provided as a parameter, if necessary.
     */
    public long generateForNewObject() throws SchemaException {
        checkExistingContainers(object);
        LOGGER.trace("Generating " + pcvsWithoutId.size() + " missing container IDs for "
                + object.toDebugType() + "/" + object.getOid());
        assignMissingContainerIds();
        return maxUsedId;
    }

    /**
     * Initializes the generator for modify object and checks that no previous CIDs are missing.
     * This is a critical step before calling {@link #processModification}.
     */
    public ContainerValueIdGenerator forModifyObject(long containerIdSeq) throws SchemaException {
        checkExistingContainers(object);
        if (!pcvsWithoutId.isEmpty()) {
            LOGGER.warn("Generating missing container IDs in previously persisted prism object "
                    + object.toDebugType() + "/" + object.getOid() + " for container values: "
                    + pcvsWithoutId);
            assignMissingContainerIds();
        }
        if (containerIdSeq <= maxUsedId) {
            LOGGER.warn("Current CID sequence (" + containerIdSeq + ") is not above max used CID ("
                    + maxUsedId + ") for " + object.toDebugType() + "/" + object.getOid()
                    + ". CID sequence will be fixed, but it's suspicious!");
        }
        return this;
    }

    /**
     * This checks the modification for containers without assigned CIDs and adds them.
     * This changes values inside the modification and this *must be called before the modification
     * is applied to the prism object*.
     *
     * Theoretically, the changes may affect the prism object after the fact, but if any cloning
     * is involved this may not be true, so preferably use this *before* applying the modification.
     */
    public void processModification(ItemDelta<?, ?> modification) throws SchemaException {
        if (modification.isReplace()) {
            freeIdsFromReplacedContainer(modification);
        }
        if (modification.isAdd()) {
            identifyReplacedContainers(modification);
        }
        try {
            processModificationValues(modification.getValuesToAdd());
            processModificationValues(modification.getValuesToReplace());
            // values to delete are irrelevant
        } catch (DuplicateContainerIdException e) {
            throw new SchemaException("CID " + maxUsedId
                    + " is used repeatedly in the object: " + object);
        }
        assignMissingContainerIds();
    }

    private void freeIdsFromReplacedContainer(ItemDelta<?, ?> modification) {
        ItemDefinition<?> definition = modification.getDefinition();
        // we check all containers, even single-value container can contain multi-value ones
        if (definition instanceof PrismContainerDefinition<?>) {
            Visitable<?> container = object.findContainer(modification.getPath());
            if (container != null) {
                container.accept(visitable -> {
                    if (visitable instanceof PrismContainer
                            && !(visitable instanceof PrismObject)
                            && ((PrismContainer<?>) visitable).getDefinition().isMultiValue()) {
                        freeContainerIds((PrismContainer<?>) visitable);
                    }
                });
            }
        }
    }

    private void identifyReplacedContainers(ItemDelta<?, ?> modification) {
        ItemDefinition<?> definition = modification.getDefinition();
        // we check all containers, even single-value container can contain multi-value ones
        if (definition instanceof PrismContainerDefinition<?>) {
            for (PrismValue prismValue : modification.getValuesToAdd()) {
                //noinspection unchecked
                PrismContainerValue<Containerable> pcv = (PrismContainerValue<Containerable>) prismValue;
                PrismContainer<Containerable> container = object.findContainer(modification.getPath());
                if (container != null) {
                    PrismContainerValue<?> oldValue = container.findValue(pcv,
                            EquivalenceStrategy.REAL_VALUE_CONSIDER_DIFFERENT_IDS);
                    if (oldValue != null) {
                        Long cid = oldValue.getId();
                        overwrittenIds.add(cid);
                        usedIds.remove(cid); // technically, it's used, new PCV will take it again
                        pcv.setId(cid);
                    }
                }
            }
        }
    }

    private void freeContainerIds(PrismContainer<?> container) {
        for (PrismContainerValue<?> val : container.getValues()) {
            Long cid = val.getId();
            if (cid != null) {
                usedIds.remove(cid);
            }
        }
    }

    private void processModificationValues(Collection<? extends PrismValue> values)
            throws SchemaException {
        if (values != null) {
            for (PrismValue prismValue : values) {
                if (prismValue instanceof PrismContainerValue) {
                    PrismContainerValue<?> pcv = (PrismContainerValue<?>) prismValue;
                    // the top level value is not covered by checkExistingContainers()
                    if (pcv.getDefinition().isMultiValue()) {
                        processContainerValue(pcv);
                    }

                    checkExistingContainers(pcv);
                }
            }
        }
    }

    /**
     * Checks the provided container (possibly the whole object) and finds values requiring CID.
     * This does NOT cover top-level PCV if provided as parameter.
     */
    private void checkExistingContainers(Visitable<?> object) throws SchemaException {
        try {
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
        Long cid = val.getId();
        if (cid != null) {
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

    public boolean isOverwrittenId(Long id) {
        return overwrittenIds.contains(id);
    }

    // static single-value runtime exception to be thrown from lambdas (used in visitor)
    private static class DuplicateContainerIdException extends RuntimeException {
        static final DuplicateContainerIdException INSTANCE = new DuplicateContainerIdException();

        private DuplicateContainerIdException() {
            super(null, null, false, false);
        }
    }
}

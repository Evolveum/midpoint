/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schema.util.cid;

import java.util.*;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.equivalence.EquivalenceStrategy;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * Generator assigning missing IDs to PCVs of multi-value containers.
 *
 * Originally internal to "new repo" (sqale), now used also from the lens (clockwork) - when simulating the effect of deltas
 * on the objects during execution of operations in simulation mode.
 */
public class ContainerValueIdGenerator {

    private static final Trace LOGGER = TraceManager.getTrace(ContainerValueIdGenerator.class);

    private final PrismObject<? extends ObjectType> object;
    private final Set<Long> overwrittenIds = new HashSet<>(); // ids of PCV overwritten by ADD
    private final List<PrismContainerValue<?>> pcvsWithoutId = new ArrayList<>();

    private long maxUsedId = 0; // tracks max CID

    private int generated;

    public ContainerValueIdGenerator(@NotNull PrismObject<? extends ObjectType> object) {
        this.object = object;
    }

    /**
     * Method inserts IDs for prism container values without IDs and returns highest CID.
     * This directly changes the object provided as a parameter, if necessary.
     */
    public long generateForNewObject() {
        checkExistingContainers(object);
        LOGGER.trace("Generating {} missing container IDs for {}/{}",
                pcvsWithoutId.size(), object.toDebugType(), object.getOid());
        assignMissingContainerIds();
        return maxUsedId;
    }

    /**
     * Initializes the generator for modify object and checks that no previous CIDs are missing.
     * This is a critical step before calling {@link #processModification}.
     */
    public ContainerValueIdGenerator forModifyObject(long containerIdSeq) {
        checkExistingContainers(object);
        if (!pcvsWithoutId.isEmpty()) {
            LOGGER.warn("Generating missing container IDs in previously persisted prism object {}/{} for container values: {}",
                    object.toDebugType(), object.getOid(), pcvsWithoutId);
            assignMissingContainerIds();
        }
        if (containerIdSeq <= maxUsedId) {
            LOGGER.warn("Current CID sequence ({}) is not above max used CID ({}) for {}/{}. "
                    + "CID sequence will be fixed, but it's suspicious!",
                    containerIdSeq, maxUsedId, object.toDebugType(), object.getOid());
        } else {
            // TODO: Is this correct? containerIdSeq is used to reboot sequence of cids (eg. if
            //  cids are not in full object, but other items
            maxUsedId = containerIdSeq != 0 ? containerIdSeq - 1 : 0;
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
    public void processModification(ItemDelta<?, ?> modification) {
        if (modification.isReplace()) {
            freeIdsFromReplacedContainer(modification);
        }
        if (modification.isAdd()) {
            identifyReplacedContainers(modification);
        }
        processModificationValues(modification.getValuesToAdd());
        processModificationValues(modification.getValuesToReplace());
        // values to delete are irrelevant
        assignMissingContainerIds();
    }

    /** Currently does nothing. Consider removing. */
    private void freeIdsFromReplacedContainer(ItemDelta<?, ?> modification) {
        ItemDefinition<?> definition = modification.getDefinition();
        // we check all containers, even single-value container can contain multi-value ones
        if (definition instanceof PrismContainerDefinition<?>) {
            Visitable container = object.findContainer(modification.getPath());
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
                        pcv.setId(cid);
                    }
                }
            }
        }
    }

    private void freeContainerIds(PrismContainer<?> container) {
    }

    private void processModificationValues(Collection<? extends PrismValue> values) {
        if (values != null) {
            for (PrismValue prismValue : values) {
                if (prismValue instanceof PrismContainerValue<?> pcv) {
                    // the top level value is not covered by checkExistingContainers()
                    // FIXME: How to process here?
                    if (pcv.getDefinition().isMultiValue()) {
                        processContainerValue(pcv, new HashSet<>());
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
    private void checkExistingContainers(Visitable object) {
        object.accept(visitable -> {
            if (visitable instanceof PrismContainer
                    && !(visitable instanceof PrismObject)
                    && ((PrismContainer<?>) visitable).getDefinition().isMultiValue()) {
                processContainer((PrismContainer<?>) visitable);
            }
        });
    }

    private void processContainer(PrismContainer<?> container) {
        Set<Long> usedIds = new HashSet<>();
        for (PrismContainerValue<?> val : container.getValues()) {
            processContainerValue(val, usedIds);
        }
    }

    private void processContainerValue(PrismContainerValue<?> val, Set<Long> usedIds) {
        Long cid = val.getId();
        if (cid != null) {
            usedIds.add(cid);
            maxUsedId = Math.max(maxUsedId, cid);
        } else {
            pcvsWithoutId.add(val);
        }
    }

    /** Generates container IDs for {@link #pcvsWithoutId} and clears the list. */
    private void assignMissingContainerIds() {
        for (PrismContainerValue<?> val : pcvsWithoutId) {
            val.setId(nextId());
            generated++;
        }
        pcvsWithoutId.clear();
    }

    private long nextId() {
        maxUsedId++;
        return maxUsedId;
    }

    public int getGenerated() {
        return generated;
    }

    public long lastUsedId() {
        return maxUsedId;
    }

    public boolean isOverwrittenId(Long id) {
        return overwrittenIds.contains(id);
    }
}

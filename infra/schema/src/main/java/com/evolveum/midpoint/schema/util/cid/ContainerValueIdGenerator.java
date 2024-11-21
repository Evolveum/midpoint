/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schema.util.cid;

import java.util.*;

import com.evolveum.midpoint.prism.impl.lazy.LazyPrismContainerValue;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;

import jakarta.xml.bind.JAXBElement;
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

    /** IDs of PCVs overwritten by item ADD VALUE deltas. */
    private final Set<Long> overwrittenIds = new HashSet<>();

    /** PCVs without ID; to be generated in {@link #assignMissingContainerIds()}. */
    private final List<PrismContainerValue<?>> pcvsWithoutId = new ArrayList<>();

    /** Tracks max CID: either existing or generated. */
    private long maxUsedId = 0;

    private int generated;

    public ContainerValueIdGenerator(@NotNull PrismObject<? extends ObjectType> object) {
        this.object = object;
    }

    /**
     * Method inserts IDs for prism container values without IDs and returns highest CID.
     * This directly changes the object provided as a parameter, if necessary.
     */
    public long generateForNewObject() {
        checkValueDeeply(object.getValue());
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
        checkValueDeeply(object.getValue());
        if (!pcvsWithoutId.isEmpty()) {
            LOGGER.debug("Generating missing container IDs in previously persisted prism object {}/{} for container values: {}",
                    object.toDebugType(), object.getOid(), pcvsWithoutId);
            assignMissingContainerIds();
        }
        if (containerIdSeq <= maxUsedId) {
            LOGGER.debug("Current CID sequence ({}) is not above max used CID ({}) for {}/{}. "
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
        if (modification.isAdd()) {
            identifyReplacedContainers(modification);
        }
        for (var prismValue : modification.getNewValues()) {
            checkValueDeeply(prismValue);
        }
        // values to delete are irrelevant
        assignMissingContainerIds();
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

    /** Checks given {@link PrismValue} for missing PCV IDs (deeply). */
    private void checkValueDeeply(PrismValue rootValue) {
        rootValue.acceptVisitor(visitable -> {
            if (visitable instanceof PrismContainerValue<?> pcv) {
                var def = pcv.getDefinition();
                if (def != null && def.isMultiValue()) {
                    checkPcvId(pcv); // prism objects are OK here, they are not multivalued
                }
                if (LazyPrismContainerValue.isNotMaterialized(pcv)) {
                    return false;
                }

            } else if (visitable instanceof PrismPropertyValue<?> ppv
                && ppv.getRealValue() instanceof ExpressionType expression) {
                for (JAXBElement<?> evaluatorElement : expression.getExpressionEvaluator()) {
                    if (evaluatorElement.getValue() instanceof Containerable containerableEvaluator) {
                        // Visitor does not go deeper into the evaluator, so we need to do it here manually
                        checkValueDeeply(containerableEvaluator.asPrismContainerValue());
                    }
                }
            }
            return true;
            // The value metadata will be visited automatically
        });
    }

    /** Checks a PCV for missing IDs. */
    private void checkPcvId(PrismContainerValue<?> val) {
        Long cid = val.getId();
        if (cid != null) {
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

    public long nextId() {
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

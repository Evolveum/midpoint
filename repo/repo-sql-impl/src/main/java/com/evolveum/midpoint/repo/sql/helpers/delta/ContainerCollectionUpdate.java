/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.helpers.delta;

import java.util.Collection;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.equivalence.EquivalenceStrategy;
import com.evolveum.midpoint.repo.sql.data.common.container.Container;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * Update operation on collection of containers.
 * The difference from ordinary collection is in handling value IDs.
 */
class ContainerCollectionUpdate<RC extends Container, C extends Containerable>
        extends CollectionUpdate<RC, PrismContainerValue<C>, PrismContainer<C>, ContainerDelta<C>> {

    private static final Trace LOGGER = TraceManager.getTrace(ContainerCollectionUpdate.class);

    ContainerCollectionUpdate(Collection<RC> targetCollection, Object collectionOwner,
            PrismObject<? extends ObjectType> prismObject, ContainerDelta<C> delta, Class<RC> attributeValueType,
            UpdateContext ctx) {
        super(targetCollection, collectionOwner, prismObject, delta, attributeValueType, ctx);
    }

    @Override
    PrismContainerValue<C> findExistingValue(PrismContainerValue<C> value) {
        if (existingItem == null) {
            return null;
        }
        if (value.getId() != null) {
            PrismContainerValue<C> byId = existingItem.findValue(value.getId());
            if (byId != null) {
                return byId;
            }
        }
        return existingItem.findValue(value, EquivalenceStrategy.REAL_VALUE_CONSIDER_DIFFERENT_IDS);
    }

    @Override
    RC adaptValueBeforeAddition(RC repoValueToAdd, PrismContainerValue<C> valueToAdd, PrismContainerValue<C> existingValue) {
        setIdIfMissing(repoValueToAdd, valueToAdd, existingValue);
        return super.adaptValueBeforeAddition(repoValueToAdd, valueToAdd, existingValue);
    }

    private void setIdIfMissing(RC repoValueToAdd, PrismContainerValue<C> valueToAdd, PrismContainerValue<C> existingValue) {
        if (repoValueToAdd.getId() != null) {
            LOGGER.trace("Value being added already has an ID ({}): {} / {}", repoValueToAdd.getId(), valueToAdd, repoValueToAdd);
        } else if (valueToAdd.getId() != null) {
            LOGGER.warn("Value to add has an ID but repo value to add has not. Please fix the code. Values: {} / {}", valueToAdd, repoValueToAdd);
            repoValueToAdd.setId(valueToAdd.getId().intValue());
        } else if (existingValue != null) {
            if (existingValue.getId() == null) {
                throw new IllegalStateException("Existing value " + existingValue + " has no ID in the repository. Please run the reindex task.");
            }
            long id = existingValue.getId();
            valueToAdd.setId(id);
            repoValueToAdd.setId((int) id);
            LOGGER.trace("Used ID of the existing value ({}) for the value being added: {} / {}", id, valueToAdd, repoValueToAdd);
        } else {
            long nextId = ctx.idGenerator.nextId();
            valueToAdd.setId(nextId);
            repoValueToAdd.setId((int) nextId);
            LOGGER.trace("Used newly-generated ID {} for the value being added: {} / {}", nextId, valueToAdd, repoValueToAdd);
        }
    }
}

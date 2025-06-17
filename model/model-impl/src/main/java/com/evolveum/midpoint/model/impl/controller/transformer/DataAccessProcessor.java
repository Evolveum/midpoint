/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.controller.transformer;

import com.evolveum.midpoint.model.impl.controller.SchemaTransformer;
import com.evolveum.midpoint.model.impl.lens.LensElementContext;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.equivalence.EquivalenceStrategy;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.AccessDecision;
import com.evolveum.midpoint.security.api.SecurityUtil;
import com.evolveum.midpoint.security.enforcer.api.PrismEntityOpConstraints;
import com.evolveum.midpoint.util.exception.AuthorizationException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import com.google.common.base.Preconditions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import static com.evolveum.midpoint.util.MiscUtil.stateCheck;

/**
 * Parts of {@link SchemaTransformer} devoted to applying read constraints to objects and deltas
 * (by removing invisible items and values).
 */
@Component
public class DataAccessProcessor {

    /** Using this logger for compatibility reasons. */
    private static final Trace LOGGER = TraceManager.getTrace(SchemaTransformer.class);

    public <O extends ObjectType> PrismObject<O> applyReadConstraints(
            @NotNull PrismObject<O> object, @NotNull PrismEntityOpConstraints.ForValueContent constraints)
            throws SecurityViolationException {
        return applyReadConstraints(object.getValue(), constraints)
                .asPrismObject();
    }

    private <V extends PrismValue> V applyReadConstraints(
            @NotNull V value, @NotNull PrismEntityOpConstraints.ForValueContent constraints)
            throws SecurityViolationException {

        AccessDecision decision = constraints.getDecision();
        if (decision == AccessDecision.ALLOW) {
            return value;
        } else if (decision == AccessDecision.DENY || !(value instanceof PrismContainerValue<?>)) {
            SecurityUtil.logSecurityDeny(value, "because the authorization denies access");
            throw new AuthorizationException("Access denied");
        } else {
            assert decision == AccessDecision.DEFAULT;
            var mutable = (PrismContainerValue<?>) value.cloneIfImmutable();
            applyReadConstraintsToMutablePcv(mutable, constraints);
            if (mutable.isEmpty()) {
                // let's make it explicit (note that the log message may show empty object if it was originally mutable)
                // TODO decide if this is really correct approach
                SecurityUtil.logSecurityDeny(value, "because the subject has no access to any item");
                throw new AuthorizationException("Access denied");
            }
            //noinspection unchecked
            return (V) mutable;
        }
    }

    private void applyReadConstraintsToMutablePcv(
            @NotNull PrismContainerValue<?> pcv,
            @NotNull PrismEntityOpConstraints.ForValueContent pcvConstraints) {
        Collection<Item<?, ?>> items = pcv.getItems();
        LOGGER.trace("applyReadConstraintsToMutablePcv: items={}", items);
        if (items.isEmpty()) {
            return;
        }
        List<Item<?, ?>> itemsToRemove = new ArrayList<>();
        for (Item<?, ?> item : items) {
            var itemConstraints = pcvConstraints.getItemConstraints(item.getElementName());
            AccessDecision itemDecision = itemConstraints.getDecision();
            if (itemDecision == AccessDecision.ALLOW) {
                // OK, keeping it untouched
            } else if (itemDecision == AccessDecision.DENY) {
                itemsToRemove.add(item);
            } else {
                assert itemDecision == AccessDecision.DEFAULT;
                applyReadConstraintsToMutableValues(item, itemConstraints);
            }
        }
        for (Item<?, ?> itemToRemove : itemsToRemove) {
            pcv.remove(itemToRemove);
        }
    }

    private <V extends PrismValue, D extends ItemDefinition<?>> void applyReadConstraintsToMutableValues(
            Item<V, D> item, @NotNull PrismEntityOpConstraints.ForItemContent readConstraints) {
        List<V> valuesToRemove = new ArrayList<>();
        for (V value : item.getValues()) {
            var valueConstraints = readConstraints.getValueConstraints(value);
            AccessDecision valueDecision = valueConstraints.getDecision();
            if (valueDecision == AccessDecision.ALLOW) {
                // OK, keeping it untouched
            } else if (valueDecision == AccessDecision.DENY) {
                valuesToRemove.add(value);
            } else {
                assert valueDecision == AccessDecision.DEFAULT;
                if (value instanceof PrismContainerValue<?>) {
                    applyReadConstraintsToMutablePcv((PrismContainerValue<?>) value, valueConstraints);
                } else {
                    valuesToRemove.add(value);
                }
            }
        }
        if (!valuesToRemove.isEmpty()) {
            if (valuesToRemove.size() == item.size()) {
                item.clear();
            } else {
                item.removeAll(valuesToRemove, EquivalenceStrategy.LITERAL);
            }
        }
    }

    /** Returns `false` if the context as a whole has access denied. */
    public <O extends ObjectType> boolean applyReadConstraints(
            LensElementContext<O> elementContext, PrismEntityOpConstraints.ForValueContent readConstraints) {
        var decision = readConstraints.getDecision();
        if (decision == AccessDecision.ALLOW) {
            return true;
        } else if (decision == AccessDecision.DENY) {
            return false;
        } else {
            assert decision == AccessDecision.DEFAULT;
            elementContext.forEachObject(object ->
                    applyReadConstraintsToMutableValue(object.getValue(), readConstraints));
            elementContext.forEachDelta(delta ->
                    applyReadConstraintsToDelta(delta, readConstraints));
            return true;
        }
    }

    private void applyReadConstraintsToMutableValue(
            @NotNull PrismValue value, @NotNull PrismEntityOpConstraints.ForValueContent constraints) {
        Preconditions.checkArgument(!value.isImmutable(), "Value is not mutable: %s", value);
        PrismValue after;
        try {
            after = applyReadConstraints(value, constraints);
        } catch (SecurityViolationException e) {
            throw SystemException.unexpected(e); // TODO
        }
        stateCheck(after == value, "Value's identity was changed. Why? %s", after);
    }

    private <O extends ObjectType> void applyReadConstraintsToDelta(
            @Nullable ObjectDelta<O> objectDelta,
            @NotNull PrismEntityOpConstraints.ForValueContent readConstraints) {
        if (objectDelta == null) {
            return;
        }
        if (objectDelta.isAdd()) {
            applyReadConstraintsToMutableValue(objectDelta.getObjectToAdd().getValue(), readConstraints);
            return;
        }
        if (objectDelta.isDelete()) {
            // Nothing to do
            return;
        }
        // Modify delta
        Collection<? extends ItemDelta<?,?>> modifications = objectDelta.getModifications();
        if (modifications.isEmpty()) {
            // Nothing to do
            return;
        }
        List<ItemDelta<?, ?>> modificationsToRemove = new ArrayList<>();
        for (ItemDelta<?, ?> modification : modifications) {
            ItemPath itemPath = modification.getPath();
            var itemConstraints = readConstraints.getValueConstraints(itemPath.namedSegmentsOnly());

            AccessDecision modReadDecision = itemConstraints.getDecision();
            LOGGER.trace("applyReadConstraintsToDelta(item): {}: decision R={}", itemPath, modReadDecision);
            if (modReadDecision == AccessDecision.DENY) {
                // Explicitly denied access to the entire modification
                modificationsToRemove.add(modification);
            } else if (modReadDecision == AccessDecision.ALLOW) {
                // do not touch
            } else {
                reduceValues(modification.getValuesToAdd(), readConstraints);
                reduceValues(modification.getValuesToDelete(), readConstraints);
                reduceValues(modification.getValuesToReplace(), readConstraints);
                reduceValues(modification.getEstimatedOldValues(), readConstraints);
                if (modification.isEmpty()) {
                    // We have removed all the content, if there was any. So, in the default case, there's nothing that
                    // we are interested in inside this item. Therefore let's just remove it.
                    modificationsToRemove.add(modification);
                }
            }
        }
        for (ItemDelta<?, ?> modificationToRemove : modificationsToRemove) {
            modifications.remove(modificationToRemove);
        }
    }

    private void reduceValues(
            @Nullable Collection<? extends PrismValue> values,
            @NotNull PrismEntityOpConstraints.ForValueContent readConstraints) {
        if (values == null) {
            return;
        }
        Iterator<? extends PrismValue> vi = values.iterator();
        while (vi.hasNext()) {
            PrismValue val = vi.next();
            AccessDecision decision = readConstraints.getDecision();
            if (decision == AccessDecision.ALLOW) {
                // continue
            } else if (decision == AccessDecision.DENY) {
                vi.remove();
            } else {
                applyReadConstraintsToMutableValue(val, readConstraints);
                boolean valIsEmpty = val instanceof PrismContainerValue<?> pcv ? pcv.hasNoItems() : val.isEmpty();
                if (valIsEmpty) {
                    // We have removed all the content, if there was any. So, in the default case, there's nothing that
                    // we are interested in inside this PCV. Therefore let's just remove it.
                    // (If itemReadDecision is ALLOW, we obviously keep this untouched.)
                    vi.remove();
                }
            }
        }
    }
}

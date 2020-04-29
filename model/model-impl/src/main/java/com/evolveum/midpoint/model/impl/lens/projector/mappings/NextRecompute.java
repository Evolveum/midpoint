/*
 * Copyright (c) 2018-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.projector.mappings;

import com.evolveum.midpoint.model.common.mapping.MappingImpl;
import com.evolveum.midpoint.model.impl.lens.LensElementContext;
import com.evolveum.midpoint.model.impl.lens.LensFocusContext;
import com.evolveum.midpoint.model.impl.trigger.RecomputeTriggerHandler;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TriggerType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.XMLGregorianCalendar;

/**
 *  Next planned recompute event.
 *  Besides holding data on that this class provides some methods to manipulate it.
 *  (Seemed as a good place for this functionality, at least until mappings are refactored to their final form.)
 */
public class NextRecompute {

    private static final Trace LOGGER = TraceManager.getTrace(NextRecompute.class);

    @NotNull private final XMLGregorianCalendar nextRecomputeTime;
    @Nullable private final String triggerOriginDescription;

    NextRecompute(@NotNull XMLGregorianCalendar nextRecomputeTime, @Nullable String triggerOriginDescription) {
        this.nextRecomputeTime = nextRecomputeTime;
        this.triggerOriginDescription = triggerOriginDescription;
    }

    public <F extends AssignmentHolderType> void createTrigger(LensFocusContext<F> focusContext) throws SchemaException {
        createTrigger(focusContext.getObjectCurrent(), focusContext.getObjectDefinition(), focusContext);
    }

    <V extends PrismValue, D extends ItemDefinition, T extends ObjectType, F extends FocusType> void createTrigger(
            PrismObject<T> targetObject, PrismObjectDefinition<T> targetObjectDefinition, LensElementContext<T> targetContext) throws SchemaException {
        if (targetObject != null) {
            for (TriggerType trigger: targetObject.asObjectable().getTrigger()) {
                if (RecomputeTriggerHandler.HANDLER_URI.equals(trigger.getHandlerUri()) &&
                        nextRecomputeTime.equals(trigger.getTimestamp())) {
                    return;
                }
            }
        }

        PrismContainerDefinition<TriggerType> triggerContDef = targetObjectDefinition.findContainerDefinition(ObjectType.F_TRIGGER);
        ContainerDelta<TriggerType> triggerDelta = triggerContDef.createEmptyDelta(ObjectType.F_TRIGGER);
        PrismContainerValue<TriggerType> triggerCVal = triggerContDef.createValue();
        triggerDelta.addValueToAdd(triggerCVal);
        TriggerType triggerType = triggerCVal.asContainerable();
        triggerType.setTimestamp(nextRecomputeTime);
        triggerType.setHandlerUri(RecomputeTriggerHandler.HANDLER_URI);
        triggerType.setOriginDescription(triggerOriginDescription);

        targetContext.swallowToSecondaryDelta(triggerDelta);
    }

    public static <V extends PrismValue, D extends ItemDefinition> NextRecompute update(MappingImpl<V, D> mapping, NextRecompute existing) {
        XMLGregorianCalendar mappingNextRecomputeTime = mapping.getNextRecomputeTime();
        LOGGER.trace("Evaluation of mapping {} delayed to {}", mapping, mappingNextRecomputeTime);
        if (mappingNextRecomputeTime != null && (existing == null || existing.nextRecomputeTime.compare(mappingNextRecomputeTime) == DatatypeConstants.GREATER)) {
            return new NextRecompute(mappingNextRecomputeTime, mapping.getIdentifier());
        } else {
            return existing;
        }
    }

    public static <V extends PrismValue, D extends ItemDefinition> NextRecompute update(NextRecompute mappingNextRecompute, NextRecompute existing) {
        if (mappingNextRecompute != null && (existing == null || existing.nextRecomputeTime.compare(mappingNextRecompute.nextRecomputeTime) == DatatypeConstants.GREATER)) {
            return mappingNextRecompute;
        } else {
            return existing;
        }
    }
}

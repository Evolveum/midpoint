/*
 * Copyright (c) 2018-2019 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
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
 * Represents a future recompute event and provides helpers for creating a recompute trigger on an object.
 *
 * This class is used by activation and mapping evaluation to schedule a later recomputation without executing
 * the full change immediately. The trigger is stored on the object and later processed by the recompute trigger
 * handler.
 */
public class NextRecompute {

    private static final Trace LOGGER = TraceManager.getTrace(NextRecompute.class);

    @NotNull private final XMLGregorianCalendar nextRecomputeTime;
    @Nullable private final String triggerOriginDescription;

    /**
     * Creates a future recompute plan.
     *
     * @param nextRecomputeTime the time when recomputation should happen
     * @param triggerOriginDescription optional description of the rule or mapping that requested the trigger
     */
    public NextRecompute(@NotNull XMLGregorianCalendar nextRecomputeTime, @Nullable String triggerOriginDescription) {
        this.nextRecomputeTime = nextRecomputeTime;
        this.triggerOriginDescription = triggerOriginDescription;
    }

    /**
     * Creates or updates a recompute trigger on the given focus context
     * (by creating the secondary delta, NOT executing the operation immediately!)
     */
    public <F extends AssignmentHolderType> void createTrigger(LensFocusContext<F> focusContext) throws SchemaException {
        createTrigger(focusContext.getObjectCurrent(), focusContext.getObjectDefinition(), focusContext);
    }

    /**
     * Creates or updates a recompute trigger on the given object
     * (by creating the secondary delta, NOT executing the operation immediately!)
     *
     * @param targetObject object that should receive the trigger
     * @param targetObjectDefinition definition of the target object type
     * @param targetContext lens context used to record the secondary delta
     */
    public <V extends PrismValue, D extends ItemDefinition, T extends ObjectType, F extends FocusType> void createTrigger(
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

    /**
     * Updates an existing recompute plan from a mapping's next-recompute time - selecting the earlier of the two times.
     */
    public static NextRecompute update(MappingImpl<?, ?> mapping, NextRecompute existing) {
        XMLGregorianCalendar mappingNextRecomputeTime = mapping.getNextRecomputeTime();
        LOGGER.trace("Evaluation of mapping {} delayed to {}", mapping, mappingNextRecomputeTime);
        if (mappingNextRecomputeTime != null
                && (existing == null || existing.nextRecomputeTime.compare(mappingNextRecomputeTime) == DatatypeConstants.GREATER)) {
            return new NextRecompute(mappingNextRecomputeTime, mapping.getIdentifier());
        } else {
            return existing;
        }
    }

    /**
     * Updates an existing recompute plan with the earliest of the current and new timestamps.
     */
    public static <V extends PrismValue, D extends ItemDefinition> NextRecompute update(
            NextRecompute mappingNextRecompute, NextRecompute existing) {
        if (mappingNextRecompute != null
                && (existing == null || existing.nextRecomputeTime.compare(mappingNextRecompute.nextRecomputeTime) == DatatypeConstants.GREATER)) {
            return mappingNextRecompute;
        } else {
            return existing;
        }
    }
}

/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.impl.lens.projector.mappings.predefinedActivationMapping;

import java.io.Serializable;
import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;

import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.util.ItemDeltaItem;
import com.evolveum.midpoint.prism.util.ObjectDeltaObject;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * Evaluates whether a time-based activation constraint has become valid or invalid.
 *
 * The helper combines a reference timestamp from a shadow or focus item with a (usually configured) offset and computes
 * whether the constraint is already active, still pending, or has already expired. It is used by predefined activation
 * mappings to decide if the computation should take place or when a future recompute should happen.
 */
class TimeConstraintEvaluation implements Serializable {

    private static final Trace LOGGER = TraceManager.getTrace(TimeConstraintEvaluation.class);

    /** Path to the reference time item (e.g. `activation/disableTimestamp`) */
    private final ItemPath referenceTimePath;

    /** Offset added to the reference timestamp to determine the time boundary (e.g. 7 days before final account deletion). */
    private final Duration timeOffset;

    /**
     * Is the time constraint valid regarding specified {@link #referenceTimePath} (in object provided later)
     * and {@link #timeOffset}? The answer depends whether we understand the constraint as `validFrom` or `validTo`,
     * see the evaluation methods.
     *
     * {@code null} if the evaluation was not done yet.
     *
     * @see #evaluateAsValidFrom(ObjectDeltaObject, XMLGregorianCalendar)
     * @see #evaluateAsValidTo(ObjectDeltaObject, XMLGregorianCalendar)
     */
    private Boolean timeConstraintValid;

    /**
     * If the time constraints indicate that the validity will change in the future
     * (either it becomes valid or becomes invalid), this is the time of the expected change.
     */
    private XMLGregorianCalendar nextRecomputeTime;

    /**
     * Creates a time-constraint evaluator for a reference item and a duration offset.
     *
     * @param referenceTimePath path of the reference timestamp item
     * @param timeOffset offset added to the reference timestamp
     */
    TimeConstraintEvaluation(ItemPath referenceTimePath, Duration timeOffset) {
        this.referenceTimePath = referenceTimePath;
        this.timeOffset = timeOffset;
    }

    /**
     * Evaluates whether we are _after_ the reference time plus offset.
     * I.e. the constraint is understood as "valid from X" where X = {@link #referenceTimePath} plus {@link #timeOffset}.
     *
     * @param parentOdo object containing the reference timestamp (driven by {@link #referenceTimePath})
     * @param now current time used for evaluation
     *
     * @see #evaluateAsValidTo(ObjectDeltaObject, XMLGregorianCalendar)
     */
    void evaluateAsValidFrom(ObjectDeltaObject<?> parentOdo, XMLGregorianCalendar now) throws SchemaException {
        if (parentOdo == null || referenceTimePath == null) {
            timeConstraintValid = true;
            return;
        }

        XMLGregorianCalendar validFrom = getReferenceTimePlusOffset(parentOdo);

        if (validFrom == null) {
            // Time is specified but there is no value for it.
            // This means that event that determines (starts) the validity haven't happened yet - therefore the mapping
            // is not yet valid.
            timeConstraintValid = false;
            return;
        }

        if (validFrom.compare(now) == DatatypeConstants.GREATER) {
            // we are before validFrom -> not valid
            nextRecomputeTime = validFrom;
            timeConstraintValid = false;
            return;
        }

        // We are after validFrom -> valid
        timeConstraintValid = true;
    }

    /**
     * Evaluates whether we are _before_ the reference time plus offset.
     * I.e. the constraint is understood as "valid to X" where X = {@link #referenceTimePath} plus {@link #timeOffset}.
     *
     * @param parentOdo object containing the reference timestamp (driven by {@link #referenceTimePath})
     * @param now current time used for evaluation
     *
     * @see #evaluateAsValidFrom(ObjectDeltaObject, XMLGregorianCalendar)
     */
    void evaluateAsValidTo(ObjectDeltaObject<?> parentOdo, XMLGregorianCalendar now) throws SchemaException {
        if (parentOdo == null || referenceTimePath == null) {
            timeConstraintValid = true;
            return;
        }

        XMLGregorianCalendar validTo = getReferenceTimePlusOffset(parentOdo);

        if (validTo == null) {
            // Time is specified but there is no value for it.
            // This means that event that determines (ends) the validity haven't happened yet - therefore the mapping is still
            // valid.
            timeConstraintValid = true;
            return;
        }

        if (validTo.compare(now) == DatatypeConstants.GREATER) {
            // we are before validTo -> valid
            nextRecomputeTime = validTo;
            timeConstraintValid = true;
            return;
        }

        // we are after validTo -> invalid
        timeConstraintValid = false;
    }

    private XMLGregorianCalendar getReferenceTimePlusOffset(ObjectDeltaObject<?> parentOdo) throws SchemaException {
        XMLGregorianCalendar referenceTime = getReferenceTime(parentOdo);
        LOGGER.trace("reference time = {}", referenceTime);

        if (referenceTime == null) {
            return null;
        }

        XMLGregorianCalendar time = (XMLGregorianCalendar) referenceTime.clone();

        if (timeOffset != null) {
            time.add(timeOffset);
        }
        LOGGER.trace("Offset {} applied; time = {}", timeOffset, time);
        return time;
    }

    private XMLGregorianCalendar getReferenceTime(ObjectDeltaObject<?> parentOdo) throws SchemaException {
        LOGGER.trace("parseTimeSource: path = {}, source object = {}", referenceTimePath, parentOdo);

        ItemDeltaItem<?, ?> sourceObject = parentOdo.findIdi(referenceTimePath);

        if (sourceObject == null) {
            return null;
        }
        //noinspection unchecked
        PrismProperty<XMLGregorianCalendar> timeProperty = (PrismProperty<XMLGregorianCalendar>) sourceObject.getItemNew();
        return timeProperty != null ? timeProperty.getRealValue() : null;
    }

    /**
     * Returns whether the time evaluation has already produced a result.
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    boolean isTimeValidityEstablished() {
        if (timeConstraintValid == null) {
            LOGGER.trace("Time validity has not been established");
            return false;
        } else {
            return true;
        }
    }

    /**
     * Returns the computed validity state of the constraint. Fails if it was not established yet.
     *
     * @return {@code true} if the mapping with this time constraint is active for the current time
     */
    boolean isTimeConstraintValid() {
        return MiscUtil.stateNonNull(timeConstraintValid, "Time validity has not been established");
    }

    /**
     * Returns the timestamp when the validity of this time constraint is expected to change next.
     * We should recompute the object at this time.
     */
    XMLGregorianCalendar getNextRecomputeTime() {
        return nextRecomputeTime;
    }
}

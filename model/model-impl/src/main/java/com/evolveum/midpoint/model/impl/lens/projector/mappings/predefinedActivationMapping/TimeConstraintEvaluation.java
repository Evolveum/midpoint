/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.projector.mappings.predefinedActivationMapping;

import java.io.Serializable;
import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.util.ItemDeltaItem;
import com.evolveum.midpoint.prism.util.ObjectDeltaObject;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Evaluates time from offset and path to reference time item.
 */
class TimeConstraintEvaluation implements Serializable {

    private static final Trace LOGGER = TraceManager.getTrace(TimeConstraintEvaluation.class);

    /**
     * path to reference time item
     */
    private final ItemPath path;
    private final Duration offset;

    /**
     * Is the time valid regarding specified offset and path to reference time item
     * (If no constraint is provided, time.)
     */
    private Boolean timeConstraintValid;

    /**
     * If the time constraints indicate that the validity will change in the future
     * (either it becomes valid or becomes invalid), this is the time of the expected change.
     */
    private XMLGregorianCalendar nextRecomputeTime;

    TimeConstraintEvaluation(ItemPath path, Duration offset) {
        this.path = path;
        this.offset = offset;
    }

    void evaluateFrom(ObjectDeltaObject parentOdo, XMLGregorianCalendar now) throws SchemaException {
        if (parentOdo == null || path == null) {
            timeConstraintValid = true;
            return;
        }

        XMLGregorianCalendar timeFrom = parseTime(parentOdo);

        if (timeFrom == null) {
            // Time is specified but there is no value for it.
            // This means that event that should start validity haven't happened yet
            // therefore the mapping is not yet valid.
            timeConstraintValid = false;
            return;
        }

        if (timeFrom != null && timeFrom.compare(now) == DatatypeConstants.GREATER) {
            // before timeFrom
            nextRecomputeTime = timeFrom;
            timeConstraintValid = false;
            return;
        }

        // Otherwise it is less than now (so we are after it)
        timeConstraintValid = true;
    }

    void evaluateTo(ObjectDeltaObject parentOdo, XMLGregorianCalendar now) throws SchemaException {
        if (parentOdo == null || path == null) {
            timeConstraintValid = true;
            return;
        }

        XMLGregorianCalendar timeTo = parseTime(parentOdo);

        if (timeTo == null) {
            // Time is specified but there is no value for it.
            // This means that event that should stop validity haven't happened yet
            // therefore the mapping is still valid.
            timeConstraintValid = true;
            return;
        }

        if (timeTo.compare(now) == DatatypeConstants.GREATER) {
            // between timeFrom and timeTo (also no timeFrom and before timeTo)
            nextRecomputeTime = timeTo;
            timeConstraintValid = true;
            return;
        }

        // Otherwise it is less than now (so we are after it), we are "out of range"
        timeConstraintValid = false;
    }

    private XMLGregorianCalendar parseTime(ObjectDeltaObject parentOdo) throws SchemaException {
        XMLGregorianCalendar referenceTime = parseTimeSource(parentOdo);
        LOGGER.trace("reference time = {}", referenceTime);

        if (referenceTime == null) {
            return null;
        }

        XMLGregorianCalendar time = (XMLGregorianCalendar) referenceTime.clone();

        if (offset != null) {
            time.add(offset);
        }
        LOGGER.trace("Offset {} applied; time = {}", offset, time);
        return time;
    }

    private XMLGregorianCalendar parseTimeSource(ObjectDeltaObject parentOdo) throws SchemaException {
        LOGGER.trace("parseTimeSource: path = {}, source object = {}", path, parentOdo);

        ItemDeltaItem<?, ?> sourceObject = parentOdo.findIdi(path);

        if (sourceObject == null) {
            return null;
        }
        //noinspection unchecked
        PrismProperty<XMLGregorianCalendar> timeProperty = (PrismProperty<XMLGregorianCalendar>) sourceObject.getItemNew();
        return timeProperty != null ? timeProperty.getRealValue() : null;
    }

    @Nullable Boolean isTimeConstraintValid() {
        if (timeConstraintValid == null) {
            LOGGER.trace("Time validity has not been established");
        }
        return timeConstraintValid;
    }

    XMLGregorianCalendar getNextRecomputeTime() {
        return nextRecomputeTime;
    }
}

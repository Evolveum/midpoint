/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.common.mapping;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.util.ItemDeltaItem;
import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;
import com.evolveum.midpoint.repo.common.expression.ExpressionVariables;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingTimeDeclarationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.VariableBindingDefinitionType;

import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;
import java.util.Objects;

/**
 * Evaluates mapping time constraints.
 */
class TimeConstraintsEvaluation {

    /**
     * "Parent" mapping evaluation.
     */
    private final AbstractMappingImpl<?, ?, ?> m;

    /**
     * Is the mapping valid regarding specified time constraint (timeFrom, timeTo)?
     * (If no constraint is provided, mapping is considered valid.)
     */
    private Boolean timeConstraintValid;

    /**
     * If the time constraints indicate that the validity of the mapping will change in the future
     * (either it becomes valid or becomes invalid), this is the time of the expected change.
     */
    private XMLGregorianCalendar nextRecomputeTime;

    TimeConstraintsEvaluation(AbstractMappingImpl<?, ?, ?> m) {
        this.m = m;
    }

    void evaluate(OperationResult result) throws SchemaException, ObjectNotFoundException, CommunicationException,
            ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
        MappingTimeDeclarationType timeFromSpec = m.mappingBean.getTimeFrom();
        MappingTimeDeclarationType timeToSpec = m.mappingBean.getTimeTo();
        if (timeFromSpec == null && timeToSpec == null) {
            timeConstraintValid = true;
            return;
        }

        XMLGregorianCalendar timeFrom = parseTime(timeFromSpec, result);
        m.traceTimeFrom(timeFrom);

        if (timeFrom == null && timeFromSpec != null) {
            // Time is specified but there is no value for it.
            // This means that event that should start validity haven't happened yet
            // therefore the mapping is not yet valid.
            timeConstraintValid = false;
            return;
        }
        XMLGregorianCalendar timeTo = parseTime(timeToSpec, result);
        m.traceTimeTo(timeTo);

        if (timeFrom != null && timeFrom.compare(m.now) == DatatypeConstants.GREATER) {
            // before timeFrom
            nextRecomputeTime = timeFrom;
            timeConstraintValid = false;
            return;
        }

        if (timeTo == null && timeToSpec != null) {
            // Time is specified but there is no value for it.
            // This means that event that should stop validity haven't happened yet
            // therefore the mapping is still valid.
            timeConstraintValid = true;
            return;
        }

        if (timeTo != null && timeTo.compare(m.now) == DatatypeConstants.GREATER) {
            // between timeFrom and timeTo (also no timeFrom and before timeTo)
            nextRecomputeTime = timeTo;
            timeConstraintValid = true;
            return;
        }

        // If timeTo is null, we are "in range"
        // Otherwise it is less than now (so we are after it), i.e. we are "out of range"
        // In both cases there is nothing to recompute in the future
        timeConstraintValid = timeTo == null;
    }

    private XMLGregorianCalendar parseTime(MappingTimeDeclarationType timeType, OperationResult result) throws SchemaException,
            ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException,
            ExpressionEvaluationException {
        if (timeType == null) {
            return null;
        }
        XMLGregorianCalendar referenceTime;
        ExpressionType expressionType = timeType.getExpression();
        VariableBindingDefinitionType referenceTimeType = timeType.getReferenceTime();
        if (referenceTimeType == null) {
            if (expressionType == null) {
                throw new SchemaException("No reference time specified, there is also no default and no expression; in time specification in " + m.getMappingContextDescription());
            } else {
                referenceTime = null;
            }
        } else {
            referenceTime = parseTimeSource(referenceTimeType, result);
        }

        XMLGregorianCalendar time;
        if (expressionType == null) {
            if (referenceTime == null) {
                return null;
            } else {
                time = (XMLGregorianCalendar) referenceTime.clone();
            }
        } else {
            MutablePrismPropertyDefinition<XMLGregorianCalendar> timeDefinition = m.beans.prismContext.definitionFactory().createPropertyDefinition(
                    ExpressionConstants.OUTPUT_ELEMENT_NAME, PrimitiveType.XSD_DATETIME);
            timeDefinition.setMaxOccurs(1);

            ExpressionVariables timeVariables = new ExpressionVariables();
            timeVariables.addVariableDefinitions(m.variables);
            timeVariables.addVariableDefinition(ExpressionConstants.VAR_REFERENCE_TIME, referenceTime, timeDefinition);

            PrismPropertyValue<XMLGregorianCalendar> timePropVal = ExpressionUtil.evaluateExpression(m.sources, timeVariables, timeDefinition,
                    expressionType, m.expressionProfile, m.beans.expressionFactory, "time expression in " + m.getMappingContextDescription(), m.getTask(), result);

            if (timePropVal == null) {
                return null;
            }

            time = timePropVal.getValue();
        }
        Duration offset = timeType.getOffset();
        if (offset != null) {
            time.add(offset);
        }
        return time;
    }

    private XMLGregorianCalendar parseTimeSource(VariableBindingDefinitionType source, OperationResult result)
            throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException {
        ItemPath path = m.parser.getSourcePath(source);

        Object sourceObject = ExpressionUtil.resolvePathGetValue(path, m.variables, false,
                m.getTypedSourceContext(), m.beans.objectResolver, m.beans.prismContext,
                "reference time definition in " + m.getMappingContextDescription(), m.getTask(), result);
        if (sourceObject == null) {
            return null;
        }
        PrismProperty<XMLGregorianCalendar> timeProperty;
        if (sourceObject instanceof ItemDeltaItem<?, ?>) {
            //noinspection unchecked
            timeProperty = (PrismProperty<XMLGregorianCalendar>) ((ItemDeltaItem<?, ?>) sourceObject).getItemNew();
        } else if (sourceObject instanceof Item<?, ?>) {
            //noinspection unchecked
            timeProperty = (PrismProperty<XMLGregorianCalendar>) sourceObject;
        } else {
            throw new IllegalStateException("Unknown resolve result " + sourceObject);
        }
        return timeProperty != null ? timeProperty.getRealValue() : null;
    }

    boolean isTimeConstraintValid() {
        return Objects.requireNonNull(timeConstraintValid, "Time validity has not been established");
    }

    XMLGregorianCalendar getNextRecomputeTime() {
        return nextRecomputeTime;
    }
}

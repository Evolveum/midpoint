/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.trigger;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType.F_TRIGGER;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.TriggerType.F_TIMESTAMP;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.repo.common.task.ItemProcessor;
import com.evolveum.midpoint.util.exception.*;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.model.impl.tasks.scanner.AbstractScanActivityExecution;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.common.activity.execution.ExecutionInstantiationContext;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * Single execution of a trigger scanner task part.
 */
class TriggerScanActivityExecution
        extends AbstractScanActivityExecution<ObjectType, TriggerScanWorkDefinition, TriggerScanActivityHandler> {

    private static final Trace LOGGER = TraceManager.getTrace(TriggerScanActivityExecution.class);

    TriggerScanActivityExecution(
            @NotNull ExecutionInstantiationContext<TriggerScanWorkDefinition, TriggerScanActivityHandler> context) {
        super(context, "Trigger scan");
    }

    @Override
    protected ObjectQuery customizeQuery(ObjectQuery configuredQuery, OperationResult opResult) {
        return ObjectQueryUtil.addConjunctions(configuredQuery, createFilter());
    }

    private ObjectFilter createFilter() {
        LOGGER.debug("Looking for triggers with timestamps up to {}", thisScanTimestamp);
        return getPrismContext().queryFor(ObjectType.class)
                .item(F_TRIGGER, F_TIMESTAMP).le(thisScanTimestamp)
                .buildFilter();
    }

    @Override
    protected @NotNull ItemProcessor<PrismObject<ObjectType>> createItemProcessor(OperationResult opResult)
            throws SchemaException, SecurityViolationException, ObjectNotFoundException, ExpressionEvaluationException,
            CommunicationException, ConfigurationException {
        return createDefaultItemProcessor(
                new TriggerScanItemProcessor(this));
    }
}

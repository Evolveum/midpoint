/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.common.stringpolicy;

import com.evolveum.midpoint.schema.ResultHandler;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ProhibitedValueItemType;

/**
 *  Resolves "origin object" during value policy evaluation.
 *
 *  (Currently it is limited to origin object for ProhibitedValueItemType specifications.)
 */
public interface ValuePolicyOriginResolver {

    /**
     * Resolves "origin object" in given prohibitedValueItem: calls handler for each origin object found.
     */
    <R extends ObjectType> void resolve(ProhibitedValueItemType prohibitedValueItemType, ResultHandler<R> handler,
            String contextDescription, Task task, OperationResult result) throws ObjectNotFoundException, SchemaException,
            CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException;
}

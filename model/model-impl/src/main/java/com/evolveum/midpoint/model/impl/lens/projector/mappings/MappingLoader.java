/*
 * Copyright (c) 2017-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.lens.projector.mappings;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import org.jetbrains.annotations.NotNull;

/**
 * @author semancik
 *
 */
public interface MappingLoader<O extends ObjectType> {

    boolean isLoaded() throws SchemaException, ConfigurationException;

    PrismObject<O> load(String loadReason, Task task, OperationResult result)
            throws ObjectNotFoundException, CommunicationException, SchemaException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException, NotLoadedException;

    /** To be used when the exact reason of not-loaded state is not known. */
    class NotLoadedException extends Exception implements SeverityAwareException {
        NotLoadedException(String message) {
            super(message);
        }

        @Override
        public @NotNull SeverityAwareException.Severity getSeverity() {
            return SeverityAwareException.Severity.WARNING; // To be recorded in the operation result mildly
        }
    }
}

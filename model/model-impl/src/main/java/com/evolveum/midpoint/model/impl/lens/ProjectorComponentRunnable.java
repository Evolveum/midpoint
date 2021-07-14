/*
 * Copyright (c) 2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.lens;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.*;

/**
 * Runnable with proper set of exceptions that fit running of projector components.
 *
 * @author semancik
 */
@FunctionalInterface
public interface ProjectorComponentRunnable {

    void run(OperationResult result) throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException,
            SecurityViolationException, PolicyViolationException, ExpressionEvaluationException, ObjectAlreadyExistsException,
            ConflictDetectedException;

}

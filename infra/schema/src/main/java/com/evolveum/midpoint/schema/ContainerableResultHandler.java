/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schema;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.schema.result.OperationResult;

/**
 * Handles iterative processes that concern containerables.
 */
@FunctionalInterface
public interface ContainerableResultHandler<C extends Containerable> {

    /**
     * Handle a single value.
     * @param value Value to process.
     * @return true if the operation should proceed, false if it should stop
     */
    boolean handle(C value, OperationResult parentResult);
}

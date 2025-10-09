/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.schema;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;

/**
 * Classes implementing this interface are used to handle arbitrary objects (not always {@link PrismObject} instances),
 * typically - but not necessarily - coming from iterative search operation.
 *
 * TODO resolve class naming with {@link ResultHandler}; maybe ObjectHandler is not the best name at all?
 *
 * @param <T> type of the objects; intentionally general enough to cover both prism objects, containerables, and maybe
 * others in the future
 *
 * @see ResultHandler
 */
@FunctionalInterface
public interface ObjectHandler<T> {

    /**
     * Handle a single object.
     *
     * @param object Object to handle.
     * @param result Where to store the result of the processing.
     *
     * @return true if the operation should proceed, false if it should stop
     */
    boolean handle(T object, OperationResult result);
}

/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.cache.handlers;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.ResultHandler;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * Records objects being found by `searchObjectsIterative` method (to {@link OperationResult} object).
 */
class RecordingResultHandler<T extends ObjectType> implements ResultHandler<T> {

    private final ResultHandler<T> innerHandler;
    private final SearchOpExecution<T> execution;
    private int objectsFound;
    private boolean wasInterrupted;

    RecordingResultHandler(ResultHandler<T> innerHandler, SearchOpExecution<T> execution) {
        this.innerHandler = innerHandler;
        this.execution = execution;
    }

    @Override
    public boolean handle(PrismObject<T> object, OperationResult result) {
        objectsFound++;
        execution.recordObjectFound(object);
        boolean cont = innerHandler.handle(object, result);
        if (!cont) {
            wasInterrupted = true;
        }
        return cont;
    }

    void recordResult() {
        execution.recordSearchResult(objectsFound, wasInterrupted);
    }
}

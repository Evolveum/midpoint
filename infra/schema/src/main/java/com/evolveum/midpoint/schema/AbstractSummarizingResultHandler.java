/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schema;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * Class provides {@link OperationResult} summarizing functionality for result handler.
 * Result is summarized after every handled object.
 *
 * @author lazyman
 */
public abstract class AbstractSummarizingResultHandler<T extends ObjectType> implements ResultHandler<T> {

    private boolean summarizeErrors;
    private boolean summarizePartialErrors;
    private boolean summarizeSuccesses;

    protected AbstractSummarizingResultHandler() {
        this(true, true, true);
    }

    protected AbstractSummarizingResultHandler(boolean summarizeErrors, boolean summarizePartialErrors,
                                               boolean summarizeSuccesses) {
        this.summarizeErrors = summarizeErrors;
        this.summarizePartialErrors = summarizePartialErrors;
        this.summarizeSuccesses = summarizeSuccesses;
    }

    @Override
    public boolean handle(PrismObject<T> object, OperationResult parentResult) {
        parentResult.setSummarizeErrors(summarizeErrors);
        parentResult.setSummarizePartialErrors(summarizePartialErrors);
        parentResult.setSummarizeSuccesses(summarizeSuccesses);

        try {
            return handleObject(object, parentResult);
        } finally {
            parentResult.summarize();
            if (!parentResult.isUnknown()) {
                parentResult.cleanupResult();
            }
        }
    }

    /**
     * Handle a single result.
     *
     * @param object Resource object to process.
     * @return true if the operation should proceed, false if it should stop
     */
    protected abstract boolean handleObject(PrismObject<T> object, OperationResult parentResult);
}



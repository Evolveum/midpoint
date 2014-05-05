/*
 * Copyright (c) 2010-2013 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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



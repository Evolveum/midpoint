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

package com.evolveum.midpoint.notifications.events;

import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.api.context.ModelElementContext;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.ObjectDeltaOperation;
import com.evolveum.midpoint.task.api.LightweightIdentifierGenerator;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.EventCategoryType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.EventOperationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.EventStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;

import java.util.ArrayList;
import java.util.List;

/**
 * @author mederly
 */
public class ModelEvent extends Event {

    private static final Trace LOGGER = TraceManager.getTrace(ModelEvent.class);

    // we can expect that modelContext != null and focus context != null as well
    private ModelContext modelContext;

    public ModelEvent(LightweightIdentifierGenerator lightweightIdentifierGenerator) {
        super(lightweightIdentifierGenerator);
    }

    public ModelContext getModelContext() {
        return modelContext;
    }

    public ModelElementContext getFocusContext() {
        return modelContext.getFocusContext();
    }

    public void setModelContext(ModelContext modelContext) {
        this.modelContext = modelContext;
    }

    public List<? extends ObjectDeltaOperation> getExecutedDeltas() {
        return getFocusContext().getExecutedDeltas();
    }

    @Override
    public boolean isStatusType(EventStatusType eventStatusType) {
        boolean allSuccess = true, anySuccess = false, allFailure = true, anyFailure = false, anyInProgress = false;
        for (Object o : getExecutedDeltas()) {
            ObjectDeltaOperation objectDeltaOperation = (ObjectDeltaOperation) o;
            if (objectDeltaOperation.getExecutionResult() != null) {
                switch (objectDeltaOperation.getExecutionResult().getStatus()) {
                    case SUCCESS: anySuccess = true; allFailure = false; break;
                    case FATAL_ERROR: allSuccess = false; anyFailure = true; break;
                    case WARNING: anySuccess = true; allFailure = false; break;
                    case HANDLED_ERROR: anySuccess = true; allFailure = false; break;
                    case IN_PROGRESS: allSuccess = false; allFailure = false; anyInProgress = true; break;
                    case NOT_APPLICABLE: break;
                    case PARTIAL_ERROR: allSuccess = false; anyFailure = true; break;
                    case UNKNOWN: allSuccess = false; allFailure = false; break;
                    default: LOGGER.warn("Unknown execution result: " + objectDeltaOperation.getExecutionResult().getStatus());
                }
            } else {
                allSuccess = false; allFailure = false; anyInProgress = true;
            }
        }

        switch (eventStatusType) {
            case SUCCESS: return anySuccess;
            case ONLY_SUCCESS: return allSuccess;
            case FAILURE: return anyFailure;
            case ONLY_FAILURE: return allFailure;
            case IN_PROGRESS: return anyInProgress;
            default: throw new IllegalStateException("Invalid eventStatusType: " + eventStatusType);
        }
    }

    @Override
    public boolean isOperationType(EventOperationType eventOperationType) {

        for (Object o : getExecutedDeltas()) {
            ObjectDeltaOperation objectDeltaOperation = (ObjectDeltaOperation) o;
            if ((eventOperationType == EventOperationType.ADD && objectDeltaOperation.getObjectDelta().isAdd()) ||
                    (eventOperationType == EventOperationType.MODIFY && objectDeltaOperation.getObjectDelta().isModify()) ||
                    (eventOperationType == EventOperationType.DELETE && objectDeltaOperation.getObjectDelta().isDelete())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isCategoryType(EventCategoryType eventCategoryType) {
        return eventCategoryType == EventCategoryType.USER_EVENT;
    }

    public List<ObjectDelta<UserType>> getUserDeltas() {
        List<ObjectDelta<UserType>> retval = new ArrayList<ObjectDelta<UserType>>();
        Class c = modelContext.getFocusClass();
        if (c != null && UserType.class.isAssignableFrom(c)) {
            for (Object o : getExecutedDeltas()) {
                ObjectDeltaOperation objectDeltaOperation = (ObjectDeltaOperation) o;
                retval.add(objectDeltaOperation.getObjectDelta());
            }
        }
        return retval;
    }

    public ObjectDelta<UserType> getSummarizedUserDeltas() throws SchemaException {
        return ObjectDelta.summarize(getUserDeltas());
    }
}

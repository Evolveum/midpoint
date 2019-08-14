/*
 * Copyright (c) 2010-2019 Evolveum
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

package com.evolveum.midpoint.provisioning.impl.sync;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import java.util.*;

/**
 * Takes care of affinity of changes to executor tasks.
 *
 * If a task processes a change with given primary identifier I, all changes related to I are automatically assigned
 * to it. This is to avoid race conditions where a change on I that arrived later is processed before an earlier change on I.
 *
 * Change identifier I is said to be _bound_ to task T if it is being currently processed by T or is waiting to be processed.
 * If it's waiting to be processed by T it is also said to be _assigned_ (or pre-assigned) to T.
 */
class AffinityController {

    private static final Trace LOGGER = TraceManager.getTrace(AffinityController.class);

    private Map<String, Queue<ProcessChangeRequest>> assignedChangesMap = new HashMap<>();
    private Map<Object, String> bindings = new HashMap<>();       // primary identifier -> task identifier

    /**
     * Gets a change waiting for give task (if there's any).
     */
    synchronized ProcessChangeRequest getAssigned(String taskIdentifier) {
        Queue<ProcessChangeRequest> assigned = assignedChangesMap.get(taskIdentifier);
        if (assigned != null) {
            return assigned.poll();
        } else {
            return null;
        }
    }

    synchronized int hasAssigned(String taskIdentifier) {
        Queue<ProcessChangeRequest> assigned = assignedChangesMap.get(taskIdentifier);
        return assigned != null ? assigned.size() : 0;
    }

    /**
     * @return true if the request was successfully bound; false if it was reassigned (so another one has to be fetched)
     */
    synchronized boolean bind(String taskIdentifier, ProcessChangeRequest request) {
        Object primaryIdentifier = request.getPrimaryIdentifierRealValue();
        if (primaryIdentifier != null) {
            boolean reassigned = reassignIfBound(request, primaryIdentifier);
            if (reassigned) {
                return false;
            } else {
                LOGGER.trace("Binding {} to {}", primaryIdentifier, taskIdentifier);
                bindings.put(primaryIdentifier, taskIdentifier);
                return true;
            }
        } else {
            LOGGER.warn("Null primaryIdentifier in change {}", request.getChange());
            return true;
        }
    }

    private boolean reassignIfBound(ProcessChangeRequest request, Object primaryIdentifier) {
        String ownerIdentifier = bindings.get(primaryIdentifier);
        if (ownerIdentifier != null) {
            LOGGER.trace("Reassigning request of {} to {}: {}", primaryIdentifier, ownerIdentifier, request);
            addToAssigned(ownerIdentifier, request);
            return true;
        } else {
            return false;
        }
    }

    private void addToAssigned(String ownerIdentifier, ProcessChangeRequest request) {
        assignedChangesMap
                .computeIfAbsent(ownerIdentifier, key -> new LinkedList<>())
                .offer(request);
    }

    synchronized void unbind(String taskIdentifier, ProcessChangeRequest request) {
        Object primaryIdentifier = request.getPrimaryIdentifierRealValue();
        LOGGER.trace("Trying to unbind {} from {}", primaryIdentifier, taskIdentifier);
        if (primaryIdentifier != null) {
            if (isAssigned(primaryIdentifier, taskIdentifier)) {
                LOGGER.trace("...but it is assigned to its owner (some relevant changes are waiting), so not unbinding now");
            } else {
                String previousOwner = bindings.remove(primaryIdentifier);
                LOGGER.trace("Unbound (previous owner was: {})", previousOwner);
                assert taskIdentifier.equals(previousOwner);
            }
        } else {
            LOGGER.trace("primaryIdentifier is null (warning has been already issued): {}", request);
        }
    }

    private boolean isAssigned(Object primaryIdentifier, String taskIdentifier) {
        Queue<ProcessChangeRequest> assigned = assignedChangesMap.get(taskIdentifier);
        if (assigned != null) {
            for (ProcessChangeRequest request : assigned) {
                if (primaryIdentifier.equals(request.getPrimaryIdentifierRealValue())) {
                    return true;
                }
            }
        }
        return false;
    }
}

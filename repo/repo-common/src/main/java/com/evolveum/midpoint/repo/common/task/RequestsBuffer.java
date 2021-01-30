/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.task;

import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * TODO fix the description
 *
 * <p>Stores requests to be processed.</p>
 *
 * <p>Its primary responsibility is that it has to ensure that changes related to given primary identifier are processed exactly
 * in the order in which they were fetched from the resource (recorded as {@link com.evolveum.midpoint.provisioning.ucf.api.Change#localSequenceNumber}).
 * This responsibility is ensured by the concepts of binding and reservation.</p>
 *
 * <p>Primary identifier I is said to be _bound_ to task T if an object with this identifier it is being currently processed by T
 * or is waiting to be processed by T (reserved for T). See {@link #bind(ProcessChangeRequest, String)} and {@link #unbind(ProcessChangeRequest, String)}
 * methods.</p>
 *
 * <p>Reservation is represented by a queue of requests waiting to be processed by specified subtask. A request gets
 * onto this queue if it's fetched from the global queue but cannot be processed because it is bound to a subtask different
 * from the current one. So it's put into "owning" task reserved requests queue. See {@link #reserve(ProcessChangeRequest, String)}.</p>
 */
@SuppressWarnings("JavadocReference")
class RequestsBuffer<I> {

    private static final Trace LOGGER = TraceManager.getTrace(RequestsBuffer.class);

    private static final long REQUEST_QUEUE_OFFER_TIMEOUT = 1000L;

    /**
     * Global queue of waiting requests.
     */
    private final BlockingQueue<ItemProcessingRequest<I>> globalQueue;

    /**
     * Current bindings of resource objects (their identifiers) to subtasks.
     *
     * Entry key: primary resource object identifier (~ account ID)
     * Entry value: task identifier
     */
    private final Map<Object, String> bindingsMap = new HashMap<>();

    /**
     * Change requests reserved to be processed by each subtask.
     *
     * Entry key: task identifier.
     * Entry value: queue of requests. The requests are sorted by change identifier, to ensure processing in the correct order.
     */
    private final Map<String, PriorityQueue<ItemProcessingRequest<I>>> reservedRequestsQueueMap = new HashMap<>();

    RequestsBuffer(int threadsCount) {
        int globalQueueSize = threadsCount*2; // actually, size of threadsCount should be sufficient but it doesn't hurt if queue is larger
        globalQueue = new ArrayBlockingQueue<>(globalQueueSize);
    }

    /**
     * Offers a request for processing.
     *
     * This method is intentionally NOT synchronized.
     *
     * @throws InterruptedException If present, the request was not put into the queue.
     */
    boolean offer(ItemProcessingRequest<I> request) throws InterruptedException {
        return globalQueue.offer(request, REQUEST_QUEUE_OFFER_TIMEOUT, TimeUnit.MILLISECONDS);
    }

    /**
     * Tries to obtain a request to be processed by the specified subtask.
     * Does NOT wait, it just checks reserved requests and global queues.
     *
     * Post-conditions:
     * - Returns null only if there are no reserved nor globally available requests.
     * - If the request is not null, the primary identifier of the request is (successfully) bound to the specified task.
     */
    synchronized ItemProcessingRequest<I> poll(String taskIdentifier) {

        ItemProcessingRequest<I> reserved = getNextReservedRequest(taskIdentifier);
        if (reserved != null) {
            LOGGER.trace("Got reserved (pre-assigned) request: {}", reserved);
            if (bind(reserved, taskIdentifier)) {
                return reserved;
            } else {
                throw new IllegalStateException("A reserved request couldn't be bound to the current task. Request = " +
                        reserved + ", task = " + taskIdentifier);
            }
        }

        for (;;) {
            ItemProcessingRequest<I> request = globalQueue.poll();
            LOGGER.trace("Got request from global queue: {}", request);

            if (request == null) {
                // Nothing in the queue for now. OK, let's try next time.
                return null;
            }

            if (bind(request, taskIdentifier)) {
                return request;
            } else {
                // The request couldn't be bound to the specified task. We have to go and try another one.
            }
        }
    }

    /**
     * Binds a request identifier to specified subtask, if possible i.e. if the request identifier is not bound to another task.
     * If the request identifier is bound to another task, the request is assigned to it.
     *
     * @return true if the request was successfully bound to the current task;
     *         false if it was reassigned (so this task has to fetch another request).
     */
    private boolean bind(ItemProcessingRequest<I> request, String taskIdentifier) {
        Object correlationValue = request.getCorrelationValue();
        if (correlationValue == null) {
            LOGGER.warn("Null correlationValue in request {}", request.getCorrelationValue());
            return true;
        }

        String boundTo = bindingsMap.get(correlationValue);
        if (boundTo == null) {
            LOGGER.trace("Binding {} to {}", correlationValue, taskIdentifier);
            bindingsMap.put(correlationValue, taskIdentifier);
            return true;
        }

        if (boundTo.equals(taskIdentifier)) {
            LOGGER.trace("Processing {} as it is already bound to current task: {}", correlationValue, taskIdentifier);
            return true;
        }

        LOGGER.trace("Request {} (ID {}) is already bound to another task {}. Moving it to the reserved"
                + " requests queue for that task. Current task: {}", request, correlationValue, boundTo, taskIdentifier);
        reserve(request, boundTo);
        return false;
    }

    /**
     * Gets a change reserved for given task (if there's any).
     * REMOVES the reservation.
     *
     * @return Change reserved for given task; or null if there's nothing there.
     */
    private ItemProcessingRequest<I> getNextReservedRequest(String taskIdentifier) {
        Queue<ItemProcessingRequest<I>> reservedRequests = reservedRequestsQueueMap.get(taskIdentifier);
        if (reservedRequests != null) {
            return reservedRequests.poll();
        } else {
            return null;
        }
    }

    private void reserve(ItemProcessingRequest<I> request, String owningTaskIdentifier) {
        reservedRequestsQueueMap
                .computeIfAbsent(owningTaskIdentifier, key -> new PriorityQueue<>())
                .offer(request);
    }

    /**
     * Marks specified request as processed: Unbinds its primary identifier from the calling subtask.
     */
    synchronized void markProcessed(ItemProcessingRequest<I> request, String taskIdentifier) {
        unbind(request, taskIdentifier);
    }

    private void unbind(ItemProcessingRequest<I> request, String taskIdentifier) {
        Object correlationValue = request.getCorrelationValue();
        LOGGER.trace("Trying to unbind {} from {}", correlationValue, taskIdentifier);
        if (correlationValue == null) {
            LOGGER.trace("correlationValue is null (warning has been already issued): {}", request);
            return;
        }

        if (isReserved(correlationValue, taskIdentifier)) {
            LOGGER.trace("...but it is reserved to its owner (some relevant changes are waiting), so not unbinding now");
            return;
        }

        String previousOwner = bindingsMap.remove(correlationValue);
        LOGGER.trace("Unbound (previous owner was: {})", previousOwner);
        assert taskIdentifier.equals(previousOwner);
    }

    private boolean isReserved(Object primaryIdentifier, String taskIdentifier) {
        Queue<ItemProcessingRequest<I>> reservedRequests = reservedRequestsQueueMap.get(taskIdentifier);
        if (reservedRequests != null) {
            for (ItemProcessingRequest<I> request : reservedRequests) {
                if (primaryIdentifier.equals(request.getCorrelationValue())) {
                    return true;
                }
            }
        }
        return false;
    }

    synchronized int getReservedRequestsCount(String taskIdentifier) {
        Queue<ItemProcessingRequest<I>> reservedRequests = reservedRequestsQueueMap.get(taskIdentifier);
        return reservedRequests != null ? reservedRequests.size() : 0;
    }

    // should be called when there's no concurrency
    void nackAllRequests(OperationResult result) {
        nackAll(globalQueue, result);
        reservedRequestsQueueMap.values().forEach(queue -> nackAll(queue, result));
    }

    private void nackAll(Queue<ItemProcessingRequest<I>> queue, OperationResult result) {
        for (;;) {
            ItemProcessingRequest<I> request = queue.poll();
            if (request == null) {
                break;
            } else {
                request.acknowledge(false, result);
            }
        }
    }
}

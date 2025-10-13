/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.provisioning.api;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;

import org.jetbrains.annotations.NotNull;

/**
 * Listens for {@link ResourceOperationDescription} events. Used e.g. for resource objects-related notifications.
 *
 * The calls to methods here should return without a major delay. It means that implementations can do calls to repository,
 * but they should not (synchronously) initiate a long-running process or a provisioning request.
 *
 * @author Radovan Semancik
 *
 * @see ResourceOperationDescription
 */
public interface ResourceOperationListener extends ProvisioningListener {

    /**
     * Submits notification about a success of a provisioning operation.
     *
     * This describes the operation that was executed on the resource and it
     * was successful. The upper layers are notified to post-process the result.
     *
     * It should be called for both synchronous and asynchronous operations.
     * For synchronous operations it should provide the same result as is
     * returned from the operation.
     *
     * @param parentResult the result that can be used to collect subresults of the listener execution.
     *                         It is NOT the result of the operation that succeeded. That result is inside the
     */
    void notifySuccess(@NotNull ResourceOperationDescription operationDescription, Task task, OperationResult parentResult);


    /**
     * Submits notification about a failure of provisioning operation.
     *
     * This describes the operation that was executed on the resource and it
     * failed. The upper layers are notified to handle that failure.
     *
     * This should be called for operations that were executed and failed
     * to execute.
     *
     * This should be called only for the FINAL result. E.g. if the system will
     * never again try the operation. If the operation has been just tried and
     * there is an intermediary failure (e.g. a timeout) then this operation
     * should NOT be called. notifyInProgress is used for that purpose.
     * However, this operation should be called even for timeout-like errors
     * if the system stops trying and will no longer attempt to retry the operation
     * (e.g. if the attempt counter hits the maximum attempts limit).
     *
     * It should be called for both synchronous and asynchronous operations.
     * For synchronous operations it should provide the same result as is
     * returned from the operation.
     *
     * @param parentResult the result that can be used to collect subresults of the listener execution.
     *                         It is NOT the result of the operation that failed. That result is inside the
     *                         operationDescription structure.
     */
    void notifyFailure(@NotNull ResourceOperationDescription operationDescription, Task task, OperationResult parentResult);

    /**
     * Submits notification about provisioning operation that is in progress.
     *
     * This describes the operation that was tried to be executed on the resource
     * but cannot be executed immediately. This may be caused by the resource being
     * down (communication error), it may be caused by the mode of resource operation
     * (e.g. asynchronous processing) or by any similar cause.
     *
     * This should be called after each attempt that the operation was tried. The number
     * of attempts already tried in indicated inside operationDescription structure. This
     * can be used e.g. to detect a first attempt to send mail to admin only once.
     *
     * This should NOT be called for the final result of the operation. Methods notifySuccess
     * and notifyFailure should be used instead.
     *
     * @param parentResult the result that can be used to collect subresults of the listener execution.
     *                         It is NOT the result of the operation that is in progress. That result is inside the
     */
    void notifyInProgress(@NotNull ResourceOperationDescription operationDescription, Task task, OperationResult parentResult);

}

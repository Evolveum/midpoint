/*
 * Copyright (c) 2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.ucf.api.async;

import com.evolveum.midpoint.schema.AcknowledgementSink;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.provisioning.ucf.api.UcfAsyncUpdateChange;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;

/**
 * Processes asynchronous changes encountered on a resource.
 */
public interface UcfAsyncUpdateChangeListener {

    /**
     * Called when the connector learns about a resource change.
     * @param change The change
     * @param task Task in context of which the further processing should take place
     *
     * <p>The task can be the same as the caller's task, but it can be also different.
     * This is the case when doing multithreaded reading from message source
     * (Although it should be used with care because of message ordering issues.)
     * Or when the source itself emits messages in a thread different from the caller's one.</p>
     */
    void onChange(UcfAsyncUpdateChange change, Task task, OperationResult result);

    /**
     * Called when the connector gets a message but it cannot be converted to change or changes.
     *
     * @param localSequentialNumber Sequential number of this "quasi-change"
     * @param throwable A throwable describing the problem
     * @param acknowledgementSink Where to send acknowledgements to
     */
    void onError(int localSequentialNumber, @NotNull Throwable throwable, @NotNull AcknowledgementSink acknowledgementSink,
            OperationResult result);
}

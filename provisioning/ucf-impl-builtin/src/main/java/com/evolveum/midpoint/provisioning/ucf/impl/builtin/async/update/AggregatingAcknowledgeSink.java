/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.ucf.impl.builtin.async.update;

import com.evolveum.midpoint.schema.AcknowledgementSink;
import com.evolveum.midpoint.schema.result.OperationResult;

/**
 * Collects acknowledgements. After getting all of them, issues the verdict.
 * The current algorithm is that if anyone rejects, the verdict is "reject".
 *
 * TODO consider rejection as soon as first negative acknowledgement arrives.
 */
class AggregatingAcknowledgeSink implements AcknowledgementSink {

    private int remainingReplies;
    private boolean value;
    private final AcknowledgementSink sink;

    AggregatingAcknowledgeSink(AcknowledgementSink sink, int expectedReplies) {
        this.sink = sink;

        assert expectedReplies > 0;
        this.remainingReplies = expectedReplies;
        this.value = true;
    }

    @Override
    public synchronized void acknowledge(boolean release, OperationResult result) {
        if (!release) {
            value = false;
        }
        remainingReplies--;
        if (remainingReplies == 0) {
            sink.acknowledge(value, result);
        }
    }
}

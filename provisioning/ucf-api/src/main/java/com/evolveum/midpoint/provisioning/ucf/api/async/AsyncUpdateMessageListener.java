/*
 * Copyright (c) 2019 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.provisioning.ucf.api.async;

import com.evolveum.midpoint.schema.AcknowledgementSink;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AsyncUpdateMessageType;

/**
 * Listener that receives "raw" async update messages from asynchronous update source (e.g. AMQP, JMS, REST, ...).
 */
public interface AsyncUpdateMessageListener {

    /**
     * Processes a message; typically by transforming it into UcfChangeType and invoking a synchronization procedure.
     *
     * @param acknowledgementSink An interface accepting acknowledgements about whether the message can be forgotten.
     */
    void onMessage(AsyncUpdateMessageType message, AcknowledgementSink acknowledgementSink);
}

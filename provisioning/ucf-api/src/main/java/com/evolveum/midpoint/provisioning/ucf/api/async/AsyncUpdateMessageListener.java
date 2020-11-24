/*
 * Copyright (c) 2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.ucf.api.async;

import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AsyncUpdateMessageType;

/**
 * Listener that receives "raw" async update messages from asynchronous update source (e.g. AMQP, JMS, REST, ...).
 */
public interface AsyncUpdateMessageListener {

    /**
     * Processes a message; typically by transforming it into UcfChangeType and invoking a synchronization procedure.
     *
     * @return true if the message was successfully processed and can be acknowledged;
     *         false (or by throwing an exception) otherwise
     */
    boolean onMessage(AsyncUpdateMessageType message) throws SchemaException;
}

/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.api;

import com.evolveum.midpoint.schema.result.OperationResult;

/**
 * TODO
 */
public interface SynchronizationEventHandler<E extends SynchronizationEvent> {

    /**
     * Passes an event to be handled.
     *
     * @return false if the emitter should stop producing further events
     *
     * @apiNote The handler MUST eventually acknowledge the event. It can be done within this method invocation
     * (in case of synchronous operation), or later. But eventually it must be done.
     *
     * Also, the handler must do all it can to NOT throw an exception.
     */
    boolean handle(E event, OperationResult opResult);
}

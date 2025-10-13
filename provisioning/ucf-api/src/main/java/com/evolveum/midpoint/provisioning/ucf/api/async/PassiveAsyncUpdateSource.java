/*
 * Copyright (c) 2019 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.provisioning.ucf.api.async;

import com.evolveum.midpoint.util.exception.SchemaException;

import com.google.common.annotations.VisibleForTesting;

/**
 * Passive source for asynchronous updates.
 */
@VisibleForTesting // just to provide mock implementations
public interface PassiveAsyncUpdateSource extends AsyncUpdateSource {

    /**
     * Sends the next update (f there's any) to the listener.
     * @return true if there was a message emitted
     */
    boolean getNextUpdate(AsyncUpdateMessageListener listener) throws SchemaException;

    /**
     * @return true if the source is (still) open i.e. it can deliver messages - now or in the future
     */
    boolean isOpen();
}

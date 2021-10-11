/*
 * Copyright (c) 2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.ucf.api.async;

import com.evolveum.midpoint.provisioning.ucf.api.ListeningActivity;
import com.evolveum.midpoint.util.exception.SchemaException;

/**
 * Active source of asynchronous updates, i.e. one that invokes message listener when it has something to process.
 */
public interface ActiveAsyncUpdateSource extends AsyncUpdateSource {

    /**
     * Starts listening on this async update source.
     * Returns a ListeningActivity that is to be used to stop the listening.
     */
    ListeningActivity startListening(AsyncUpdateMessageListener listener) throws SchemaException;
}

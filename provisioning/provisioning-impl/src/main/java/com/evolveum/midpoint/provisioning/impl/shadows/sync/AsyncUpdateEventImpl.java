/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.provisioning.impl.shadows.sync;

import com.evolveum.midpoint.provisioning.api.AsyncUpdateEvent;
import com.evolveum.midpoint.provisioning.impl.shadows.ShadowedAsyncChange;

/**
 * TODO
 */
abstract class AsyncUpdateEventImpl extends SynchronizationEventImpl<ShadowedAsyncChange> implements AsyncUpdateEvent {

    AsyncUpdateEventImpl(ShadowedAsyncChange change) {
        super(change);
    }
}

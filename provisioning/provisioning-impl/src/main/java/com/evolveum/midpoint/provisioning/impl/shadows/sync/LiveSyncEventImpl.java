/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.provisioning.impl.shadows.sync;

import com.evolveum.midpoint.provisioning.api.LiveSyncEvent;
import com.evolveum.midpoint.provisioning.impl.shadows.ShadowedLiveSyncChange;

/**
 * Abstract {@link LiveSyncEvent} implementation. Currently, it has only one specialization, so we may consider simplifying
 * it somehow.
 */
abstract class LiveSyncEventImpl extends SynchronizationEventImpl<ShadowedLiveSyncChange> implements LiveSyncEvent {

    LiveSyncEventImpl(ShadowedLiveSyncChange change) {
        super(change);
    }
}

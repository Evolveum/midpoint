/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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

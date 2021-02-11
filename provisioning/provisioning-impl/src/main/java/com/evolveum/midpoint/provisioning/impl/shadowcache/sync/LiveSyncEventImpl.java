/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.shadowcache.sync;

import com.evolveum.midpoint.provisioning.api.LiveSyncEvent;
import com.evolveum.midpoint.provisioning.impl.shadowcache.AdoptedLiveSyncChange;

/**
 * TODO
 */
abstract class LiveSyncEventImpl extends SynchronizationEventImpl<AdoptedLiveSyncChange> implements LiveSyncEvent {

    LiveSyncEventImpl(AdoptedLiveSyncChange change) {
        super(change);
    }

}

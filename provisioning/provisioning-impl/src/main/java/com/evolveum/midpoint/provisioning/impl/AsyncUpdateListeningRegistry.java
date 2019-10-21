/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl;

import com.evolveum.midpoint.provisioning.ucf.api.ListeningActivity;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Keeps track of active async update listening activities.
 * Externally visible methods are synchronized to ensure thread safety.
 */
@Component
public class AsyncUpdateListeningRegistry {

    private AtomicLong counter = new AtomicLong(0);

    private Map<String, ListeningActivity> listeningActivities = new HashMap<>();

    @NotNull
    synchronized String registerListeningActivity(ListeningActivity activity) {
        String handle = String.valueOf(counter.incrementAndGet());
        listeningActivities.put(handle, activity);
        return handle;
    }

    synchronized ListeningActivity removeListeningActivity(@NotNull String handle) {
        ListeningActivity listeningActivity = getListeningActivity(handle);
        if (listeningActivity == null) {
            throw new IllegalArgumentException("Listening activity handle " + handle + " is unknown at this moment");
        }
        listeningActivities.remove(handle);
        return listeningActivity;
    }

    synchronized ListeningActivity getListeningActivity(@NotNull String handle) {
        return listeningActivities.get(handle);
    }
}

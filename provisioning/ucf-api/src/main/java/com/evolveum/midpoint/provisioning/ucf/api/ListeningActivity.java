/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.provisioning.ucf.api;

/**
 * Single listening activity, e.g. accepting notifications from message queue, or listening on REST endpoint.
 */
public interface ListeningActivity {

    /**
     * Stops this listening activity.
     */
    void stop();

    /**
     * @return true if this activity is alive i.e. it can (eventually) deliver some messages
     */
    boolean isAlive();
}

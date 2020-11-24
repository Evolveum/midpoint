/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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

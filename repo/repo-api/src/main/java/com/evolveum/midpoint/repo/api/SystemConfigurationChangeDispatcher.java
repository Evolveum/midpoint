/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.api;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.SchemaException;

/**
 * Central point of dispatching notifications about changes to the system configuration object.
 */
public interface SystemConfigurationChangeDispatcher {

    /**
     * Dispatches information on system configuration object change.
     *
     * Basically this directly pushes information to lower layers (prism, schema, repo, etc), and calls registered
     * listeners that originate in upper layers.
     *
     * @param ignoreVersion If false, the information is dispatched unconditionally. If true, we dispatch the notification only
     *                      if the system configuration version was really changed. This is to easily support sources that
     *                      "ping" sysconfig object in regular intervals, e.g. the cluster manager thread.
     * @param allowNotFound If true, we take non-existence of sysconfig object more easily. To be used e.g. on system init or
     *                      during tests execution.
     */
    void dispatch(boolean ignoreVersion, boolean allowNotFound, OperationResult result) throws SchemaException;

    /**
     * Registers a listener that will be updated on system configuration object changes.
     */
    void registerListener(SystemConfigurationChangeListener listener);

    /**
     * Unregisters a listener.
     */
    void unregisterListener(SystemConfigurationChangeListener listener);

}

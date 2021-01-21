/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.api;

import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;
import org.jetbrains.annotations.Nullable;

/**
 * Listener that needs to receive notifications related to system configuration object changes.
 */
public interface SystemConfigurationChangeListener {
    /**
     * Updates the listener's internal state with the configuration provided.
     *
     * @param value Current value of the system configuration object. It is 'null' if the object does not exist.
     *              Usually listeners keep their current state in such cases, but if needed, it will have the information
     *              about missing sysconfig object, so it could act accordingly.
     */
    void update(@Nullable SystemConfigurationType value);
}

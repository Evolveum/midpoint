/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.projector.mappings;

import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;

public interface LoadedStateProvider {

    /** Returns {@code true} if the shadow or shadow item is loaded: either cached (from repo) or fresh (from resource). */
    boolean isLoaded() throws SchemaException, ConfigurationException;
}

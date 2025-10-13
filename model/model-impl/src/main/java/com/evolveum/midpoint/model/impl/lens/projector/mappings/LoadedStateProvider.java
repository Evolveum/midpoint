/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.impl.lens.projector.mappings;

import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;

public interface LoadedStateProvider {

    /** Returns {@code true} if the shadow or shadow item is loaded: either cached (from repo) or fresh (from resource). */
    boolean isLoaded() throws SchemaException, ConfigurationException;
}

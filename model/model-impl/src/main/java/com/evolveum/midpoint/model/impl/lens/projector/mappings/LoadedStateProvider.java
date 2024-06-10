package com.evolveum.midpoint.model.impl.lens.projector.mappings;

import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;

public interface LoadedStateProvider {
    boolean isLoaded() throws SchemaException, ConfigurationException;
}

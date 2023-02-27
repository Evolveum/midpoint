/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.api.correlator;

import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;

/**
 * A registry of correlator factories.
 *
 * This method resides in model API module just because a custom code may need to register its own correlator
 * and correlator factory.
 */
public interface CorrelatorFactoryRegistry {

    /**
     * Registers a correlator factory. Typically called from a `@PostConstruct` method.
     *
     * @param name Name of the configuration item. Must be qualified.
     */
    void registerFactory(@NotNull QName name, @NotNull CorrelatorFactory<?, ?> factory);
}

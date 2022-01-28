/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.api.correlator;

import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractCorrelatorType;

/**
 * A registry of correlator factories.
 */
public interface CorrelatorFactoryRegistry {

    /**
     * Registers a correlator factory. Typically called from a `@PostConstruct` method.
     *
     * @param name Name of the configuration item. Must be qualified.
     */
    void registerFactory(@NotNull QName name, @NotNull CorrelatorFactory<?, ?> factory);

    /**
     * Convenience method to look up a correlator factory based on the specific (typed) configuration,
     * and then instantiate the correlator.
     */
    <CB extends AbstractCorrelatorType> @NotNull Correlator instantiateCorrelator(
            @NotNull CorrelatorContext<CB> correlatorContext,
            @NotNull Task task,
            @NotNull OperationResult result) throws ConfigurationException;
}

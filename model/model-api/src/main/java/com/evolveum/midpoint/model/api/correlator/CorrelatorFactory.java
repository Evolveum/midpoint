/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.api.correlator;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractCorrelatorType;

import org.jetbrains.annotations.NotNull;

/**
 * Instantiates configured correlators.
 *
 * @see Correlator
 *
 * @param <C> class of correlators instantiated
 * @param <CB> class of correlator configuration bean
 */
public interface CorrelatorFactory<C extends Correlator, CB extends AbstractCorrelatorType> {

    /**
     * Instantiates correlator of given type with provided configuration.
     *
     * TODO consider deleting unused `task` parameter
     */
    @NotNull C instantiate(@NotNull CorrelatorContext<CB> configuration, @NotNull Task task, @NotNull OperationResult result)
            throws ConfigurationException;

    /**
     * Returns the type of configuration bean supported by this factory.
     */
    @NotNull Class<CB> getConfigurationBeanType();
}

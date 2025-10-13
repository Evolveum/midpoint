/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.common.rest;

import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface ConverterInterface {

    /**
     * Converts incoming object into a form that is consumable by the REST service.
     *
     * @param input Object to be converted (coming as input)
     * @return Object to be passed to the REST service.
     */
    Object convert(@NotNull Object input);
}

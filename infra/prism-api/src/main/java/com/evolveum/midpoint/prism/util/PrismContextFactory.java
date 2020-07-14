/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.prism.util;

import java.io.FileNotFoundException;
import java.io.IOException;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.util.exception.SchemaException;

/**
 * @author Radovan Semancik
 *
 */
@FunctionalInterface
public interface PrismContextFactory {

    /**
     * Returns UNINITIALIZED prism context.
     */
    PrismContext createPrismContext() throws SchemaException, IOException;

}

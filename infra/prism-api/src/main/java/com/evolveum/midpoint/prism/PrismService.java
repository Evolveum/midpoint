/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism;

/**
 * Statically holds an instance of PrismContext (and maybe other beans later).
 */
public class PrismService {

    private static final PrismService INSTANCE = new PrismService();

    private static PrismContext prismContext;

    private PrismService() {
    }

    public static PrismService get() {
        return INSTANCE;
    }

    public PrismContext prismContext() {
        return prismContext;
    }

    public void prismContext(PrismContext prismContext) {
        PrismService.prismContext = prismContext;
    }
}

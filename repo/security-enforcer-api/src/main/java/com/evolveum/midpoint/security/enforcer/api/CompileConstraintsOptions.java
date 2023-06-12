/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.security.enforcer.api;

public class CompileConstraintsOptions {

    /**
     * If `true`, sub-object selectors (i.e. ones that deal with values of containers, references, and properties)
     * are skipped. This is used e.g. when dealing with objects and deltas in lens element context, where the data being
     * processed may differ from the data upon which the authorizations are derived. (Hence, we can safely deal only with
     * selectors that do not distinguish between item values.)
     */
    private final boolean skipSubObjectSelectors;

    private static final CompileConstraintsOptions DEFAULT_ONES = new CompileConstraintsOptions();
    private static final CompileConstraintsOptions SKIP_SUB_OBJECT_SELECTORS = DEFAULT_ONES.withSkipSubObjectSelectors();

    private CompileConstraintsOptions() {
        this.skipSubObjectSelectors = false;
    }

    private CompileConstraintsOptions(boolean skipSubObjectSelectors) {
        this.skipSubObjectSelectors = skipSubObjectSelectors;
    }

    public static CompileConstraintsOptions defaultOnes() {
        return DEFAULT_ONES;
    }

    public static CompileConstraintsOptions skipSubObjectSelectors() {
        return SKIP_SUB_OBJECT_SELECTORS;
    }

    public boolean isSkipSubObjectSelectors() {
        return skipSubObjectSelectors;
    }

    @SuppressWarnings("WeakerAccess")
    public CompileConstraintsOptions withSkipSubObjectSelectors() {
        return new CompileConstraintsOptions(true);
    }
}

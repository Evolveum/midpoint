/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.security.enforcer.api;

import com.evolveum.midpoint.util.annotation.Experimental;

@Experimental
public class CompileConstraintsOptions {

    /**
     * FIXME this will be implemented in a different way (LensContext authorizations will be computed precisely)
     *
     * If `true`, sub-object selectors (i.e. ones that deal with values of containers, references, and properties)
     * are skipped. This is used e.g. when dealing with objects and deltas in lens element context, where the data being
     * processed may differ from the data upon which the authorizations are derived. (Hence, we can safely deal only with
     * selectors that do not distinguish between item values.)
     */
    private final boolean skipSubObjectSelectors;

    /**
     * Is full information available?
     *
     * TODO This is maybe not the right place; we should probably have a variant of {@link AuthorizationParameters} here.
     *
     * @see AuthorizationParameters#fullInformationAvailable
     */
    private final boolean fullInformationAvailable;

    private static final CompileConstraintsOptions DEFAULT = new CompileConstraintsOptions();
    private static final CompileConstraintsOptions SKIP_SUB_OBJECT_SELECTORS = DEFAULT.withSkipSubObjectSelectors();

    private CompileConstraintsOptions() {
        this.skipSubObjectSelectors = false;
        this.fullInformationAvailable = true;
    }

    private CompileConstraintsOptions(boolean skipSubObjectSelectors, boolean fullInformationAvailable) {
        this.skipSubObjectSelectors = skipSubObjectSelectors;
        this.fullInformationAvailable = fullInformationAvailable;
    }

    public static CompileConstraintsOptions create() {
        return DEFAULT;
    }

    public static CompileConstraintsOptions skipSubObjectSelectors() {
        return SKIP_SUB_OBJECT_SELECTORS;
    }

    public boolean isSkipSubObjectSelectors() {
        return skipSubObjectSelectors;
    }

    public boolean isFullInformationAvailable() {
        return fullInformationAvailable;
    }

    @SuppressWarnings("WeakerAccess")
    public CompileConstraintsOptions withSkipSubObjectSelectors() {
        return new CompileConstraintsOptions(true, fullInformationAvailable);
    }

    public CompileConstraintsOptions withFullInformationAvailable(boolean fullInformationAvailable) {
        return new CompileConstraintsOptions(skipSubObjectSelectors, fullInformationAvailable);
    }
}

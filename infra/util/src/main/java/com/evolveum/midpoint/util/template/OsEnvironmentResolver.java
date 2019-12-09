/*
 * Copyright (c) 2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.util.template;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Resolves references using operating system environment variables.
 */
public class OsEnvironmentResolver extends AbstractChainedResolver {

    @SuppressWarnings("WeakerAccess")
    public static final String SCOPE = "env";

    public OsEnvironmentResolver(ReferenceResolver upstreamResolver, boolean actsAsDefault) {
        super(upstreamResolver, actsAsDefault);
    }

    @Override
    protected String resolveLocally(String reference, List<String> parameters) {
        return System.getenv(reference);
    }

    @NotNull
    @Override
    protected Collection<String> getScopes() {
        return Collections.singleton(SCOPE);
    }
}

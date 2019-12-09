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
 * Resolves references using Java system properties.
 */
public class JavaPropertiesResolver extends AbstractChainedResolver {

    @SuppressWarnings("WeakerAccess")
    public static final String SCOPE = "prop";

    public JavaPropertiesResolver(ReferenceResolver upstreamResolver, boolean actsAsDefault) {
        super(upstreamResolver, actsAsDefault);
    }

    @Override
    protected String resolveLocally(String reference, List<String> parameters) {
        return System.getProperty(reference);
    }

    @NotNull
    @Override
    protected Collection<String> getScopes() {
        return Collections.singletonList(SCOPE);
    }
}

/*
 * Copyright (c) 2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.util.template;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;

/**
 * Resolver that is able to call upstream if it cannot resolve a reference.
 */
public abstract class AbstractChainedResolver implements ReferenceResolver {

    /**
     * Resolver to call if we are unable to help.
     */
    private final ReferenceResolver upstreamResolver;

    /**
     * Whether this resolver is engaged in resolving references in default (null) scope.
     */
    private final boolean actsAsDefault;

    public AbstractChainedResolver(ReferenceResolver upstreamResolver, boolean actsAsDefault) {
        this.upstreamResolver = upstreamResolver;
        this.actsAsDefault = actsAsDefault;
    }

    protected abstract String resolveLocally(String reference, List<String> parameters);

    @NotNull
    protected abstract Collection<String> getScopes();

    @Override
    public String resolve(String scope, String reference, @NotNull List<String> parameters) {
        if (isScopeApplicable(scope)) {
            String resolution = resolveLocally(reference, parameters);
            if (resolution != null) {
                return resolution;
            }
        }

        if (upstreamResolver != null) {
            return upstreamResolver.resolve(scope, reference, parameters);
        } else {
            return null;
        }
    }

    private boolean isScopeApplicable(String scope) {
        return actsAsDefault && scope == null || getScopes().contains(scope);
    }
}

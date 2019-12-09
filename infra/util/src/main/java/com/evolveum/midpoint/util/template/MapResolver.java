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
import java.util.Map;

/**
 * Resolves references using externally provided map.
 */
public class MapResolver extends AbstractChainedResolver {

    private final String myScope;
    private final Map<String, String> map;

    public MapResolver(ReferenceResolver upstreamResolver, boolean actsAsDefault, String myScope, Map<String, String> map) {
        super(upstreamResolver, actsAsDefault);
        this.myScope = myScope;
        this.map = map;
    }

    @Override
    protected String resolveLocally(String reference, List<String> parameters) {
        return map.get(reference);
    }

    @NotNull
    @Override
    protected Collection<String> getScopes() {
        return myScope != null ? Collections.singleton(myScope) : Collections.emptySet();
    }
}

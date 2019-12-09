/*
 * Copyright (c) 2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.util.template;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Resolver of references in templates.
 */
@FunctionalInterface
public interface ReferenceResolver {

    /**
     * Returns resolved value of the reference.
     *
     * @param scope      Scope e.g. null meaning default scope, "env" meaning environment variables, "" meaning built-in references etc.
     * @param reference  Reference name e.g. "seq"
     * @param parameters Reference parameters e.g. "%04d"
     * @return Resolved reference value e.g. "0003"
     */
    String resolve(String scope, String reference, @NotNull List<String> parameters);
}
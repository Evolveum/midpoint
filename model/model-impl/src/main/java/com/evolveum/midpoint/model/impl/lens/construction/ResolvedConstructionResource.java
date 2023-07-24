/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.construction;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.Objects;

/**
 * Information on the resource referenced by particular {@link ResourceObjectConstruction} - the object
 * as well as information what to do in case it couldn't be resolved.
 */
public class ResolvedConstructionResource implements Serializable {

    /**
     * Resolved form of the object.
     */
    @Nullable public final ResourceType resource;

    /**
     * Is the fact that the object couldn't be found a warning, or is it even to be simply ignored?
     */
    public final boolean warning;

    ResolvedConstructionResource(@NotNull ResourceType resource) {
        this.resource = resource;
        this.warning = false;
    }

    ResolvedConstructionResource(boolean warning) {
        this.resource = null;
        this.warning = warning;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) { return true; }
        if (!(o instanceof ResolvedConstructionResource that)) { return false; }
        return warning == that.warning && Objects.equals(resource, that.resource);
    }

    @Override
    public int hashCode() {
        return Objects.hash(resource, warning);
    }
}

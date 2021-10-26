/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.sync.tasks;

import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.common.activity.run.SearchSpecification;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public class ResourceSearchSpecification extends SearchSpecification<ShadowType> {

    @NotNull private final ResourceObjectClassSpecification resourceObjectClassSpecification;

    ResourceSearchSpecification(@NotNull ResourceObjectClassSpecification resourceObjectClassSpecification,
            ObjectQuery query, Collection<SelectorOptions<GetOperationOptions>> options) {
        super(ShadowType.class, query, options, null);
        this.resourceObjectClassSpecification = resourceObjectClassSpecification;
    }

    private ResourceSearchSpecification(@NotNull ResourceSearchSpecification prototype) {
        super(prototype);
        this.resourceObjectClassSpecification = prototype.resourceObjectClassSpecification; // Not cloning for now FIXME
    }

    // FIXME problematic: usually we need TargetInfo in initializeExecution, i.e. before search specification is ready
    public @NotNull ResourceObjectClassSpecification getTargetInfo() {
        return resourceObjectClassSpecification;
    }

    @Override
    public ResourceSearchSpecification clone() {
        return new ResourceSearchSpecification(this);
    }
}

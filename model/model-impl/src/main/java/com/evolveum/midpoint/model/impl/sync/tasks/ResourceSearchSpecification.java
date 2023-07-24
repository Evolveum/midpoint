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

/**
 * Specialization of standard {@link SearchSpecification} dealing with resource objects search.
 *
 * (We could maybe delete this class in the future.)
 */
public class ResourceSearchSpecification extends SearchSpecification<ShadowType> {

    ResourceSearchSpecification(ObjectQuery query, Collection<SelectorOptions<GetOperationOptions>> options) {
        super(ShadowType.class, query, options, null);
    }

    private ResourceSearchSpecification(@NotNull ResourceSearchSpecification prototype) {
        super(prototype);
    }

    @SuppressWarnings("MethodDoesntCallSuperMethod")
    @Override
    public ResourceSearchSpecification clone() {
        return new ResourceSearchSpecification(this);
    }
}

/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.sync.tasks;

import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.common.task.SearchSpecification;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public class ResourceSearchSpecification extends SearchSpecification<ShadowType> {

    @NotNull private final TargetInfo targetInfo;

    ResourceSearchSpecification(@NotNull TargetInfo targetInfo, ObjectQuery query, Collection<SelectorOptions<GetOperationOptions>> options) {
        super(ShadowType.class, query, options, null);
        this.targetInfo = targetInfo;
    }

    // FIXME problematic: usually we need TargetInfo in initializeExecution, i.e. before search specification is ready
    public @NotNull TargetInfo getTargetInfo() {
        return targetInfo;
    }
}

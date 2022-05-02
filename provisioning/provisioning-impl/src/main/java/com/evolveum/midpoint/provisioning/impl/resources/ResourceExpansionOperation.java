/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.resources;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.provisioning.impl.CommonBeans;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

/**
 * TODO better name
 *
 * Represents an operation where the concrete (incomplete) resource is expanded into its full form by resolving
 * its ancestor(s). This process is called resource inheritance, and is used to implement e.g. resource templates.
 */
class ResourceExpansionOperation {

    private static final String OP_EXPAND = ResourceExpansionOperation.class.getName() + ".expand";

    /**
     * The resource being expanded. TODO name
     */
    @NotNull private final PrismObject<ResourceType> resource;
    @NotNull private final CommonBeans beans;

    @NotNull private final Set<String> ancestorsOids = new HashSet<>();

    ResourceExpansionOperation(@NotNull PrismObject<ResourceType> resource, @NotNull CommonBeans beans) {
        this.resource = resource;
        this.beans = beans;
    }

    public void execute(OperationResult parentResult) {
        OperationResult result = parentResult.createMinorSubresult(OP_EXPAND);
        try {
            // TODO
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            result.close();
        }
    }

    public @NotNull Set<String> getAncestorsOids() {
        return ancestorsOids;
    }
}

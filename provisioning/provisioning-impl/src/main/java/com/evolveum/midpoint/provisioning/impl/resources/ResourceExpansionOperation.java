/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.resources;

import com.evolveum.midpoint.provisioning.impl.CommonBeans;
import com.evolveum.midpoint.provisioning.impl.resources.merger.ResourceMergeOperation;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.SuperResourceDeclarationType;

import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

/**
 * TODO better name
 *
 * Represents an operation where the concrete (incomplete) resource is expanded into its full form by resolving
 * its ancestor(s). This feature is called resource inheritance, and is used to implement e.g. resource templates.
 */
class ResourceExpansionOperation {

    private static final String OP_EXPAND = ResourceExpansionOperation.class.getName() + ".expand";

    private static final Trace LOGGER = TraceManager.getTrace(ResourceExpansionOperation.class);

    /**
     * The resource being expanded. TODO name
     */
    @NotNull private final ResourceType resource;

    /** Useful beans. */
    @NotNull private final CommonBeans beans;

    @NotNull private final Set<String> ancestorsOids = new HashSet<>();

    ResourceExpansionOperation(@NotNull ResourceType resource, @NotNull CommonBeans beans) {
        this.resource = resource;
        this.beans = beans;
    }

    public void execute(OperationResult parentResult) throws SchemaException, ConfigurationException, ObjectNotFoundException {
        OperationResult result = parentResult.createMinorSubresult(OP_EXPAND);
        try {
            if (resource.getSuper() != null) {
                doExpansion(result);
            } else {
                LOGGER.trace("Expansion done for {}, as there is no super-resource for it.", resource);
            }
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            result.close();
        }
    }

    private void doExpansion(OperationResult result) throws SchemaException, ConfigurationException, ObjectNotFoundException {
        ResourceType superResource = getExpandedSuperResource(result);

        new ResourceMergeOperation(resource, superResource)
                .execute();

        resource.setSuper(null);
    }

    private @NotNull ResourceType getExpandedSuperResource(OperationResult result)
            throws ConfigurationException, ObjectNotFoundException, SchemaException {
        LOGGER.trace("Doing expansion for {} having a super-resource declaration", resource);
        SuperResourceDeclarationType declaration = resource.getSuper();
        ObjectReferenceType superRef = MiscUtil.requireNonNull(
                declaration.getResourceRef(),
                () -> new ConfigurationException("No resourceRef in super-resource declaration"));

        String superOid = MiscUtil.requireNonNull(
                superRef.getOid(),
                () -> new ConfigurationException("No OID in super-resource reference (filters are not supported there, yet)"));

        ResourceType superResource = beans.cacheRepositoryService
                .getObject(ResourceType.class, superOid, null, result)
                .asObjectable();
        ResourceExpansionOperation superExpansionOperation = new ResourceExpansionOperation(superResource, beans);
        superExpansionOperation.execute(result);

        ancestorsOids.addAll(superExpansionOperation.getAncestorsOids());
        ancestorsOids.add(superResource.getOid());

        LOGGER.trace("Super-resource {} of {} is expanded", superResource, resource);
        return superResource;
    }

    public @NotNull Set<String> getAncestorsOids() {
        return ancestorsOids;
    }

    public @NotNull ResourceType getResource() {
        return resource;
    }
}

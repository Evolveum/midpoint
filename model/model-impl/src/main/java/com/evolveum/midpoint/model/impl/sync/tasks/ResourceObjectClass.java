/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.sync.tasks;

import static java.util.Objects.requireNonNull;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.provisioning.api.LiveSyncEventHandler;
import com.evolveum.midpoint.provisioning.api.LiveSyncOptions;
import com.evolveum.midpoint.provisioning.api.LiveSyncTokenStorage;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.repo.common.activity.run.buckets.ItemDefinitionProvider;
import com.evolveum.midpoint.schema.processor.ResourceObjectDefinition;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.schema.ResourceShadowDiscriminator;
import com.evolveum.midpoint.schema.processor.ResourceObjectClassDefinition;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;

import java.util.Collection;

/**
 * Specifies the object class (on given resource) against which a synchronization will be executed.
 *
 * We keep the following:
 *
 * - resolved resource,
 * - resolved object class definition (refined or raw),
 * - kind & intent (if specified).
 *
 * The role of kind and intent is indirect: they are used only for {@link #getCoords()} and {@link #getObjectFilter()} methods.
 *
 * Note that the object class handling in synchronization task is currently quite counter-intuitive.
 * Please see {@link SyncTaskHelper} and MID-7470 for more information.
 */
@SuppressWarnings("FieldCanBeLocal")
public class ResourceObjectClass implements DebugDumpable {

    /**
     * The resolved resource.
     */
    @NotNull public final ResourceType resource;

    /**
     * Definition of selected object class - determined directly by name or indirectly by kind and intent.
     * (It is null if the operation should be executed against all object classes.)
     *
     * The definition can be raw or refined. The {@link #createBareQuery()} method returns different queries in these cases.
     */
    @Nullable private final ResourceObjectDefinition resourceObjectDefinition;

    /** Shadow kind - if specified. */
    @Nullable private final ShadowKindType kind;

    /** Shadow intent - if specified. */
    @Nullable private final String intent;

    ResourceObjectClass(
            @NotNull ResourceType resource,
            @Nullable ResourceObjectDefinition resourceObjectDefinition,
            @Nullable ShadowKindType kind,
            @Nullable String intent) {
        this.resource = resource;
        this.kind = kind;
        this.intent = intent;
        this.resourceObjectDefinition = resourceObjectDefinition;
    }

    @Override
    public String toString() {
        return "ResourceObjectClass{" +
                ", resource=" + resource +
                ", kind=" + kind +
                ", intent=" + intent +
                ", objectClassDefinition=" + resourceObjectDefinition +
                '}';
    }

    public @NotNull ResourceShadowDiscriminator getCoords() {
        return new ResourceShadowDiscriminator(resource.getOid(), kind, intent,
                resourceObjectDefinition != null ? resourceObjectDefinition.getTypeName() : null);
    }

    public @NotNull ResourceType getResource() {
        return resource;
    }

    public @NotNull ObjectReferenceType getResourceRef() {
        return ObjectTypeUtil.createObjectRef(resource);
    }

    public @Nullable ResourceObjectDefinition getResourceObjectDefinition() {
        return resourceObjectDefinition;
    }

    public @NotNull ResourceObjectDefinition getResourceObjectDefinitionRequired() {
        return requireNonNull(resourceObjectDefinition);
    }

    public @NotNull QName getObjectClassName() {
        return getResourceObjectDefinitionRequired().getTypeName();
    }

    @Nullable
    private QName getObjectClassNameIfKnown() {
        return resourceObjectDefinition != null ?
                resourceObjectDefinition.getObjectClassName() : null;
    }

    public @NotNull SynchronizationObjectsFilterImpl getObjectFilter() {
        return new SynchronizationObjectsFilterImpl(getObjectClassNameIfKnown(), kind, intent);
    }

    public String getContextDescription() {
        return String.valueOf(resource); // TODO something more human friendly
    }

    public String getResourceOid() {
        return resource.getOid();
    }

    public void checkResourceUp() {
        if (ResourceTypeUtil.getLastAvailabilityStatus(resource) != AvailabilityStatusType.UP) {
            throw new IllegalStateException("Resource has to have value of last availability status on UP");
        }
    }

    /**
     * Creates a query covering the specified resource + object class, or resource + kind + intent.
     * (Depending on whether provided {@link #resourceObjectDefinition} is raw or refined.) See:
     *
     * * {@link ResourceObjectClassDefinition#createShadowSearchQuery(String)}
     *
     * The query is interpreted by provisioning module - see:
     *
     * * {@link ProvisioningService#searchObjects(Class, ObjectQuery, Collection, Task, OperationResult)},
     * * {@link ProvisioningService#synchronize(ResourceShadowDiscriminator, LiveSyncOptions, LiveSyncTokenStorage,
     * LiveSyncEventHandler, Task, OperationResult)},
     *
     * The handling of kind/intent pair is quite straightforward. But when using object class name only,
     * one should be careful, because the provisioning module will internally search for the default refined definition
     * with the given object class name. See also MID-7470.
     */
    @NotNull ObjectQuery createBareQuery() throws SchemaException {
        return getResourceObjectDefinitionRequired()
                .createShadowSearchQuery(getResourceOid());
    }

    /**
     * Returns {@link ItemDefinitionProvider} for bucketing over objects in this object class.
     */
    public ItemDefinitionProvider createItemDefinitionProvider() {
        if (resourceObjectDefinition != null) {
            return ItemDefinitionProvider.forResourceObjectAttributes(resourceObjectDefinition);
        } else {
            // Should never occur
            return null;
        }
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = DebugUtil.createTitleStringBuilderLn(getClass(), indent);
        DebugUtil.debugDumpWithLabelLn(sb, "resource", String.valueOf(resource), indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "objectClassDefinition", String.valueOf(resourceObjectDefinition), indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "kind", kind, indent + 1);
        DebugUtil.debugDumpWithLabel(sb, "intent", intent, indent + 1);
        return sb.toString();
    }
}

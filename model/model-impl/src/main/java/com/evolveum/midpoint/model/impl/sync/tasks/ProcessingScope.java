/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.sync.tasks;

import static com.evolveum.midpoint.schema.result.OperationResultStatus.FATAL_ERROR;
import static com.evolveum.midpoint.task.api.TaskRunResult.TaskRunResultStatus.PERMANENT_ERROR;

import static java.util.Objects.requireNonNull;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.provisioning.api.LiveSyncEventHandler;
import com.evolveum.midpoint.provisioning.api.LiveSyncOptions;
import com.evolveum.midpoint.provisioning.api.LiveSyncTokenStorage;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.repo.common.activity.run.ActivityRunException;
import com.evolveum.midpoint.repo.common.activity.run.buckets.ItemDefinitionProvider;
import com.evolveum.midpoint.schema.ResourceOperationCoordinates;
import com.evolveum.midpoint.schema.processor.*;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;

import java.util.Collection;

/**
 * Specifies the basic scope of synchronization (or potentially other resource-related) activity, usually given
 * by [resource, kind, intent, object class name] tuple. Any custom filtering is not represented here.
 *
 * We keep the following:
 *
 * - resolved resource,
 * - resolved object type or class definition,
 * - original kind, intent, and object class name values (if specified).
 *
 * A special case is the scope for a single shadow (see {@link #of(ResourceType, ShadowType)}). In this case we resolve
 * the definition but keep kind, intent, and object class unspecified. We don't expect any search or similar operation
 * to be invoked based on these values.
 *
 * See also:
 *
 * - {@link SyncTaskHelper}
 * - MID-7470
 */
@SuppressWarnings("FieldCanBeLocal")
public class ProcessingScope implements DebugDumpable {

    private static final Trace LOGGER = TraceManager.getTrace(ProcessingScope.class);

    /**
     * The resolved resource.
     */
    @NotNull public final ResourceType resource;

    /**
     * Definition of selected object type or class - derived from kind/intent/objectclass values.
     * (It is null if the operation is executed against the whole resource.)
     */
    @Nullable private final ResourceObjectDefinition resourceObjectDefinition;

    /** Shadow kind - if specified. */
    @Nullable private final ShadowKindType kind;

    /** Shadow intent - if specified. */
    @Nullable private final String intent;

    /** Object class name - if specified. */
    @Nullable private final QName objectClassName;

    private ProcessingScope(
            @NotNull ResourceType resource,
            @Nullable ResourceObjectDefinition resourceObjectDefinition,
            @Nullable ShadowKindType kind,
            @Nullable String intent,
            @Nullable QName objectClassName) {
        this.resource = resource;
        this.resourceObjectDefinition = resourceObjectDefinition;
        this.kind = kind;
        this.intent = intent;
        this.objectClassName = objectClassName;
    }

    public static ProcessingScope of(
            @NotNull ResourceType resource,
            @NotNull ResourceObjectSetType resourceObjectSet)
            throws ActivityRunException {

        ShadowKindType kind = resourceObjectSet.getKind();
        String intent = resourceObjectSet.getIntent();
        QName objectClassName = resourceObjectSet.getObjectclass();

        ResourceObjectDefinition definition;
        try {
            definition = ResourceSchemaUtil.findDefinitionForBulkOperation(resource, kind, intent, objectClassName);
        } catch (CommonException e) {
            throw new ActivityRunException(
                    "Couldn't determine object definition for " + kind + "/" + intent + "/" + objectClassName + " on " + resource,
                    FATAL_ERROR, PERMANENT_ERROR, e);
        }

        if (definition == null) {
            LOGGER.debug("Processing all object classes");
        }

        return new ProcessingScope(resource, definition, kind, intent, objectClassName);
    }

    /** See the note in class-level javadoc. */
    public static ProcessingScope of(
            @NotNull ResourceType resource,
            @NotNull ShadowType shadow) throws ActivityRunException, SchemaException {
        ResourceSchema resourceSchema = getCompleteSchema(resource);
        ResourceObjectDefinition definition = resourceSchema.findDefinitionForShadow(shadow);
        return new ProcessingScope(resource, definition, null, null, null);
    }

    private static @NotNull ResourceSchema getCompleteSchema(ResourceType resource) throws ActivityRunException {
        try {
            return MiscUtil.requireNonNull(
                    ResourceSchemaFactory.getCompleteSchema(resource, LayerType.MODEL),
                    () -> new ActivityRunException(
                            "No schema present. Probably some configuration problem.", FATAL_ERROR, PERMANENT_ERROR));
        } catch (ConfigurationException | SchemaException e) {
            throw new ActivityRunException("Error during processing resource schema", FATAL_ERROR, PERMANENT_ERROR, e);
        }
    }

    @Override
    public String toString() {
        return "ProcessingScope{" +
                ", resource=" + resource +
                ", definition=" + resourceObjectDefinition +
                ", kind=" + kind +
                ", intent=" + intent +
                ", objectClassName=" + objectClassName +
                '}';
    }

    public @NotNull ResourceOperationCoordinates getCoords() {
        return ResourceOperationCoordinates.of(resource.getOid(), kind, intent, objectClassName);
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

    private @NotNull ResourceObjectDefinition getResourceObjectDefinitionRequired() {
        return requireNonNull(resourceObjectDefinition);
    }

    public @NotNull QName getResolvedObjectClassName() {
        return getResourceObjectDefinitionRequired().getTypeName();
    }

    public @NotNull PostSearchFilter getPostSearchFilter() {
        return new PostSearchFilterImpl();
    }

    public @NotNull ObjectFilter getKindIntentFilter() {
        var builder = PrismContext.get().queryFor(ShadowType.class).all();
        if (kind != null) {
            builder = builder.and().item(ShadowType.F_KIND).eq(kind);
        }
        if (intent != null) {
            builder = builder.and().item(ShadowType.F_INTENT).eq(intent);
        }
        return builder.buildFilter();
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
     * Creates a query covering the specified resource + [kind] + [intent] + [object class], exactly as requested by the activity.
     *
     * The query is interpreted by provisioning module - see:
     *
     * * {@link ProvisioningService#searchObjects(Class, ObjectQuery, Collection, Task, OperationResult)},
     * * {@link ProvisioningService#synchronize(ResourceOperationCoordinates, LiveSyncOptions, LiveSyncTokenStorage,
     * LiveSyncEventHandler, Task, OperationResult)}.
     *
     * We don't try to outsmart the provisioning module - so we simply pass all parameters just like we obtained them.
     * Provisioning should be capable to resolve the query correctly.
     */
    @NotNull ObjectQuery createBareQuery() {
        var q = PrismContext.get().queryFor(ShadowType.class)
                .item(ShadowType.F_RESOURCE_REF).ref(getResourceOid());
        if (kind != null) {
            q = q.and().item(ShadowType.F_KIND).eq(kind);
        }
        if (intent != null) {
            q = q.and().item(ShadowType.F_INTENT).eq(intent);
        }
        if (objectClassName != null) {
            q = q.and().item(ShadowType.F_OBJECT_CLASS).eq(objectClassName);
        }
        return q.build();
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

    /**
     * Specifies which objects are to be synchronized.
     * Currently uses combination of object class, kind, and intent. (No custom filters yet.)
     *
     * TODO shouldn't we take object class inheritance (subtyping) into account?
     */
    @Experimental
    public class PostSearchFilterImpl implements PostSearchFilter {

        @Override
        public boolean matches(@NotNull PrismObject<ShadowType> shadow) {
            return matchesObjectClassName(shadow) && matchesKind(shadow) && matchesIntent(shadow);
        }

        private boolean matchesObjectClassName(@NotNull PrismObject<ShadowType> shadow) {
            return objectClassName == null
                    || QNameUtil.match(objectClassName, shadow.asObjectable().getObjectClass());
        }

        /**
         * Does the shadow kind match?
         */
        private boolean matchesKind(PrismObject<ShadowType> shadow) {
            return kind == null || ShadowUtil.getKind(shadow) == kind;
        }

        /**
         * Does the shadow intent match?
         */
        private boolean matchesIntent(PrismObject<ShadowType> shadow) {
            return intent == null || intent.equals(ShadowUtil.getIntent(shadow));
        }
    }
}

/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.processor;

import static com.evolveum.midpoint.util.MiscUtil.stateCheck;

import com.evolveum.midpoint.util.DOMUtil;

import com.google.common.base.Preconditions;
import org.jetbrains.annotations.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.impl.schema.SchemaParsingUtil;
import com.evolveum.midpoint.schema.internals.InternalCounters;
import com.evolveum.midpoint.schema.internals.InternalMonitor;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LayerType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

/** The official place where resource schemas are created. */
public class ResourceSchemaFactory {

    private static final String USER_DATA_KEY_NATIVE_SCHEMA = ResourceSchema.class.getName() + ".nativeSchema";
    private static final String USER_DATA_KEY_COMPLETE_SCHEMA = ResourceSchema.class.getName() + ".completeSchema";

    public static @Nullable CompleteResourceSchema getCompleteSchema(@NotNull ResourceType resource)
            throws SchemaException, ConfigurationException {
        return getCompleteSchema(resource.asPrismObject());
    }

    /**
     * We assume that missing schema is a configuration (not schema) problem.
     */
    public static @NotNull CompleteResourceSchema getCompleteSchemaRequired(@NotNull ResourceType resource)
            throws ConfigurationException, SchemaException {
        return requireSchemaPresent(
                getCompleteSchema(resource),
                resource);
    }

    public static @NotNull CompleteResourceSchema getCompleteSchemaRequired(@NotNull PrismObject<ResourceType> resource)
            throws ConfigurationException, SchemaException {
        return getCompleteSchemaRequired(resource.asObjectable());
    }

    private static <S> @NotNull S requireSchemaPresent(S schema, @NotNull ResourceType resource)
            throws ConfigurationException {
        return MiscUtil.requireNonNull(
                schema,
                () -> new ConfigurationException("No schema in " + resource + ". A configuration problem?"));
    }

    public static CompleteResourceSchema getCompleteSchema(ResourceType resourceType, LayerType layer)
            throws SchemaException, ConfigurationException {
        return getCompleteSchema(resourceType.asPrismObject(), layer);
    }

    public static @NotNull ResourceSchema getCompleteSchemaRequired(PrismObject<ResourceType> resource, LayerType layer)
            throws SchemaException, ConfigurationException {
        return getCompleteSchemaRequired(resource.asObjectable(), layer);
    }

    public static @NotNull ResourceSchema getCompleteSchemaRequired(ResourceType resource, LayerType layer)
            throws SchemaException, ConfigurationException {
        return requireSchemaPresent(
                getCompleteSchema(resource.asPrismObject(), layer),
                resource);
    }

    /**
     * Obtains refined schema for the resource.
     *
     * Returns null if the resource does not contain any (native) schema.
     *
     * If the resource does NOT contain the schema, it must be mutable.
     *
     * Returned schema is immutable.
     */
    public static CompleteResourceSchema getCompleteSchema(@NotNull PrismObject<ResourceType> resource)
            throws SchemaException, ConfigurationException {
        Preconditions.checkNotNull(resource, "Resource must not be null");

        CompleteResourceSchema existingCompleteSchema = getExistingCompleteSchema(resource);
        if (existingCompleteSchema != null) {
            return existingCompleteSchema;
        } else {
            stateCheck(!resource.isImmutable(), "Trying to setup parsed schema on immutable resource: %s", resource);
            CompleteResourceSchema completeSchema = parseCompleteSchema(resource.asObjectable());
            if (completeSchema != null) {
                completeSchema.freeze();
            }
            resource.setUserData(USER_DATA_KEY_COMPLETE_SCHEMA, completeSchema);
            return completeSchema;
        }
    }

    /** Removes schemas that are attached to the particular prism object using user data. */
    public static void deleteCachedSchemas(PrismObject<ResourceType> resource) {
        resource.setUserData(USER_DATA_KEY_COMPLETE_SCHEMA, null);
        resource.setUserData(USER_DATA_KEY_NATIVE_SCHEMA, null);
    }

    private static CompleteResourceSchema getExistingCompleteSchema(PrismObject<ResourceType> resource) {
        Object userDataEntry = resource.getUserData(USER_DATA_KEY_COMPLETE_SCHEMA);
        if (userDataEntry != null) {
            if (userDataEntry instanceof CompleteResourceSchema completeResourceSchema) {
                return completeResourceSchema;
            } else {
                throw new IllegalStateException(
                        "Expected ResourceSchema under user data key %s in %s, but got %s".formatted(
                                USER_DATA_KEY_COMPLETE_SCHEMA, resource, userDataEntry.getClass()));
            }
        } else {
            return null;
        }
    }

    /** Returned schema is immutable. FIXME there is a lot of cloning if layer != MODEL! */
    public static CompleteResourceSchema getCompleteSchema(PrismObject<ResourceType> resource, LayerType layer)
            throws SchemaException, ConfigurationException {
        CompleteResourceSchema schema = getCompleteSchema(resource);
        if (schema != null) {
            return schema.forLayerImmutable(layer);
        } else {
            return null;
        }
    }

    public static NativeResourceSchema getNativeSchema(@NotNull ResourceType resource) throws SchemaException {
        return getNativeSchema(
                resource.asPrismObject());
    }

    public static NativeResourceSchema getNativeSchemaRequired(ResourceType resource) throws SchemaException, ConfigurationException {
        return requireSchemaPresent(
                getNativeSchema(resource),
                resource);
    }

    public static NativeResourceSchema getNativeSchemaRequired(PrismObject<ResourceType> resource) throws SchemaException, ConfigurationException {
        return getNativeSchemaRequired(resource.asObjectable());
    }

    public static BareResourceSchema getBareSchema(@NotNull PrismObject<ResourceType> resource) throws SchemaException {
        return nativeToBare(
                getNativeSchema(resource));
    }

    public static BareResourceSchema getBareSchema(@NotNull ResourceType resource) throws SchemaException {
        return nativeToBare(
                getNativeSchema(resource));
    }

    /**
     * Obtains "raw" schema for the resource, i.e. the one without `schemaHandling` and similar configuration.
     *
     * If the resource does NOT contain the schema, it must be mutable.
     *
     * The returned schema is immutable.
     */
    public static NativeResourceSchema getNativeSchema(@NotNull PrismObject<ResourceType> resource) throws SchemaException {
        Element resourceXsdSchemaElement = ResourceTypeUtil.getResourceXsdSchemaElement(resource);
        if (resourceXsdSchemaElement == null) {
            return null;
        }

        // Synchronization here is a workaround for MID-5648. We need to synchronize parsing here because of DOM access even
        // before DomToSchemaProcessor (where global synchronization is done) comes into play.
        synchronized (resourceXsdSchemaElement) {
            Object cachedNativeSchema = resource.getUserData(USER_DATA_KEY_NATIVE_SCHEMA);
            if (cachedNativeSchema != null) {
                if (cachedNativeSchema instanceof NativeResourceSchema schema) {
                    schema.checkImmutable();
                    return schema;
                } else {
                    throw new IllegalStateException(
                            "Expected %s under user data key %s in %s, but got %s".formatted(
                                    NativeResourceSchema.class, USER_DATA_KEY_NATIVE_SCHEMA,
                                    resource, cachedNativeSchema.getClass()));
                }
            } else {
                stateCheck(!resource.isImmutable(), "Trying to set parsed schema on immutable resource: %s", resource);
                InternalMonitor.recordCount(InternalCounters.RESOURCE_SCHEMA_PARSE_COUNT);
                NativeResourceSchema parsedSchema =
                        parseNativeSchema(resourceXsdSchemaElement, "resource schema of " + resource);
                parsedSchema.freeze();
                resource.setUserData(USER_DATA_KEY_NATIVE_SCHEMA, parsedSchema);
                return parsedSchema;
            }
        }
    }

    @VisibleForTesting
    public static boolean hasParsedSchema(ResourceType resource) {
        return resource.asPrismObject().getUserData(USER_DATA_KEY_NATIVE_SCHEMA) != null;
    }

    /**
     * Executes the real parsing. Returns complete schema (raw + refined).
     *
     * Normally internal to this class, but may be called externally from the test code.
     *
     * DO NOT call it directly from the production code. The returned schema is NOT immutable here, but we want to ensure
     * immutability throughout the running system. Use {@link #getCompleteSchema(PrismObject)} instead.
     */
    @VisibleForTesting
    public static CompleteResourceSchema parseCompleteSchema(ResourceType resource) throws SchemaException, ConfigurationException {
        return parseCompleteSchema(resource, ResourceSchemaFactory.getNativeSchema(resource));
    }

    /** Parses the complete schema from the provided raw schema plus definitions in the resource. */
    @Contract("_, null -> null; _, !null -> !null")
    public static CompleteResourceSchema parseCompleteSchema(@NotNull ResourceType resource, NativeResourceSchema nativeSchema)
            throws SchemaException, ConfigurationException {
        if (nativeSchema != null) {
            return ResourceSchemaParser.parseComplete(resource, nativeSchema);
        } else {
            return null;
        }
    }

    public static @NotNull NativeResourceSchema parseNativeSchema(@NotNull Element sourceXsdElement, String description)
            throws SchemaException {
        var schema = new NativeResourceSchemaImpl();
        SchemaParsingUtil.parse(schema, sourceXsdElement, true, description, false);
        schema.computeReferenceTypes();
        schema.freeze();
        return schema;
    }

    @TestOnly
    public static @NotNull BareResourceSchema parseNativeSchemaAsBare(@NotNull Document sourceXsdDocument) throws SchemaException {
        return parseNativeSchemaAsBare(DOMUtil.getFirstChildElement(sourceXsdDocument));
    }

    @TestOnly
    public static @NotNull BareResourceSchema parseNativeSchemaAsBare(@NotNull Element sourceXsdElement) throws SchemaException {
        return nativeToBare(
                parseNativeSchema(sourceXsdElement, "test"));
    }

    @Contract("null -> null; !null -> !null")
    public static BareResourceSchema nativeToBare(@Nullable NativeResourceSchema nativeResourceSchema) throws SchemaException {
        return nativeResourceSchema != null ?
                ResourceSchemaParser.parseBare(nativeResourceSchema) :
                null;
    }
}

/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.processor;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.Referencable;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.schema.SchemaLookup;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.util.annotation.Experimental;

import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConstructionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Keeps {@link CompleteResourceSchema} objects.
 */
@Component
@Experimental // unfinished!
public class ResourceSchemaRegistry implements SchemaLookup.Based {


    private final List<Function<String, PrismObject<ResourceType>> > resourceObjectLoaders = new ArrayList<>();
    private final Map<String, CompleteResourceSchema> schemaMap = new ConcurrentHashMap<>();

    public void putSchema(@NotNull String oid, @Nullable CompleteResourceSchema completeSchema) {
        schemaMap.put(oid, completeSchema);
    }

    public @Nullable ResourceObjectDefinition getDefinitionForShadow(@NotNull ShadowType shadow) throws SchemaException {
        var resourceOid = ShadowUtil.getResourceOid(shadow);
        if (resourceOid == null) {
            return null;
        }
        var resourceSchema = getResourceSchema(resourceOid);
        if (resourceSchema == null) {
            return null;
        }
        return resourceSchema.findDefinitionForShadow(shadow);
    }

    public @Nullable ResourceObjectDefinition getDefinitionForConstruction(@NotNull ConstructionType construct) {
        var resourceOid = Referencable.getOid(construct.getResourceRef());
        if (resourceOid == null) {
            return null;
        }
        var resourceSchema = getResourceSchema(resourceOid);
        if (resourceSchema == null) {
            return null;
        }
        return resourceSchema.findDefinitionForConstruction(construct);
    }

    public @Nullable ResourceObjectDefinition getDefinitionForKindIntent(String resourceOid, ShadowKindType kind, String intent) throws SchemaException {
        if (resourceOid == null) {
            return null;
        }
        var resourceSchema = getResourceSchema(resourceOid);
        if (resourceSchema == null) {
            return null;
        }
        if (kind == null) {
            kind = ShadowKindType.ACCOUNT;
        }
        try {
            return ResourceSchemaUtil.findObjectDefinitionPrecisely(resourceSchema, kind, intent, null, resourceOid);
        } catch (ConfigurationException e) {
            return null;
        }
    }

    public @Nullable CompleteResourceSchema getResourceSchema(@NotNull String resourceOid) {
        var cached = schemaMap.get(resourceOid);
        if (cached != null) {
            return cached;
        }
        return tryToLoadSchema(resourceOid);
    }

    public @Nullable ResourceObjectDefinition getDefinitionForQuery(ObjectQuery query) {
        try {
            var coordinates = ObjectQueryUtil.getOperationCoordinates(query);
            coordinates.checkNotUnknown();
            coordinates.checkNotResourceScoped();

            var resourceSchema = getResourceSchema(coordinates.getResourceOid()) ;

            if (resourceSchema != null) {
                return ResourceSchemaUtil.findObjectDefinitionPrecisely(resourceSchema, coordinates.getKind(),
                        coordinates.getIntent(), coordinates.getObjectClassName(), null);
            }


        } catch (Exception e) {
            // Handle error
        }

        return null;
    }

    public ObjectQuery tryToNormalizeQuery(ObjectQuery query) {
        var def = getDefinitionForQuery(query);
        if (def != null) {
            query = ShadowsNormalizationUtil.transformQueryValues(query, def);
        }
        return query;
    }

    private CompleteResourceSchema tryToLoadSchema(String resourceOid) {

        CompleteResourceSchema schema = null;
        for (var loader : resourceObjectLoaders) {
            var resourceObj = loader.apply(resourceOid);
            try {
                if (resourceObj != null) {
                    schema = ResourceSchemaFactory.getCompleteSchema(resourceObj);
                }
                if (schema != null) {
                    putSchema(resourceOid, schema);
                    return schema;
                }
            } catch (SchemaException | ConfigurationException e) {
                // FIXME: We should do something here probably?
            }
        }
        return null;
    }

    public void registerResourceObjectLoader(Function<String, PrismObject<ResourceType>> schemaLoader) {
        resourceObjectLoaders.add(schemaLoader);
    }

    public void unregisterResourceObjectLoader(Function<String, PrismObject<ResourceType>> schemaLoader) {
        resourceObjectLoaders.remove(schemaLoader);
    }
}

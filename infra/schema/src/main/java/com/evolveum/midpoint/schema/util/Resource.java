/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.util;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.builder.S_ItemEntry;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.ItemDefinitionResolver;
import com.evolveum.midpoint.prism.query.builder.S_FilterExit;
import com.evolveum.midpoint.prism.query.builder.S_MatchingRuleEntry;
import com.evolveum.midpoint.schema.processor.*;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;
import java.util.Collection;
import java.util.List;

/**
 * "One stop shop" for accessing various aspects of a resource (defined by {@link ResourceType} object).
 *
 * Currently used for:
 *
 * - accessing the schema,
 * - creating queries.
 *
 * Resides as low as in `schema` module, because it is used also in AbstractIntegrationTest class.
 * This limits the functionality provided, though.
 *
 * Open questions:
 *
 * - Should we change the name to reflect that it is more schema-oriented?
 * - Should we merge this class e.g. with {@link ResourceSchema}? Or {@link ResourceTypeUtil}?
 * - Should we add more functionality here and move back to `provisioning-api`?
 *
 * HIGHLY EXPERIMENTAL (maybe not a good idea at all)
 */
@Experimental
public class Resource {

    @NotNull private final ResourceType resourceBean;

    private Resource(@NotNull ResourceType resourceBean) {
        this.resourceBean = resourceBean;
    }

    public static Resource of(@NotNull ResourceType resourceBean) {
        return new Resource(resourceBean);
    }

    public static Resource of(@NotNull PrismObject<ResourceType> resourceObject) {
        return new Resource(resourceObject.asObjectable());
    }

    public @Nullable ResourceSchema getRawSchema() throws SchemaException {
        return ResourceSchemaFactory.getRawSchema(resourceBean);
    }

    public @NotNull ResourceSchema getRawSchemaRequired() throws SchemaException, ConfigurationException {
        return ResourceSchemaFactory.getRawSchemaRequired(resourceBean);
    }

    public @Nullable ResourceSchema getCompleteSchema() throws SchemaException, ConfigurationException {
        return ResourceSchemaFactory.getCompleteSchema(resourceBean);
    }

    public @NotNull ResourceSchema getCompleteSchemaRequired() throws SchemaException, ConfigurationException {
        return ResourceSchemaFactory.getCompleteSchemaRequired(resourceBean);
    }

    public @NotNull Collection<ResourceObjectTypeDefinition> getObjectTypeDefinitions()
            throws SchemaException, ConfigurationException {
        ResourceSchema schema = getCompleteSchema();
        return schema != null ? schema.getObjectTypeDefinitions() : List.of();
    }

    public @NotNull S_MatchingRuleEntry queryFor(@NotNull ResourceObjectTypeIdentification typeIdentification)
            throws SchemaException, ConfigurationException {
        return queryFor(typeIdentification.getKind(), typeIdentification.getIntent());
    }

    public @NotNull S_MatchingRuleEntry queryFor(@NotNull ShadowKindType kind, @NotNull String intent)
            throws SchemaException, ConfigurationException {
        ResourceObjectDefinition objectDefinition =
                getCompleteSchemaRequired()
                        .findObjectDefinitionRequired(kind, intent);
        return queryFor(objectDefinition)
                .and().item(ShadowType.F_KIND).eq(kind)
                .and().item(ShadowType.F_INTENT).eq(intent);
    }

    public @NotNull S_MatchingRuleEntry queryFor(@NotNull QName objectClassName)
            throws SchemaException, ConfigurationException {
        ResourceObjectDefinition objectDefinition =
                getCompleteSchemaRequired()
                        .findObjectClassDefinitionRequired(objectClassName);
        return queryFor(objectDefinition)
                .and().item(ShadowType.F_OBJECT_CLASS).eq(objectClassName);
    }

    // Beware, no kind/intent/OC filter is set here. Must be private.
    private S_FilterExit queryFor(@NotNull ResourceObjectDefinition objectDefinition) {
        return PrismContext.get().queryFor(ShadowType.class, new ResourceItemDefinitionResolver(objectDefinition))
                .item(ShadowType.F_RESOURCE_REF).ref(resourceBean.getOid());
    }

    public @NotNull S_ItemEntry deltaFor(@NotNull QName objectClassName)
            throws SchemaException, ConfigurationException {
        return deltaFor(
                getCompleteSchemaRequired()
                        .findObjectClassDefinitionRequired(objectClassName));
    }

    private S_ItemEntry deltaFor(@NotNull ResourceObjectDefinition objectDefinition) throws SchemaException {
        return PrismContext.get().deltaFor(ShadowType.class, new ResourceItemDefinitionResolver(objectDefinition));
    }

    private static class ResourceItemDefinitionResolver implements ItemDefinitionResolver {

        @NotNull private final ResourceObjectDefinition definition;

        private ResourceItemDefinitionResolver(@NotNull ResourceObjectDefinition definition) {
            this.definition = definition;
        }

        @Override
        public ItemDefinition<?> findItemDefinition(@NotNull Class<? extends Containerable> type, @NotNull ItemPath itemPath) {
            if (!ShadowType.class.isAssignableFrom(type)
                    || !itemPath.startsWith(ShadowType.F_ATTRIBUTES)
                    || itemPath.size() != 2) {
                return null;
            }
            ItemName attrName = itemPath.rest().firstToNameOrNull();
            if (attrName == null) {
                return null;
            } else {
                return definition.findAttributeDefinition(attrName);
            }
        }
    }
}

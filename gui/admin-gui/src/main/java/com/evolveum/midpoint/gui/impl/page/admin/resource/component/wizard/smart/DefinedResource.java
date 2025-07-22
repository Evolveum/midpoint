/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.smart;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeIdentification;
import com.evolveum.midpoint.schema.processor.ResourceSchemaFactory;
import com.evolveum.midpoint.schema.util.Resource;
import com.evolveum.midpoint.schema.util.ResourceObjectTypeDefinitionTypeUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectTypeDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SchemaHandlingType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;

/** Wraps a {@link ResourceType}, providing some auxiliary methods. */
public class DefinedResource implements Serializable {

    private ResourceType resource;

    DefinedResource(ResourceType resource) {
        this.resource = resource;
    }

    public @Nullable SchemaHandlingType getSchemaHandling() {
        return resource.getSchemaHandling();
    }

    ResourceType getResourceBean() {
        return resource;
    }

    public Resource getResource() {
        return Resource.of(resource);
    }

    void replaceObjectTypeDefinition(ResourceObjectTypeIdentification selectedTypeId, ResourceObjectTypeDefinitionType newDefinition) {
        var bean = findObjectTypeDefinitionBean(selectedTypeId);
        if (bean != null) {
            resource.getSchemaHandling().getObjectType().remove(bean);
        }
        if (newDefinition == null) {
            return;
        }
        if (resource.getSchemaHandling() == null) {
            resource.setSchemaHandling(new SchemaHandlingType());
        }
        resource.getSchemaHandling().getObjectType().add(newDefinition);
    }

    /** Returns live (i.e. directly updatable) bean. */
    @Nullable ResourceObjectTypeDefinitionType findObjectTypeDefinitionBean(ResourceObjectTypeIdentification typeId) {
        var schemaHandling = resource.getSchemaHandling();
        if (schemaHandling == null) {
            return null;
        }
        for (ResourceObjectTypeDefinitionType definitionBean : schemaHandling.getObjectType()) {
            if (ResourceObjectTypeDefinitionTypeUtil.matches(definitionBean, typeId.getKind(), typeId.getIntent())) {
                return definitionBean;
            }
        }
        return null;
    }

    public String getOid() {
        return resource.getOid();
    }

    private void afterChanged() {
        ResourceSchemaFactory.deleteCachedSchemas(resource.asPrismObject());
    }

    public void replace(@NotNull PrismObject<ResourceType> newObject) {
        resource = newObject.asObjectable();
        afterChanged();
    }

    public void setSchemaHandling(SchemaHandlingType schemaHandling) {
        resource.setSchemaHandling(schemaHandling);
        afterChanged();
    }
}

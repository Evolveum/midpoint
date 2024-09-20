/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.dataModel.model;

import java.util.Objects;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.schema.processor.ResourceObjectTypeDefinition;
import com.evolveum.midpoint.schema.processor.ShadowSimpleAttributeDefinition;

import com.evolveum.midpoint.schema.processor.ResourceObjectDefinition;
import com.evolveum.midpoint.schema.processor.ResourceSchema;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.model.impl.dataModel.DataModel;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;

public class ResourceDataItem extends DataItem {

    @NotNull private final DataModel ctx;
    @NotNull private final String resourceOid;
    @NotNull private final ShadowKindType kind;
    @NotNull private final String intent; // TODO or more intents?
    @NotNull private final ItemPath itemPath;

    private ResourceSchema refinedResourceSchema;
    private ResourceObjectDefinition objectDefinition;
    private ShadowSimpleAttributeDefinition<?> attributeDefinition;

    public ResourceDataItem(
            @NotNull DataModel ctx,
            @NotNull String resourceOid,
            @NotNull ShadowKindType kind,
            @NotNull String intent,
            ResourceSchema refinedResourceSchema,
            ResourceObjectTypeDefinition refinedDefinition,
            @NotNull ItemPath itemPath) {
        this.ctx = ctx;
        this.resourceOid = resourceOid;
        this.kind = kind;
        this.intent = intent;
        this.itemPath = itemPath;
        if (itemPath.lastName() == null) {
            throw new IllegalArgumentException("Wrong itemPath (have a named segment): " + itemPath);
        }
        this.refinedResourceSchema = refinedResourceSchema;
        this.objectDefinition = refinedDefinition;
    }

    @NotNull
    public String getResourceOid() {
        return resourceOid;
    }

    @NotNull
    public ShadowKindType getKind() {
        return kind;
    }

    @NotNull
    public String getIntent() {
        return intent;
    }

    @NotNull
    public QName getLastItemName() {
        return itemPath.lastName();
    }

    @NotNull
    public ItemPath getItemPath() {
        return itemPath;
    }

    public ResourceSchema getRefinedResourceSchema() {
        if (refinedResourceSchema == null) {
            refinedResourceSchema = ctx.getRefinedResourceSchema(resourceOid);
        }
        return refinedResourceSchema;
    }

    public ResourceObjectDefinition getObjectDefinition() {
        if (objectDefinition == null) {
            ResourceSchema schema = getRefinedResourceSchema();
            if (schema != null) {
                objectDefinition = schema.findObjectDefinition(kind, intent);
            }
        }
        return objectDefinition;
    }

    public ShadowSimpleAttributeDefinition<?> getAttributeDefinition() {
        if (attributeDefinition == null) {
            ResourceObjectDefinition def = getObjectDefinition();
            if (def != null && itemPath.size() == 1) {
                attributeDefinition = def.findSimpleAttributeDefinition(getLastItemName());
            }
        }
        return attributeDefinition;
    }

    public void setAttributeDefinition(ShadowSimpleAttributeDefinition<?> attributeDefinition) {
        this.attributeDefinition = attributeDefinition;
    }

    @Override
    public String toString() {
        return "ResourceDataItem{" +
                "resourceOid='" + resourceOid + '\'' +
                ", kind=" + kind +
                ", intent='" + intent + '\'' +
                ", path=" + itemPath +
                '}';
    }

    public QName getObjectClassName() {
        return objectDefinition != null ? objectDefinition.getTypeName() : null;
    }

    public boolean matches(String resourceOid, ShadowKindType kind, String intent, QName objectClassName,
            ItemPath path) {
        return this.resourceOid.equals(resourceOid)
                && this.kind == kind
                && Objects.equals(this.intent, intent)
                && QNameUtil.match(getObjectClassName(), objectClassName)
                && this.itemPath.equivalent(path);
    }
}

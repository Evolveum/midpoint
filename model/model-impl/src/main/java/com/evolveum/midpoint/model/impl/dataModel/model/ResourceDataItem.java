/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.dataModel.model;

import java.util.Objects;
import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.common.refinery.RefinedAttributeDefinition;
import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.model.impl.dataModel.DataModel;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;

/**
 * @author mederly
 */
public class ResourceDataItem extends DataItem {

    @NotNull private final DataModel ctx;
    @NotNull private final String resourceOid;
    @NotNull private final ShadowKindType kind;
    @NotNull private final String intent; // TODO or more intents?
    @NotNull private final ItemPath itemPath;
    private final boolean hasItemDefinition;

    private RefinedResourceSchema refinedResourceSchema;
    private RefinedObjectClassDefinition refinedObjectClassDefinition;
    private RefinedAttributeDefinition<?> refinedAttributeDefinition;

    public ResourceDataItem(@NotNull DataModel ctx, @NotNull String resourceOid, @NotNull ShadowKindType kind,
            @NotNull String intent, RefinedResourceSchema refinedResourceSchema,
            RefinedObjectClassDefinition refinedDefinition, @NotNull ItemPath itemPath) {
        this.ctx = ctx;
        this.resourceOid = resourceOid;
        this.kind = kind;
        this.intent = intent;
        this.itemPath = itemPath;
        if (itemPath.lastName() == null) {
            throw new IllegalArgumentException("Wrong itemPath (have a named segment): " + itemPath);
        }
        this.hasItemDefinition = itemPath.size() == 1; // TODO... TODO what?
        this.refinedResourceSchema = refinedResourceSchema;
        this.refinedObjectClassDefinition = refinedDefinition;
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

    public boolean isHasItemDefinition() {
        return hasItemDefinition;
    }

    public RefinedResourceSchema getRefinedResourceSchema() {
        if (refinedResourceSchema == null) {
            refinedResourceSchema = ctx.getRefinedResourceSchema(resourceOid);
        }
        return refinedResourceSchema;
    }

    public RefinedObjectClassDefinition getRefinedObjectClassDefinition() {
        if (refinedObjectClassDefinition == null) {
            RefinedResourceSchema schema = getRefinedResourceSchema();
            if (schema != null) {
                refinedObjectClassDefinition = schema.getRefinedDefinition(kind, intent);
            }
        }
        return refinedObjectClassDefinition;
    }

    public RefinedAttributeDefinition<?> getRefinedAttributeDefinition() {
        if (refinedAttributeDefinition == null) {
            RefinedObjectClassDefinition def = getRefinedObjectClassDefinition();
            if (def != null && hasItemDefinition) {
                refinedAttributeDefinition = def.findAttributeDefinition(getLastItemName());
            }
        }
        return refinedAttributeDefinition;
    }

    public void setRefinedAttributeDefinition(RefinedAttributeDefinition<?> refinedAttributeDefinition) {
        this.refinedAttributeDefinition = refinedAttributeDefinition;
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
        return refinedObjectClassDefinition != null ? refinedObjectClassDefinition.getTypeName() : null;
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

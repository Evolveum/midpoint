/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.api;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.schema.util.SchemaDebugUtil;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import java.io.Serializable;

import static com.evolveum.midpoint.util.MiscUtil.stateCheck;

/**
 * Describes a change of a specific resource object together with definitions of the source and possibly
 * also other information. This is useful to completely describe a change that was detected on the resource.
 *
 * This object can describe either relative change or new absolute state. In case of relative change the "objectDelta"
 * property will be provided. In case of description of new absolute state the "currentShadow" value will be provided.
 * It may happen that both of them will be provided if both are known (and efficiently detected). In such a case the
 * implementation may choose any one to process.
 *
 * @author Radovan Semancik
 */
public class ResourceObjectShadowChangeDescription implements DebugDumpable, Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * Current "shadowed" resource object. I.e. it is the resource object combined with its repository shadow
     * in a specific way. (Please see the shadows package in provisioning-impl.)
     *
     * It describes the state after the change.
     *
     * Must not be null. If the resource has no read capability, it should be constructed using cached attributes.
     * If the object was deleted, this must be the last state. (Usually, a dead shadow is presented here.)
     *
     * It must exist in repo. The only exception is when the object delta is delete.
     */
    private PrismObject<ShadowType> shadowedResourceObject;

    /**
     * Delta describing change - if known.
     * If present, it must have an OID. There are a lot of consistency checks in the code that watch this.
     */
    private ObjectDelta<ShadowType> objectDelta;

    /**
     * Via what channel did we learn about this change?
     */
    private String sourceChannel;

    /** Related resource. Must be present. */
    private PrismObject<ResourceType> resource;

    /** Is this a simulated operation? TODO reconsider this flag here. */
    private boolean simulate = false;

    /** We want to just clean - i.e. unlink - a dead shadow. TODO reconsider using custom interface for this. */
    private boolean cleanDeadShadow = false;

    /**
     * Identifies (synchronizable) item processing as part of which this change description was generated.
     * This identifier applies only to immediate synchronization of the item. It should *not* be propagated
     * to any related or unrelated processing like the one induced by discovery, error handling or similar
     * activities.
     */
    @Experimental
    private String itemProcessingIdentifier;

    public ObjectDelta<ShadowType> getObjectDelta() {
        return objectDelta;
    }

    public void setObjectDelta(ObjectDelta<ShadowType> objectDelta) {
        this.objectDelta = objectDelta;
    }

    public PrismObject<ShadowType> getShadowedResourceObject() {
        return shadowedResourceObject;
    }

    public void setShadowedResourceObject(PrismObject<ShadowType> shadowedResourceObject) {
        this.shadowedResourceObject = shadowedResourceObject;
    }

    public String getSourceChannel() {
        return sourceChannel;
    }

    public void setSourceChannel(String sourceChannel) {
        this.sourceChannel = sourceChannel;
    }

    public PrismObject<ResourceType> getResource() {
        return resource;
    }

    public void setResource(PrismObject<ResourceType> resource) {
        this.resource = resource;
    }

    public boolean isSimulate() {
        return simulate;
    }

    public void setSimulate(boolean simulate) {
        this.simulate = simulate;
    }

    public boolean isCleanDeadShadow() {
        return cleanDeadShadow;
    }

    public void setCleanDeadShadow(boolean cleanDeadShadow) {
        this.cleanDeadShadow = cleanDeadShadow;
    }

    public String getItemProcessingIdentifier() {
        return itemProcessingIdentifier;
    }

    public void setItemProcessingIdentifier(String itemProcessingIdentifier) {
        this.itemProcessingIdentifier = itemProcessingIdentifier;
    }

    public void checkConsistence() {
        stateCheck(resource != null, "No resource");
        resource.checkConsistence();
        stateCheck(sourceChannel != null, "No source channel");
        if (objectDelta != null) {
            objectDelta.checkConsistence();
        }
        stateCheck(shadowedResourceObject != null, "No shadowed resource object present");
        stateCheck(shadowedResourceObject.getOid() != null, "Shadowed resource object without OID");
        ShadowUtil.checkConsistence(shadowedResourceObject,"shadowed resource object in change notification");
    }

    public boolean isProtected() {
        return ShadowUtil.isProtected(shadowedResourceObject);
    }

    @Override
    public String toString() {
        return "ResourceObjectShadowChangeDescription("
                + "objectDelta=" + objectDelta
                + ", currentShadow=" + SchemaDebugUtil.prettyPrint(shadowedResourceObject)
                + ", sourceChannel=" + sourceChannel
                + ", resource=" + resource
                + ", processing=" + itemProcessingIdentifier
                + (simulate ? " SIMULATE" : "")
                + (cleanDeadShadow ? " CLEAN DEAD SHADOW" : "")
                + ")";
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        SchemaDebugUtil.indentDebugDump(sb, indent);
        sb.append("ResourceObjectShadowChangeDescription(");
        sb.append(sourceChannel);
        sb.append(")\n");

        SchemaDebugUtil.indentDebugDump(sb, indent+1);
        sb.append("resource:");
        if (resource == null) {
            sb.append(" null");
        } else {
            sb.append(resource);
        }

        sb.append("\n");
        SchemaDebugUtil.indentDebugDump(sb, indent+1);

        sb.append("objectDelta:");
        if (objectDelta == null) {
            sb.append(" null");
        } else {
            sb.append("\n");
            sb.append(objectDelta.debugDump(indent+2));
        }
        sb.append("\n");
        SchemaDebugUtil.indentDebugDump(sb, indent+1);

        sb.append("\n");
        SchemaDebugUtil.indentDebugDump(sb, indent+1);

        sb.append("currentShadow:");
        if (shadowedResourceObject == null) {
            sb.append(" null\n");
        } else {
            sb.append("\n");
            sb.append(shadowedResourceObject.debugDump(indent+2));
        }

        sb.append("\n");
        SchemaDebugUtil.indentDebugDump(sb, indent+1);
        sb.append("simulate: ").append(simulate);

        sb.append("\n");
        SchemaDebugUtil.indentDebugDump(sb, indent+1);
        sb.append("cleanDeadShadow: ").append(cleanDeadShadow);

        sb.append("\n");
        SchemaDebugUtil.indentDebugDump(sb, indent+1);
        sb.append("itemProcessingIdentifier: ").append(itemProcessingIdentifier);

        return sb.toString();
    }

    public String getShadowOid() {
        return shadowedResourceObject.getOid();
    }

    public boolean isDelete() {
        return ObjectDelta.isDelete(objectDelta);
    }
}

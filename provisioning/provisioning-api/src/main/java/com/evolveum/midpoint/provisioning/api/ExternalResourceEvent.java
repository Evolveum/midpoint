/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.api;

import java.io.Serializable;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.util.SchemaDebugUtil;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * Externally-provided resource event. Used to inform provisioning module about the fact
 * that an object on a resource has changed.
 */
public class ExternalResourceEvent implements ProvisioningEvent, Serializable, DebugDumpable {

    /**
     * The change - if known.
     */
    private final ObjectDelta<ShadowType> objectDelta;

    /**
     * Resource object after the change - if known.
     */
    private final PrismObject<ShadowType> resourceObject;

    /**
     * Repository shadow connected to the object - if known and if existing.
      */
    private final PrismObject<ShadowType> oldRepoShadow;

    /**
     * Channel through which we learned about the change.
     */
    private final String sourceChannel;

    public ExternalResourceEvent(ObjectDelta<ShadowType> objectDelta, PrismObject<ShadowType> resourceObject,
            PrismObject<ShadowType> oldRepoShadow, String sourceChannel) {
        this.objectDelta = objectDelta;
        this.resourceObject = resourceObject;
        this.oldRepoShadow = oldRepoShadow;
        this.sourceChannel = sourceChannel;
    }

    public PrismObject<ShadowType> getResourceObject() {
        return resourceObject;
    }

    public PrismObject<ShadowType> getOldRepoShadow() {
        return oldRepoShadow;
    }

    public ObjectDelta<ShadowType> getObjectDelta() {
        return objectDelta;
    }

    public String getSourceChannel() {
        return sourceChannel;
    }

    public boolean isProtected() {
        return ShadowUtil.isProtected(resourceObject) ||
                ShadowUtil.isProtected(oldRepoShadow) ||
                objectDelta != null && objectDelta.isAdd() && ShadowUtil.isProtected(objectDelta.getObjectToAdd());
    }

    @Override
    public String toString() {
        return getClass().getSimpleName()
                + "(delta=" + objectDelta
                + ", currentResourceObject=" + SchemaDebugUtil.prettyPrint(resourceObject)
                + ", oldRepoShadow=" + SchemaDebugUtil.prettyPrint(oldRepoShadow)
                + ", sourceChannel=" + sourceChannel
                + ")";
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        SchemaDebugUtil.indentDebugDump(sb, indent);
        sb.append(getClass().getSimpleName()).append("(").append(sourceChannel).append(")\n");

        SchemaDebugUtil.indentDebugDump(sb, indent+1);

        sb.append("Delta:");
        if (objectDelta == null) {
            sb.append(" null");
        } else {
            sb.append(objectDelta.debugDump(indent+2));
        }
        sb.append("\n");
        SchemaDebugUtil.indentDebugDump(sb, indent+1);

        sb.append("oldRepoShadow:");
        if (oldRepoShadow == null) {
            sb.append(" null");
        } else {
            sb.append(oldRepoShadow.debugDump(indent+2));
        }

        sb.append("\n");
        SchemaDebugUtil.indentDebugDump(sb, indent+1);

        sb.append("currentResourceObject:");
        if (resourceObject == null) {
            sb.append(" null\n");
        } else {
            sb.append("\n");
            sb.append(resourceObject.debugDump(indent+2));
        }

        return sb.toString();
    }

}

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
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import java.io.Serializable;

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

    private ObjectDelta<ShadowType> objectDelta;

    /**
     * Current "shadowized" resource object.
     */
    private PrismObject<ShadowType> currentShadow;

    /**
     * Repository shadow as it existed before it was updated (as a result of the detected change).
     * In general it is useful only when {@link #currentShadow} is null or it has null OID. TODO!!!
     */
    private PrismObject<ShadowType> oldShadow;

    private String sourceChannel;
    private PrismObject<ResourceType> resource;

    /**
     * If set to true then this change is not related to the primary goal of
     * the running task. E.g. it may be a change in entitlement that is discovered
     * when reading an account. Or it may be ordinary creation of a new shadow during
     * search.
     *
     * On the other hand, related change is a change in the object that is being processed.
     * E.g. discovering that the object is missing, or a conflicting object already exists.
     *
     * It is expected that reactions to the unrelated changes will be lighter, faster,
     * with lower overhead and without ambition to provide full synchronization.
     */
    private boolean unrelatedChange = false;

    private boolean simulate = false;

    private boolean cleanDeadShadow = false;

    public ObjectDelta<ShadowType> getObjectDelta() {
        return objectDelta;
    }

    public void setObjectDelta(ObjectDelta<ShadowType> objectDelta) {
        this.objectDelta = objectDelta;
    }

    public PrismObject<ShadowType> getCurrentShadow() {
        return currentShadow;
    }

    public void setCurrentShadow(PrismObject<ShadowType> currentShadow) {
        this.currentShadow = currentShadow;
    }

    public PrismObject<ShadowType> getOldShadow() {
        return oldShadow;
    }

    public void setOldShadow(PrismObject<ShadowType> oldShadow) {
        this.oldShadow = oldShadow;
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

    public boolean isUnrelatedChange() {
        return unrelatedChange;
    }

    public void setUnrelatedChange(boolean unrelatedChange) {
        this.unrelatedChange = unrelatedChange;
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

    public void checkConsistence() {
        if (resource == null) {
            throw new IllegalArgumentException("No resource in "+this.getClass().getSimpleName());
        }
        resource.checkConsistence();
        if (sourceChannel == null) {
            throw new IllegalArgumentException("No sourceChannel in "+this.getClass().getSimpleName());
        }
        if (objectDelta == null && currentShadow == null) {
            throw new IllegalArgumentException("Either objectDelta or currentShadow must be set in "+this.getClass().getSimpleName());
        }
        if (objectDelta != null && objectDelta.getOid() == null) {
            throw new IllegalArgumentException("Delta OID not set in "+this.getClass().getSimpleName());
        }
        if (objectDelta != null) {
            objectDelta.checkConsistence();
        }
        if (currentShadow != null && currentShadow.getOid() == null) {
            throw new IllegalArgumentException("Current shadow OID not set in "+this.getClass().getSimpleName());
        }
        if (currentShadow != null) {
            ShadowUtil.checkConsistence(currentShadow,"current shadow in change notification");
        }
        if (oldShadow != null) {
            ShadowUtil.checkConsistence(oldShadow,"old shadow in change notification");
        }
    }

    public boolean isProtected() {
        if ((ShadowUtil.isProtected(currentShadow))
                || (ShadowUtil.isProtected(oldShadow))) {
            return true;
        }
        if (objectDelta != null && objectDelta.isAdd() && ShadowUtil.isProtected(objectDelta.getObjectToAdd())) {
            return true;
        }
        return false;
    }

    @Override
    public String toString() {
        return "ResourceObjectShadowChangeDescription(objectDelta=" + objectDelta + ", currentShadow="
                + SchemaDebugUtil.prettyPrint(currentShadow) + ", oldShadow=" + SchemaDebugUtil.prettyPrint(oldShadow) + ", sourceChannel=" + sourceChannel
                + ", resource=" + resource + (unrelatedChange ? " UNRELATED" : "") + (simulate ? " SIMULATE" : "") + (cleanDeadShadow ? " CLEAN DEAD SHADOW" : "") + ")";
    }

    /* (non-Javadoc)
     * @see com.evolveum.midpoint.util.DebugDumpable#debugDump(int)
     */
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

        sb.append("oldShadow:");
        if (oldShadow == null) {
            sb.append(" null");
        } else {
            sb.append("\n");
            sb.append(oldShadow.debugDump(indent+2));
        }

        sb.append("\n");
        SchemaDebugUtil.indentDebugDump(sb, indent+1);

        sb.append("currentShadow:");
        if (currentShadow == null) {
            sb.append(" null\n");
        } else {
            sb.append("\n");
            sb.append(currentShadow.debugDump(indent+2));
        }

        sb.append("\n");
        SchemaDebugUtil.indentDebugDump(sb, indent+1);
        sb.append("unrelatedChange: ").append(unrelatedChange);

        sb.append("\n");
        SchemaDebugUtil.indentDebugDump(sb, indent+1);
        sb.append("simulate: ").append(simulate);

        sb.append("\n");
        SchemaDebugUtil.indentDebugDump(sb, indent+1);
        sb.append("cleanDeadShadow: ").append(cleanDeadShadow);


        return sb.toString();
    }

}

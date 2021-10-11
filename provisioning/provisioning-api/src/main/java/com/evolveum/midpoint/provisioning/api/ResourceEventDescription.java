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

public class ResourceEventDescription implements Serializable, DebugDumpable{

    private PrismObject<ShadowType> oldRepoShadow;
    private PrismObject<ShadowType> currentResourceObject;
    private ObjectDelta<ShadowType> delta;
    private String sourceChannel;
//    private PrismObject<ResourceType> resource;


    public PrismObject<ShadowType> getCurrentResourceObject() {
        return currentResourceObject;
    }

    public PrismObject<ShadowType> getOldRepoShadow() {
        return oldRepoShadow;
    }

    public ObjectDelta<ShadowType> getDelta() {
        return delta;
    }

    public String getSourceChannel() {
        return sourceChannel;
    }
//    public PrismObject<ResourceType> getResource() {
//        return resource;
//    }

    public void setDelta(ObjectDelta delta) {
        this.delta = delta;
    }

    public void setOldRepoShadow(PrismObject<ShadowType> oldRepoShadow) {
        this.oldRepoShadow = oldRepoShadow;
    }

    public void setCurrentResourceObject(PrismObject<ShadowType> currentResourceObject) {
        this.currentResourceObject = currentResourceObject;
    }

    public void setSourceChannel(String sourceChannel) {
        this.sourceChannel = sourceChannel;
    }

     public boolean isProtected() {
            if ((currentResourceObject != null && ShadowUtil.isProtected(currentResourceObject))
                    || (oldRepoShadow != null && ShadowUtil.isProtected(oldRepoShadow))) {
                return true;
            }
            if (delta != null && delta.isAdd() && ShadowUtil.isProtected(delta.getObjectToAdd())) {
                return true;
            }
            return false;
        }

     @Override
        public String toString() {
            return "ResourceEventDescription(delta=" + delta + ", currentResourceObject="
                    + SchemaDebugUtil.prettyPrint(currentResourceObject) + ", oldRepoShadow=" + SchemaDebugUtil.prettyPrint(oldRepoShadow) + ", sourceChannel=" + sourceChannel
                    + ")";
        }

        /* (non-Javadoc)
         * @see com.evolveum.midpoint.util.DebugDumpable#debugDump(int)
         */
        @Override
        public String debugDump(int indent) {
            StringBuilder sb = new StringBuilder();
            SchemaDebugUtil.indentDebugDump(sb, indent);
            sb.append("ResourceEventDescription(");
            sb.append(sourceChannel);
            sb.append(")\n");

            SchemaDebugUtil.indentDebugDump(sb, indent+1);

            sb.append("Delta:");
            if (delta == null) {
                sb.append(" null");
            } else {
                sb.append(delta.debugDump(indent+2));
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
            if (currentResourceObject == null) {
                sb.append(" null\n");
            } else {
                sb.append("\n");
                sb.append(currentResourceObject.debugDump(indent+2));
            }

            return sb.toString();
        }
//    public void setResource(PrismObject<ResourceType> resource) {
//        this.resource = resource;
//    }
//
//

    public PrismObject<ShadowType> getShadow() {
        PrismObject<ShadowType> shadow;
        if (getCurrentResourceObject() != null) {
            shadow = getCurrentResourceObject();
        } else if (getOldRepoShadow() != null) {
            shadow = getOldRepoShadow();
        } else if (getDelta() != null && getDelta().isAdd()) {
            if (getDelta().getObjectToAdd() == null) {
                throw new IllegalStateException("Found ADD delta, but no object to add was specified.");
            }
            shadow = getDelta().getObjectToAdd();
        } else {
            throw new IllegalStateException("Resource event description does not contain neither old shadow, nor current shadow, nor shadow in delta");
        }
        return shadow;
    }
}

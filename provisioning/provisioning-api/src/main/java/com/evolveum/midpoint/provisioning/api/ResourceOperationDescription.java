/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.api;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.util.SchemaDebugUtil;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * Describes an attempt to apply a change to a specific resource object.
 *
 * @author Radovan Semancik
 */
public class ResourceOperationDescription implements ProvisioningEvent, DebugDumpable {

    private ObjectDelta<? extends ShadowType> objectDelta;
    private PrismObject<? extends ShadowType> currentShadow;
    private String sourceChannel;
    private PrismObject<ResourceType> resource;
    private String message;
    private boolean asynchronous = false;
    private int attemptNumber = 0;

    /**
     * The operation that was about to execute and that has failed.
     */
    public ObjectDelta<? extends ShadowType> getObjectDelta() {
        return objectDelta;
    }

    public void setObjectDelta(ObjectDelta<? extends ShadowType> objectDelta) {
        this.objectDelta = objectDelta;
    }

    /**
     * Shadow describing the object that was the target of the operation. It may a "temporary" shadow that
     * is not yet bound to a specific resource object (e.g. in case of add operation).
     */
    public PrismObject<? extends ShadowType> getCurrentShadow() {
        return currentShadow;
    }

    public void setCurrentShadow(PrismObject<? extends ShadowType> currentShadow) {
        this.currentShadow = currentShadow;
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

    /**
     * Result of the failed operation.
     */
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * True if the operation is asynchronous. I.e. true if the operation
     * cannot provide direct return value and therefore the invocation of
     * the listener is the only way how to pass operation return value to
     * the upper layers.
     *
     * This may be useful e.g. for decided whether log the message and what
     * log level to use (it can be assumed that the error gets logged at least
     * once for synchronous operations, but this may be the only chance to
     * properly log it for asynchronous operations).
     */
    public boolean isAsynchronous() {
        return asynchronous;
    }

    public void setAsynchronous(boolean asynchronous) {
        this.asynchronous = asynchronous;
    }

    public int getAttemptNumber() {
        return attemptNumber;
    }

    public void setAttemptNumber(int attemptNumber) {
        this.attemptNumber = attemptNumber;
    }

    public void checkConsistence() {
        if (resource == null) {
            throw new IllegalArgumentException("No resource in "+this.getClass().getSimpleName());
        }
        resource.checkConsistence();
        //FIXME: have not to be set always
//        if (sourceChannel == null) {
//            throw new IllegalArgumentException("No sourceChannel in "+this.getClass().getSimpleName());
//        }
        if (objectDelta == null && currentShadow == null) {
            throw new IllegalArgumentException("Either objectDelta or currentShadow must be set in "+this.getClass().getSimpleName());
        }
        if (objectDelta != null && !objectDelta.isAdd() && objectDelta.getOid() == null) {
            throw new IllegalArgumentException("Delta OID not set in "+this.getClass().getSimpleName());
        }
        if (objectDelta != null) {
            objectDelta.checkConsistence();
        }
        //shadow does not have oid set, for example the shadow should be added, but is wasn't because of some error
        if (currentShadow != null && currentShadow.getOid() == null && objectDelta != null && !objectDelta.isAdd()) {
            throw new IllegalArgumentException("Current shadow OID not set in "+this.getClass().getSimpleName());
        }
        if (currentShadow != null) {
            currentShadow.checkConsistence();
        }
    }

    @Override
    public String toString() {
        return "ResourceOperationDescription(objectDelta=" + objectDelta + ", currentShadow="
                + SchemaDebugUtil.prettyPrint(currentShadow) + ", sourceChannel=" + sourceChannel
                + ", resource=" + resource +
                (asynchronous ? ", ASYNC" : "") +
                (attemptNumber != 0 ? ", attemptNumber="+attemptNumber : "") +
                ", message=" + message + ")";
    }

    /* (non-Javadoc)
     * @see com.evolveum.midpoint.util.DebugDumpable#debugDump(int)
     */
    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        SchemaDebugUtil.indentDebugDump(sb, indent);
        sb.append("ResourceOperationDescription(");
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
            sb.append(objectDelta.debugDump(indent+2));
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
        DebugUtil.debugDumpLabel(sb, "Asynchronous", indent+1);
        sb.append(asynchronous);

        sb.append("\n");
        DebugUtil.debugDumpLabel(sb, "Attempt number", indent+1);
        sb.append(attemptNumber);

        sb.append("\n");
        SchemaDebugUtil.indentDebugDump(sb, indent+1);
        sb.append("message: ").append(message);
        return sb.toString();
    }
}

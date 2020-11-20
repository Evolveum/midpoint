/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.ucf.api;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.processor.ObjectClassComplexTypeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import java.util.Collection;

/**
 * @author Radovan Semancik
 *
 */
public final class Change implements DebugDumpable {

    /*
     * Object identification: these values should be reasonably filled-in on Change object creation.
     */
    private final Object primaryIdentifierRealValue; // we might reconsider this in the future
    private Collection<ResourceAttribute<?>> identifiers;
    private ObjectClassComplexTypeDefinition objectClassDefinition;

    /**
     * Sequence number that is local to the current live sync or async update operation.
     * It is used to ensure related changes are processed in the same order in which they came.
     */
    @Experimental
    private final int localSequenceNumber;

    /*
     * Usually either of the following two should be present. An exception is a notification-only change event.
     */

    /**
     * This starts as a (converted) resource object. Gradually it gets augmented with shadow information,
     * but the process needs to be clarified. See MID-5834.
     */
    private PrismObject<ShadowType> currentResourceObject;

    /**
     * Delta from the resource - if known. Substantial e.g. for asynchronous updates.
     */
    private ObjectDelta<ShadowType> objectDelta;

    /**
     * Token is used only in live synchronization process.
     */
    private PrismProperty<?> token;

    /**
     * Original value of the corresponding shadow stored in repository.
     *
     * This is usually filled-in during change processing in provisioning module. (Except for notifyChange calls: TBD.)
     */
    private PrismObject<ShadowType> oldRepoShadow;

    /**
     * This means that the change is just a notification that a resource object has changed. To know about its state
     * it has to be fetched. For notification-only changes both objectDelta and currentResourceObject have to be null.
     * (And this flag is introduced to distinguish intentional notification-only changes from malformed ones that have
     * both currentResourceObject and objectDelta missing.)
     */
    private boolean notificationOnly;

    /**
     * When token is present.
     */
    public Change(Object primaryIdentifierRealValue, Collection<ResourceAttribute<?>> identifiers,
            PrismObject<ShadowType> currentResourceObject, ObjectDelta<ShadowType> delta,
            PrismProperty<?> token, int localSequenceNumber) {
        this.primaryIdentifierRealValue = primaryIdentifierRealValue;
        this.identifiers = identifiers;
        this.currentResourceObject = currentResourceObject;
        this.objectDelta = delta;
        this.token = token;
        this.localSequenceNumber = localSequenceNumber;
    }

    /**
     * When token is not present.
     */
    public Change(Object primaryIdentifierRealValue, Collection<ResourceAttribute<?>> identifiers,
            PrismObject<ShadowType> currentResourceObject, ObjectDelta<ShadowType> delta, int localSequenceNumber) {
        this.primaryIdentifierRealValue = primaryIdentifierRealValue;
        this.identifiers = identifiers;
        this.currentResourceObject = currentResourceObject;
        this.objectDelta = delta;
        this.localSequenceNumber = localSequenceNumber;
    }

    public ObjectDelta<ShadowType> getObjectDelta() {
        return objectDelta;
    }

    public void setObjectDelta(ObjectDelta<ShadowType> change) {
        this.objectDelta = change;
    }

    public Collection<ResourceAttribute<?>> getIdentifiers() {
        return identifiers;
    }

    public void setIdentifiers(Collection<ResourceAttribute<?>> identifiers) {
        this.identifiers = identifiers;
    }

    public ObjectClassComplexTypeDefinition getObjectClassDefinition() {
        return objectClassDefinition;
    }

    public void setObjectClassDefinition(ObjectClassComplexTypeDefinition objectClassDefinition) {
        this.objectClassDefinition = objectClassDefinition;
    }

    public PrismProperty<?> getToken() {
        return token;
    }

    public void setToken(PrismProperty<?> token) {
        this.token = token;
    }

    public PrismObject<ShadowType> getOldRepoShadow() {
        return oldRepoShadow;
    }

    public void setOldRepoShadow(PrismObject<ShadowType> oldRepoShadow) {
        this.oldRepoShadow = oldRepoShadow;
    }

    public PrismObject<ShadowType> getCurrentResourceObject() {
        return currentResourceObject;
    }

    public void setCurrentResourceObject(PrismObject<ShadowType> currentResourceObject) {
        this.currentResourceObject = currentResourceObject;
    }

    public void setNotificationOnly(boolean notificationOnly) {
        this.notificationOnly = notificationOnly;
    }

    public boolean isNotificationOnly() {
        return notificationOnly;
    }

    public boolean isDelete() {
        return objectDelta != null && objectDelta.isDelete();
    }

    // todo what if delta is null, oldShadow is null, current is not null?
    public boolean isAdd() {
        return objectDelta != null && objectDelta.isAdd();
    }

    @Override
    public String toString() {
        return "Change(uid=" + primaryIdentifierRealValue
                + ", identifiers=" + identifiers
                + ", objectDelta=" + objectDelta
                + ", token=" + token
                + ", localSequenceNumber=" + localSequenceNumber
                + ", oldRepoShadow=" + oldRepoShadow
                + ", currentResourceObject=" + currentResourceObject + ")";
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        DebugUtil.indentDebugDump(sb, 0);
        sb.append("Change");
        if (notificationOnly) {
            sb.append(" (notification only)");
        }
        sb.append("\n");
        DebugUtil.debugDumpWithLabel(sb, "primaryIdentifierValue", String.valueOf(primaryIdentifierRealValue), indent + 1);
        sb.append("\n");
        DebugUtil.debugDumpWithLabel(sb, "identifiers", identifiers, indent + 1);
        sb.append("\n");
        DebugUtil.debugDumpWithLabel(sb, "localSequenceNumber", localSequenceNumber, indent + 1);
        sb.append("\n");
        DebugUtil.debugDumpWithLabel(sb, "objectDelta", objectDelta, indent + 1);
        sb.append("\n");
        DebugUtil.debugDumpWithLabel(sb, "objectClassDefinition", objectClassDefinition, indent + 1);
        sb.append("\n");
        DebugUtil.debugDumpWithLabel(sb, "token", token, indent + 1);
        sb.append("\n");
        DebugUtil.debugDumpWithLabel(sb, "oldRepoShadow", oldRepoShadow, indent + 1);
        sb.append("\n");
        DebugUtil.debugDumpWithLabel(sb, "currentResourceObject", currentResourceObject, indent + 1);
        return sb.toString();
    }

    public String getOid() {
        String oid = guessOid();
        if (oid != null) {
            return oid;
        } else {
            throw new IllegalArgumentException("No oid value defined for the object to synchronize.");
        }
    }

    public String guessOid() {
        if (objectDelta != null && objectDelta.getOid() != null) {
            return objectDelta.getOid();
        } else if (currentResourceObject.getOid() != null) {
            return currentResourceObject.getOid();
        } else if (oldRepoShadow.getOid() != null) {
            return oldRepoShadow.getOid();
        } else {
            return null;
        }
    }

    public Object getPrimaryIdentifierRealValue() {
        return primaryIdentifierRealValue;
    }

    public int getLocalSequenceNumber() {
        return localSequenceNumber;
    }
}

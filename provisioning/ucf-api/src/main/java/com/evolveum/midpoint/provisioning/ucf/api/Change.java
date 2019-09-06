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
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import java.util.Collection;

/**
 * @author Radovan Semancik
 *
 */
public final class Change implements DebugDumpable {

    private Collection<ResourceAttribute<?>> identifiers;
	private Object primaryIdentifierRealValue;      // we might reconsider this in the future
    private ObjectClassComplexTypeDefinition objectClassDefinition;
    private ObjectDelta<ShadowType> objectDelta;
    private PrismProperty<?> token;
    // TODO: maybe call this repoShadow?
    private PrismObject<ShadowType> oldShadow;
    private PrismObject<ShadowType> currentShadow;

	/**
	 * This means that the change is just a notification that a resource object has changed. To know about its state
	 * it has to be fetched. For notification-only changes the objectDelta and currentShadow has to be null.
	 * (And this flag is introduced to distinguish intentional notification-only changes from malformed ones that have
	 * both currentShadow and objectDelta missing.)
	 */
	private boolean notificationOnly;

    public Change(Object primaryIdentifierRealValue, Collection<ResourceAttribute<?>> identifiers, ObjectDelta<ShadowType> change,
		    PrismProperty<?> token) {
    	this.primaryIdentifierRealValue = primaryIdentifierRealValue;
        this.identifiers = identifiers;
        this.objectDelta = change;
        this.currentShadow = null;
        this.token = token;
    }

    public Change(Object primaryIdentifierRealValue, Collection<ResourceAttribute<?>> identifiers,
		    PrismObject<ShadowType> currentShadow, PrismProperty<?> token) {
    	this.primaryIdentifierRealValue = primaryIdentifierRealValue;
        this.identifiers = identifiers;
        this.objectDelta = null;
        this.currentShadow = currentShadow;
        this.token = token;
    }

    public Change(Object primaryIdentifierRealValue, Collection<ResourceAttribute<?>> identifiers,
		    PrismObject<ShadowType> currentShadow, PrismObject<ShadowType> oldShadow, ObjectDelta<ShadowType> objectDelta) {
	    this.primaryIdentifierRealValue = primaryIdentifierRealValue;
    	this.identifiers = identifiers;
    	this.currentShadow = currentShadow;
    	this.oldShadow = oldShadow;
    	this.objectDelta = objectDelta;
    }

    public Change(PrismProperty<?> token) {
        this.token = token;
    }

    private Change() {
    }

    public static Change createNotificationOnly(Collection<ResourceAttribute<?>> identifiers) {
	    Change rv = new Change();
	    rv.identifiers = identifiers;
	    rv.notificationOnly = true;
	    return rv;
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

	public PrismObject<ShadowType> getOldShadow() {
		return oldShadow;
	}

	public void setOldShadow(PrismObject<ShadowType> oldShadow) {
		this.oldShadow = oldShadow;
	}

	public PrismObject<ShadowType> getCurrentShadow() {
		return currentShadow;
	}

	public void setCurrentShadow(PrismObject<ShadowType> currentShadow) {
		this.currentShadow = currentShadow;
	}

	public boolean isTokenOnly() {
		return identifiers == null && objectDelta == null && currentShadow == null && token != null;
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
		return "Change(uid=" + primaryIdentifierRealValue + ",identifiers=" + identifiers + ", objectDelta=" + objectDelta + ", token=" + token
				+ ", oldShadow=" + oldShadow + ", currentShadow=" + currentShadow + ")";
	}

	@Override
	public String debugDump() {
		return debugDump(0);
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
		DebugUtil.debugDumpWithLabel(sb, "objectDelta", objectDelta, indent + 1);
		sb.append("\n");
		DebugUtil.debugDumpWithLabel(sb, "objectClassDefinition", objectClassDefinition, indent + 1);
		sb.append("\n");
		DebugUtil.debugDumpWithLabel(sb, "token", token, indent + 1);
		sb.append("\n");
		DebugUtil.debugDumpWithLabel(sb, "oldShadow", oldShadow, indent + 1);
		sb.append("\n");
		DebugUtil.debugDumpWithLabel(sb, "currentShadow", currentShadow, indent + 1);
		return sb.toString();
	}

	public String getOid() {
		if (objectDelta != null && objectDelta.getOid() != null) {
			return objectDelta.getOid();
		} else if (currentShadow.getOid() != null) {
			return currentShadow.getOid();
		} else if (oldShadow.getOid() != null) {
			return oldShadow.getOid();
		} else {
			throw new IllegalArgumentException("No oid value defined for the object to synchronize.");
		}
	}

	public Object getPrimaryIdentifierRealValue() {
		return primaryIdentifierRealValue;
	}
}

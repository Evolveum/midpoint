/*
 * Copyright (c) 2011 Evolveum
 * 
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 * 
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 * 
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * Portions Copyrighted 2011 [name of copyright owner]
 */
package com.evolveum.midpoint.provisioning.ucf.api;

import com.evolveum.midpoint.schema.delta.ObjectDelta;
import com.evolveum.midpoint.schema.processor.PrismProperty;
import com.evolveum.midpoint.schema.processor.ResourceObjectAttribute;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectChangeType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceObjectShadowType;

import java.util.Set;

/**
 * FIXME: this is somehow wrong. It is using concept of Shadow. Universal API should not use that.
 * But it works for now.
 * @author Radovan Semancik
 *
 */
public final class Change {
	
    private Set<ResourceObjectAttribute> identifiers;
    private ObjectDelta<? extends ResourceObjectShadowType> objectDelta;
    private PrismProperty token;
    private ResourceObjectShadowType oldShadow;
    private ResourceObjectShadowType currentShadow;

    public Change(Set<ResourceObjectAttribute> identifiers, ObjectDelta<? extends ResourceObjectShadowType> change, PrismProperty token) {
        this.identifiers = identifiers;
        this.objectDelta = change;
        this.currentShadow = null;
        this.token = token;
    }

    public Change(Set<ResourceObjectAttribute> identifiers, ResourceObjectShadowType currentShadow, PrismProperty token) {
        this.identifiers = identifiers;
        this.objectDelta = null;
        this.currentShadow = currentShadow;
        this.token = token;
    }

    public Change(ObjectDelta<? extends ResourceObjectShadowType> change, PrismProperty token) {
        this.objectDelta = change;
        this.token = token;
    }

    public ObjectDelta<? extends ResourceObjectShadowType> getObjectDelta() {
        return objectDelta;
    }

    public void setObjectDelta(ObjectDelta<? extends ResourceObjectShadowType> change) {
        this.objectDelta = change;
    }

    public Set<ResourceObjectAttribute> getIdentifiers() {
        return identifiers;
    }

    public void setIdentifiers(Set<ResourceObjectAttribute> identifiers) {
        this.identifiers = identifiers;
    }

	public PrismProperty getToken() {
		return token;
	}

	public void setToken(PrismProperty token) {
		this.token = token;
	}

	public ResourceObjectShadowType getOldShadow() {
		return oldShadow;
	}

	public void setOldShadow(ResourceObjectShadowType oldShadow) {
		this.oldShadow = oldShadow;
	}

	public ResourceObjectShadowType getCurrentShadow() {
		return currentShadow;
	}

	public void setCurrentShadow(ResourceObjectShadowType currentShadow) {
		this.currentShadow = currentShadow;
	}
	
}
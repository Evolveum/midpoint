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

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.processor.ObjectClassComplexTypeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceObjectShadowType;

import java.util.Collection;
import java.util.Set;

/**
 * @author Radovan Semancik
 *
 */
public final class Change<T extends ResourceObjectShadowType> {
	
    private Collection<ResourceAttribute<?>> identifiers;
    private ObjectClassComplexTypeDefinition objectClassDefinition;
    private ObjectDelta<T> objectDelta;
    private PrismProperty<?> token;
    // TODO: maybe call this repoShadow?
    private PrismObject<T> oldShadow;
    private PrismObject<T> currentShadow;

    public Change(Collection<ResourceAttribute<?>> identifiers, ObjectDelta<T> change, PrismProperty<?> token) {
        this.identifiers = identifiers;
        this.objectDelta = change;
        this.currentShadow = null;
        this.token = token;
    }

    public Change(Collection<ResourceAttribute<?>> identifiers, PrismObject<T> currentShadow, PrismProperty<?> token) {
        this.identifiers = identifiers;
        this.objectDelta = null;
        this.currentShadow = currentShadow;
        this.token = token;
    }

    public Change(ObjectDelta<T> change, PrismProperty<?> token) {
        this.objectDelta = change;
        this.token = token;
    }

    public ObjectDelta<? extends ResourceObjectShadowType> getObjectDelta() {
        return objectDelta;
    }

    public void setObjectDelta(ObjectDelta<T> change) {
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

	public PrismObject<T> getOldShadow() {
		return oldShadow;
	}

	public void setOldShadow(PrismObject<T> oldShadow) {
		this.oldShadow = oldShadow;
	}

	public PrismObject<T> getCurrentShadow() {
		return currentShadow;
	}

	public void setCurrentShadow(PrismObject<T> currentShadow) {
		this.currentShadow = currentShadow;
	}

	@Override
	public String toString() {
		return "Change(identifiers=" + identifiers + ", objectDelta=" + objectDelta + ", token=" + token
				+ ", oldShadow=" + oldShadow + ", currentShadow=" + currentShadow + ")";
	}
	
}
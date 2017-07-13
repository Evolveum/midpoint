/*
 * Copyright (c) 2010-2017 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
    private ObjectClassComplexTypeDefinition objectClassDefinition;
    private ObjectDelta<ShadowType> objectDelta;
    private PrismProperty<?> token;
    // TODO: maybe call this repoShadow?
    private PrismObject<ShadowType> oldShadow;
    private PrismObject<ShadowType> currentShadow;

    public Change(Collection<ResourceAttribute<?>> identifiers, ObjectDelta<ShadowType> change, PrismProperty<?> token) {
        this.identifiers = identifiers;
        this.objectDelta = change;
        this.currentShadow = null;
        this.token = token;
    }

    public Change(Collection<ResourceAttribute<?>> identifiers, PrismObject<ShadowType> currentShadow, PrismProperty<?> token) {
        this.identifiers = identifiers;
        this.objectDelta = null;
        this.currentShadow = currentShadow;
        this.token = token;
    }
    
    public Change(Collection<ResourceAttribute<?>> identifiers, PrismObject<ShadowType> currentShadow, PrismObject<ShadowType> oldStadow, ObjectDelta<ShadowType> objectDetla){
    	this.identifiers = identifiers;
    	this.currentShadow = currentShadow;
    	this.oldShadow = oldStadow;
    	this.objectDelta = objectDetla;
    }

    public Change(ObjectDelta<ShadowType> change, PrismProperty<?> token) {
        this.objectDelta = change;
        this.token = token;
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


	@Override
	public String toString() {
		return "Change(identifiers=" + identifiers + ", objectDelta=" + objectDelta + ", token=" + token
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
		sb.append("Change\n");
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
	
}

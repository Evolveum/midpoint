/*
 * Copyright (c) 2012 Evolveum
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
 *
 * Portions Copyrighted 2012 [name of copyright owner]
 */

package com.evolveum.midpoint.provisioning.api;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.util.ResourceObjectShadowUtil;
import com.evolveum.midpoint.schema.util.SchemaDebugUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.Dumpable;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceType;

/**
 * Describes a change of a specific resource object together with definitions of the source and possibly
 * also other information. This is useful to completely describe a change that was detected on the resource.
 * <p/>
 * This object can describe either relative change or new absolute state. In case of relative change the "objectDelta"
 * property will be provided. In case of description of new absolute state the "currentShadow" value will be provided.
 * It may happen that both of them will be provided if both are known (and efficiently detected). In such a case the
 * implementation may choose any one to process.
 *
 * @author Radovan Semancik
 */
public class ResourceObjectShadowChangeDescription implements Dumpable, DebugDumpable {

    private ObjectDelta<? extends ShadowType> objectDelta;
    private PrismObject<? extends ShadowType> currentShadow;
    private PrismObject<? extends ShadowType> oldShadow;
    private String sourceChannel;
    private PrismObject<ResourceType> resource;

    public ObjectDelta<? extends ShadowType> getObjectDelta() {
        return objectDelta;
    }

    public void setObjectDelta(ObjectDelta<? extends ShadowType> objectDelta) {
        this.objectDelta = objectDelta;
    }

    public PrismObject<? extends ShadowType> getCurrentShadow() {
        return currentShadow;
    }

    public void setCurrentShadow(PrismObject<? extends ShadowType> currentShadow) {
        this.currentShadow = currentShadow;
    }

    public PrismObject<? extends ShadowType> getOldShadow() {
        return oldShadow;
    }

    public void setOldShadow(PrismObject<? extends ShadowType> oldShadow) {
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
    		ResourceObjectShadowUtil.checkConsistence(currentShadow,"current shadow in change notification");
    	}
    	if (oldShadow != null) {
    		ResourceObjectShadowUtil.checkConsistence(oldShadow,"old shadow in change notification");
    	}
    }

	@Override
	public String toString() {
		return "ResourceObjectShadowChangeDescription(objectDelta=" + objectDelta + ", currentShadow="
				+ SchemaDebugUtil.prettyPrint(currentShadow) + ", oldShadow=" + SchemaDebugUtil.prettyPrint(oldShadow) + ", sourceChannel=" + sourceChannel
				+ ", resource=" + resource + ")";
	}
    
    @Override
    public String dump() {
    	return debugDump(0);
    }

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.util.DebugDumpable#debugDump()
	 */
	@Override
	public String debugDump() {
		return debugDump(0);
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
			sb.append(objectDelta.debugDump(indent+2));
		}
		sb.append("\n");
		SchemaDebugUtil.indentDebugDump(sb, indent+1);
		
		sb.append("oldShadow:");
		if (oldShadow == null) {
			sb.append(" null");
		} else {
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

		return sb.toString();
	}

}

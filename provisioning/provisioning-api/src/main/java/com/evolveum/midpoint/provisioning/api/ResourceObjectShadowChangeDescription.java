/**
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
package com.evolveum.midpoint.provisioning.api;

import com.evolveum.midpoint.schema.delta.ObjectDelta;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceObjectShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceType;

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
 *
 */
public class ResourceObjectShadowChangeDescription {
	
	private ObjectDelta<? extends ResourceObjectShadowType> objectDelta;
	private ResourceObjectShadowType currentShadow;
	private ResourceObjectShadowType oldShadow;
	private String sourceChannel;
	private ResourceType resource;
	
	public ObjectDelta<? extends ResourceObjectShadowType> getObjectDelta() {
		return objectDelta;
	}
	
	public void setObjectDelta(ObjectDelta<? extends ResourceObjectShadowType> objectDelta) {
		this.objectDelta = objectDelta;
	}

	public ResourceObjectShadowType getCurrentShadow() {
		return currentShadow;
	}

	public void setCurrentShadow(ResourceObjectShadowType currentShadow) {
		this.currentShadow = currentShadow;
	}

	public ResourceObjectShadowType getOldShadow() {
		return oldShadow;
	}

	public void setOldShadow(ResourceObjectShadowType oldShadow) {
		this.oldShadow = oldShadow;
	}

	public String getSourceChannel() {
		return sourceChannel;
	}
	
	public void setSourceChannel(String sourceChannel) {
		this.sourceChannel = sourceChannel;
	}
	
	public ResourceType getResource() {
		return resource;
	}
	
	public void setResource(ResourceType resource) {
		this.resource = resource;
	}

}

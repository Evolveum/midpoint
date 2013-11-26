package com.evolveum.midpoint.provisioning.api;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ShadowType;

public class ResourceEventDescription {
	
	private PrismObject<ShadowType> oldShadow;
	private PrismObject<ShadowType> currentShadow;
	private ObjectDelta delta;
	private PrismObject<ResourceType> resource;
	
	
	public PrismObject<ShadowType> getCurrentShadow() {
		return currentShadow;
	}
	
	public PrismObject<ShadowType> getOldShadow() {
		return oldShadow;
	}
	
	public ObjectDelta getDelta() {
		return delta;
	}
	
	public PrismObject<ResourceType> getResource() {
		return resource;
	}
	
	public void setDelta(ObjectDelta delta) {
		this.delta = delta;
	}
	
	public void setOldShadow(PrismObject<ShadowType> oldShadow) {
		this.oldShadow = oldShadow;
	}
	
	public void setCurrentShadow(PrismObject<ShadowType> currentShadow) {
		this.currentShadow = currentShadow;
	}
	
	public void setResource(PrismObject<ResourceType> resource) {
		this.resource = resource;
	}
	
	

}

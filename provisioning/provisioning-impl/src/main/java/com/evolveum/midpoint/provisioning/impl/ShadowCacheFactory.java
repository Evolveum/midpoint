package com.evolveum.midpoint.provisioning.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


@Component
public class ShadowCacheFactory {
	
	protected enum Mode { STANDARD, RECON}
	
	@Autowired(required = true)
	private ShadowCacheProvisioner shadowCacheStandard;
	
	@Autowired(required = true)
	private ShadowCacheReconciler shadowCacheFinisher;
	
	public ShadowCache getShadowCache(Mode mode) {
		switch (mode){
		case STANDARD:
			return shadowCacheStandard;
		case RECON:
			return shadowCacheFinisher;
		default:
			return shadowCacheStandard;
		}
	}

}

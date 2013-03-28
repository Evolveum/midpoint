/*
 * Copyright (c) 2013 Evolveum
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
 * Portions Copyrighted 2013 [name of copyright owner]
 */

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

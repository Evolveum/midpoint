/**
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
package com.evolveum.midpoint.model.lens;

import java.util.ArrayList;
import java.util.Collection;

import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.schema.util.SchemaDebugUtil;

/**
 * @author semancik
 *
 */
public class AccountConstructionPack {
	
	private Collection<PrismPropertyValue<AccountConstruction>> constructions = new ArrayList<PrismPropertyValue<AccountConstruction>>();
	private boolean forceRecon;
	
	public boolean isForceRecon() {
		return forceRecon;
	}
	
	public void setForceRecon(boolean forceRecon) {
		this.forceRecon = forceRecon;
	}
	
	public Collection<PrismPropertyValue<AccountConstruction>> getConstructions() {
		return constructions;
	}

	public void add(PrismPropertyValue<AccountConstruction> construction) {
		constructions.add(construction);
	}

	@Override
	public String toString() {
		return "AccountConstructionPack(" + SchemaDebugUtil.prettyPrint(constructions) + (forceRecon ? ", forceRecon" : "") + ")";
	}

}

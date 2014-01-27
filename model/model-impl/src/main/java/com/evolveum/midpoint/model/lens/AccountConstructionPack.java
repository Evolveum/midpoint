/*
 * Copyright (c) 2010-2013 Evolveum
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
	
	private Collection<PrismPropertyValue<Construction>> constructions = new ArrayList<PrismPropertyValue<Construction>>();
	private boolean forceRecon;
	
	public boolean isForceRecon() {
		return forceRecon;
	}
	
	public void setForceRecon(boolean forceRecon) {
		this.forceRecon = forceRecon;
	}
	
	public Collection<PrismPropertyValue<Construction>> getConstructions() {
		return constructions;
	}

	public void add(PrismPropertyValue<Construction> construction) {
		constructions.add(construction);
	}

	@Override
	public String toString() {
		return "AccountConstructionPack(" + SchemaDebugUtil.prettyPrint(constructions) + (forceRecon ? ", forceRecon" : "") + ")";
	}

}

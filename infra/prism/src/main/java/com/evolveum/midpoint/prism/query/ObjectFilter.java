/*
 * Copyright (c) 2010-2015 Evolveum
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

package com.evolveum.midpoint.prism.query;

import java.io.Serializable;

import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.Revivable;
import com.evolveum.midpoint.prism.match.MatchingRuleRegistry;
import com.evolveum.midpoint.prism.marshaller.QueryConvertor;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.exception.SchemaException;

public abstract class ObjectFilter implements DebugDumpable, Serializable, Revivable {

	transient private PrismContext prismContext;

	/**
	 * Does a SHALLOW clone.
	 */
	public abstract ObjectFilter clone();
	
	public abstract boolean match(PrismContainerValue value, MatchingRuleRegistry matchingRuleRegistry) throws SchemaException;
	
	public void accept(Visitor visitor) {
		visitor.visit(this);
	}

	@Override
	public void revive(PrismContext prismContext) throws SchemaException {
		QueryConvertor.revive(this, prismContext);
		this.prismContext = prismContext;
	}
	
	public abstract void checkConsistence(boolean requireDefinitions);

	public abstract boolean equals(Object o, boolean exact);

	public PrismContext getPrismContext() {
		return prismContext;
	}

	public void setPrismContext(PrismContext prismContext) {
		this.prismContext = prismContext;
	}
}

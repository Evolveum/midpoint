/*
 * Copyright (c) 2010-2018 Evolveum
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

import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.Revivable;
import com.evolveum.midpoint.prism.match.MatchingRuleRegistry;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.exception.SchemaException;

import java.io.Serializable;

public interface ObjectFilter extends DebugDumpable, Serializable, Revivable {

	/**
	 * Does a SHALLOW clone.
	 */
	ObjectFilter clone();
	
	boolean match(PrismContainerValue value, MatchingRuleRegistry matchingRuleRegistry) throws SchemaException;
	
	void accept(Visitor visitor);

	@Override
	void revive(PrismContext prismContext) throws SchemaException;

	void checkConsistence(boolean requireDefinitions);

	boolean equals(Object o, boolean exact);

	PrismContext getPrismContext();

	void setPrismContext(PrismContext prismContext);
}

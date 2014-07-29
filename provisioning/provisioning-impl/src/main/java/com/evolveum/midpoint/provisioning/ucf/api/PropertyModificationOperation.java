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
package com.evolveum.midpoint.provisioning.ucf.api;

import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.schema.util.SchemaDebugUtil;

/**
 * @author Radovan Semancik
 *
 */
public final class PropertyModificationOperation extends Operation {
	
	private PropertyDelta propertyDelta;
	
	public PropertyModificationOperation(PropertyDelta propertyDelta) {
		super();
		this.propertyDelta = propertyDelta;
	}
	
	public PropertyDelta getPropertyDelta() {
		return propertyDelta;
	}

	public void setPropertyDelta(PropertyDelta propertyDelta) {
		this.propertyDelta = propertyDelta;
	}

	@Override
	public String debugDump(int indent) {
		StringBuilder sb = new StringBuilder();
		SchemaDebugUtil.indentDebugDump(sb, indent);
		sb.append("Property modification operation:\n");
		sb.append(propertyDelta.debugDump(indent+1));
		return sb.toString();
	}

}
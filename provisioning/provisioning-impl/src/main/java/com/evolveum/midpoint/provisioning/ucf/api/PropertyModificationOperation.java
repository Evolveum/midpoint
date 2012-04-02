/*
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
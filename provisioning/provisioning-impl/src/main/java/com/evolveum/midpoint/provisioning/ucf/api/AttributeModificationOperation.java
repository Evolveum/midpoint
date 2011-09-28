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

import com.evolveum.midpoint.common.DebugUtil;
import com.evolveum.midpoint.schema.processor.Property;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyModificationTypeType;

/**
 * @author Radovan Semancik
 *
 */
public final class AttributeModificationOperation extends Operation {
	private PropertyModificationTypeType changeType;
	private Property newAttribute;

	public Property getNewAttribute() {
		return newAttribute;
	}
	
	public void setNewAttribute(Property newAttribute) {
		this.newAttribute = newAttribute;
	}

	public PropertyModificationTypeType getChangeType() {
		return changeType;
	}

	public void setChangeType(PropertyModificationTypeType changeType) {
		this.changeType = changeType;
	}

	@Override
	public String debugDump(int indent) {
		StringBuilder sb = new StringBuilder();
		DebugUtil.indentDebugDump(sb, indent);
		sb.append("Attribute modification ");
		sb.append(changeType);
		sb.append(": ");
		sb.append(newAttribute.debugDump(0));
		return sb.toString();
	}
	
}
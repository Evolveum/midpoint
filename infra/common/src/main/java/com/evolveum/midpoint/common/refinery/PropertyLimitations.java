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
package com.evolveum.midpoint.common.refinery;

import java.io.Serializable;

import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.Dumpable;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.PropertyAccessType;

/**
 * @author semancik
 *
 */
public class PropertyLimitations implements Dumpable, DebugDumpable, Serializable {
	
	private boolean ignore;
	private int minOccurs;
	private int maxOccurs;
	private PropertyAccessType access = new PropertyAccessType();
	
	public boolean isIgnore() {
		return ignore;
	}
	
	public void setIgnore(boolean ignore) {
		this.ignore = ignore;
	}
	
	public int getMinOccurs() {
		return minOccurs;
	}
	
	public void setMinOccurs(int minOccurs) {
		this.minOccurs = minOccurs;
	}
	
	public int getMaxOccurs() {
		return maxOccurs;
	}
	
	public void setMaxOccurs(int maxOccurs) {
		this.maxOccurs = maxOccurs;
	}
	
	public PropertyAccessType getAccess() {
		return access;
	}
	
	public void setAccess(PropertyAccessType access) {
		this.access = access;
	}

	@Override
	public String debugDump() {
		return debugDump(0);
	}

	@Override
	public String debugDump(int indent) {
		StringBuilder sb = new StringBuilder();
		DebugUtil.indentDebugDump(sb, indent);
		sb.append(toString());
		return sb.toString();
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("[").append(minOccurs).append(",").append(maxOccurs).append("]");
		sb.append(",");
		if (getAccess().isRead()) {
			sb.append("R");
		} else {
			sb.append("-");
		}
		if (getAccess().isCreate()) {
			sb.append("C");
		} else {
			sb.append("-");
		}
		if (getAccess().isUpdate()) {
			sb.append("U");
		} else {
			sb.append("-");
		}
		if (ignore) {
			sb.append(",ignored");
		}
		return sb.toString();
	}

	@Override
	public String dump() {
		return debugDump();
	}
	
	

}

/*
 * Copyright (c) 2010-2014 Evolveum
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
package com.evolveum.midpoint.common.refinery;

import java.io.Serializable;

import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PropertyAccessType;

/**
 * @author semancik
 *
 */
public class PropertyLimitations implements DebugDumpable, Serializable {

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
		if (getAccess().isAdd()) {
			sb.append("A");
		} else {
			sb.append("-");
		}
		if (getAccess().isModify()) {
			sb.append("M");
		} else {
			sb.append("-");
		}
		if (ignore) {
			sb.append(",ignored");
		}
		return sb.toString();
	}

}

/*
 * Copyright (c) 2010-2016 Evolveum
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
import com.evolveum.midpoint.util.DebugUtil;

import javax.xml.namespace.QName;

/**
 * @author Radovan Semancik
 *
 */
public final class PropertyModificationOperation<T> extends Operation {
	
	private PropertyDelta<T> propertyDelta;

	// Matching rule for entitlements can be specified at the level of association definition.
	// And we need this information, if avoidDuplicateValues == true.
	// So, in order to preserve it, we store it here.

	private QName matchingRuleQName;
	
	public PropertyModificationOperation(PropertyDelta<T> propertyDelta) {
		super();
		this.propertyDelta = propertyDelta;
	}
	
	public QName getMatchingRuleQName() {
		return matchingRuleQName;
	}

	public void setMatchingRuleQName(QName matchingRuleQName) {
		this.matchingRuleQName = matchingRuleQName;
	}

	public PropertyDelta getPropertyDelta() {
		return propertyDelta;
	}

	public void setPropertyDelta(PropertyDelta<T> propertyDelta) {
		this.propertyDelta = propertyDelta;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((matchingRuleQName == null) ? 0 : matchingRuleQName.hashCode());
		result = prime * result + ((propertyDelta == null) ? 0 : propertyDelta.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		PropertyModificationOperation other = (PropertyModificationOperation) obj;
		if (matchingRuleQName == null) {
			if (other.matchingRuleQName != null) {
				return false;
			}
		} else if (!matchingRuleQName.equals(other.matchingRuleQName)) {
			return false;
		}
		if (propertyDelta == null) {
			if (other.propertyDelta != null) {
				return false;
			}
		} else if (!propertyDelta.equals(other.propertyDelta)) {
			return false;
		}
		return true;
	}

	@Override
	public String debugDump(int indent) {
		StringBuilder sb = new StringBuilder();
		DebugUtil.debugDumpLabelLn(sb, "PropertyModificationOperation", indent);
		DebugUtil.debugDumpWithLabelLn(sb, "delta", propertyDelta, indent + 1);
		DebugUtil.debugDumpWithLabel(sb, "matchingRule", matchingRuleQName, indent + 1);
		return sb.toString();
	}
	
	@Override
	public String toString() {
		return propertyDelta.toString();
	}

}
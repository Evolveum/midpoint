/**
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
package com.evolveum.midpoint.model.lens;

import com.evolveum.midpoint.common.mapping.Mapping;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.Dumpable;

/**
 * @author semancik
 *
 */
public class PropertyValueWithOrigin implements Dumpable, DebugDumpable {
	
	private PrismPropertyValue<?> propertyValue;
	private Mapping<?> mapping;
	private AccountConstruction accountConstruction;
	
	public PropertyValueWithOrigin(PrismPropertyValue<?> propertyValue,
			Mapping<?> mapping, AccountConstruction accountConstruction) {
		super();
		this.propertyValue = propertyValue;
		this.mapping = mapping;
		this.accountConstruction = accountConstruction;
	}
	
	public PrismPropertyValue<?> getPropertyValue() {
		return propertyValue;
	}
	
	public Mapping<?> getMapping() {
		return mapping;
	}
	
	public AccountConstruction getAccountConstruction() {
		return accountConstruction;
	}

	public boolean equalsRealValue(PrismPropertyValue<?> pvalue) {
		if (propertyValue == null) {
			return false;
		}
		return propertyValue.equalsRealValue(pvalue);
	}
	
	public PropertyValueWithOrigin clone() {
		PropertyValueWithOrigin clone = new PropertyValueWithOrigin(propertyValue, mapping, accountConstruction);
		copyValues(clone);
		return clone;
	}

	protected void copyValues(PropertyValueWithOrigin clone) {
		if (this.propertyValue != null) {
			clone.propertyValue = this.propertyValue.clone();
		}
		if (this.mapping != null) {
			clone.mapping = this.mapping.clone();
		}
		clone.accountConstruction = this.accountConstruction;
	}

	@Override
	public String debugDump() {
		return debugDump(0);
	}

	@Override
	public String debugDump(int indent) {
		StringBuilder sb = new StringBuilder();
		DebugUtil.indentDebugDump(sb, indent);
		sb.append("PropertyValueWithOrigin:\n");
		DebugUtil.debugDumpWithLabel(sb, "propertyValue", propertyValue, indent +1);
		sb.append("\n");
		DebugUtil.debugDumpWithLabelToString(sb, "mapping", mapping, indent +1);
		sb.append("\n");
		DebugUtil.debugDumpWithLabelToString(sb, "accountConstruction", accountConstruction, indent +1);
		return sb.toString();
	}

	@Override
	public String dump() {
		return debugDump();
	}

	@Override
	public String toString() {
		return "PropertyValueWithOrigin(" + propertyValue + ", M="
				+ mapping + ", AC=" + accountConstruction + ")";
	}

}

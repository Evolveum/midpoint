/**
 * Copyright (c) 2017 Evolveum
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
package com.evolveum.midpoint.model.impl.lens.projector;

import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;

/**
 *
 * @author semancik
 */
public class SmartAssignmentElement implements DebugDumpable {

	private PrismContainerValue<AssignmentType> assignmentCVal;
	private boolean isCurrent = false;
	private boolean isOld = false;
	private boolean isChanged = false;

	public SmartAssignmentElement(PrismContainerValue<AssignmentType> assignmentCVal) {
		this.assignmentCVal = assignmentCVal;
	}

	public boolean isCurrent() {
		return isCurrent;
	}

	public void setCurrent(boolean isCurrent) {
		this.isCurrent = isCurrent;
	}

	public boolean isOld() {
		return isOld;
	}

	public void setOld(boolean isOld) {
		this.isOld = isOld;
	}

	public boolean isChanged() {
		return isChanged;
	}

	public void setChanged(boolean isChanged) {
		this.isChanged = isChanged;
	}

	public PrismContainerValue<AssignmentType> getAssignmentCVal() {
		return assignmentCVal;
	}

	public SmartAssignmentKey getKey() {
		return new SmartAssignmentKey(assignmentCVal);
	}

	@Override
	public String toString() {
		return "SAE(" + flag(isCurrent,"current") + flag(isOld,"old") + flag(isChanged,"changed") + ": " + assignmentCVal + ")";
	}

	private String flag(boolean b, String label) {
		if (b) {
			return label + ",";
		}
		return "";
	}

	@Override
	public String debugDump(int indent) {
		StringBuilder sb = new StringBuilder();
		DebugUtil.indentDebugDump(sb, indent);
		sb.append("SmartAssignmentElement: ");
		flag(sb, isCurrent, "current");
		flag(sb, isOld, "old");
		flag(sb, isChanged, "changed");
		sb.append("\n");
		sb.append(assignmentCVal.debugDump(indent + 1));
		return sb.toString();
	}

	private void flag(StringBuilder sb, boolean b, String label) {
		if (b) {
			sb.append(label).append(",");
		}
	}

	// TODO: equals, hashCode


}

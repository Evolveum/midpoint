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
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;

/**
 * @author semancik
 *
 */
public class SmartAssignmentKey {

	private static final Trace LOGGER = TraceManager.getTrace(SmartAssignmentKey.class);
	
	private PrismContainerValue<AssignmentType> assignmentCVal;

	public SmartAssignmentKey(PrismContainerValue<AssignmentType> assignmentCVal) {
		super();
		this.assignmentCVal = assignmentCVal;
	}

	// This is a key to an assignment hasmap.
	// hashCode() and equals() are very important here. 
	// Especially equals(). We want to make the comparison reliable, but reasonably quick.

	@Override
	public int hashCode() { 
		return assignmentCVal.hashCode();
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
		SmartAssignmentKey other = (SmartAssignmentKey) obj;
		if (assignmentCVal == null) {
			if (other.assignmentCVal != null) {
				return false;
			}
		} else if (!equalsAssignment(other.assignmentCVal)) {
			return false;
		}
		return true;
	}

	private boolean equalsAssignment(PrismContainerValue<AssignmentType> other) {
		return assignmentCVal.match(other);
	}

	@Override
	public String toString() {
		return "SmartAssignmentKey(" + assignmentCVal + ")";
	}
	
}

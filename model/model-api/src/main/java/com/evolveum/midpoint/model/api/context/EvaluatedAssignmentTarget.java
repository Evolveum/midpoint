/**
 * Copyright (c) 2015-2016 Evolveum
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
package com.evolveum.midpoint.model.api.context;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;

/**
 * @author semancik
 *
 */
public interface EvaluatedAssignmentTarget extends DebugDumpable {
	
	PrismObject<? extends FocusType> getTarget();

	boolean isDirectlyAssigned();

	// if this target applies to focus (by direct assignment or by some inducement)
	boolean appliesToFocus();

	/**
	 * True for roles whose constructions are evaluated - i.e. those roles that are considered to be applied
	 * to the focal object (e.g. to the user).
 	 */
	boolean isEvaluateConstructions();

	/**
	 * An assignment which assigns the given role (useful for knowing e.g. tenantRef or orgRef).
	 * TODO consider providing here also the "magic assignment"
	 * (https://wiki.evolveum.com/display/midPoint/Assignment+Configuration#AssignmentConfiguration-ConstructionVariables)
	 */
	AssignmentType getAssignment();

	AssignmentPath getAssignmentPath();

	boolean isValid();
}

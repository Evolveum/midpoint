/**
 * Copyright (c) 2018-2019 Evolveum
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
package com.evolveum.midpoint.model.test.asserter;

import static org.testng.AssertJUnit.assertNotNull;

import org.testng.AssertJUnit;

import com.evolveum.midpoint.model.api.AssignmentTargetSpecification;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.asserter.AbstractAsserter;

/**
 * @author semancik
 *
 */
public class AssignmentTargetSpecificationAsserter<RA> extends AbstractAsserter<RA> {
	
	private final AssignmentTargetSpecification targetSpec;

	public AssignmentTargetSpecificationAsserter(AssignmentTargetSpecification archetypeSpec, RA returnAsserter, String details) {
		super(returnAsserter, details);
		this.targetSpec = archetypeSpec;
	}
	
	AssignmentTargetSpecification getAssignmentTargetSpecification() {
		assertNotNull("Null " + desc(), targetSpec);
		return targetSpec;
	}
	
	public AssignmentTargetSpecificationAsserter<RA> assertNull() {
		AssertJUnit.assertNull("Unexpected " + desc(), targetSpec);
		return this;
	}
	
	public AssignmentTargetRelationsAsserter<AssignmentTargetSpecificationAsserter<RA>> assignmentTargetRelations() {
		AssignmentTargetRelationsAsserter<AssignmentTargetSpecificationAsserter<RA>> displayAsserter = new AssignmentTargetRelationsAsserter<>(getAssignmentTargetSpecification().getAssignmentTargetRelations(), this, "in " + desc());
		copySetupTo(displayAsserter);
		return displayAsserter;
	}
	
	public AssignmentTargetSpecificationAsserter<RA> display() {
		display(desc());
		return this;
	}
	
	public AssignmentTargetSpecificationAsserter<RA> display(String message) {
		IntegrationTestTools.display(message, targetSpec);
		return this;
	}
	
	@Override
	protected String desc() {
		return descWithDetails("archetype target specification");
	}
	
}

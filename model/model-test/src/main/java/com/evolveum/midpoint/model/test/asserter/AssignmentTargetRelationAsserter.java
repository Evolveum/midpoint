/**
 * Copyright (c) 2019 Evolveum
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

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

import java.util.List;

import com.evolveum.midpoint.model.api.AssignmentTargetRelation;
import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.test.asserter.AbstractAsserter;
import com.evolveum.midpoint.xml.ns._public.common.common_4.ObjectReferenceType;

/**
 * @author semancik
 *
 */
public class AssignmentTargetRelationAsserter<RA> extends AbstractAsserter<RA> {
	
	private final AssignmentTargetRelation assignmentTargetRelation;
	
	public AssignmentTargetRelationAsserter(AssignmentTargetRelation assignmentTargetRelation, RA returnAsserter, String desc) {
		super(returnAsserter, desc);
		this.assignmentTargetRelation = assignmentTargetRelation;
	}

	public AssignmentTargetRelationAsserter<RA> assertDescription(String expected) {
		assertEquals("Wrong description in "+desc(), expected, assignmentTargetRelation.getDescription());
		return this;
	}
	
	public AssignmentTargetRelationAsserter<RA> assertNoArchetype() {
		assertNotNull("Unexpected archetype references in "+desc()+": "+assignmentTargetRelation.getArchetypeRefs(), assignmentTargetRelation.getArchetypeRefs());
		return this;
	}
	
	public AssignmentTargetRelationAsserter<RA> assertArchetypeOid(String expected) {
		List<ObjectReferenceType> archetypeRefs = assignmentTargetRelation.getArchetypeRefs();
		if (archetypeRefs == null || archetypeRefs.isEmpty()) {
			fail("No archetype refs in "+desc());
		}
		if (archetypeRefs.size() > 1) {
			fail("Too many archetype refs in "+desc());
		}
		assertEquals("Wrong archetype ref in "+desc(), expected, archetypeRefs.get(0).getOid());
		return this;
	}
	// TODO
	
	@Override
	protected String desc() {
		return descWithDetails(assignmentTargetRelation);
	}

}

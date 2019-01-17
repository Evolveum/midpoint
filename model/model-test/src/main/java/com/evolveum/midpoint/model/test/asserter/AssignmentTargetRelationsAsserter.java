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
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.evolveum.midpoint.model.api.AssignmentTargetRelation;
import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;
import com.evolveum.midpoint.model.api.context.ModelProjectionContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.asserter.AbstractAsserter;
import com.evolveum.midpoint.test.asserter.ArchetypePolicyAsserter;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PendingOperationExecutionStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PendingOperationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.prism.xml.ns._public.types_3.ChangeTypeType;

/**
 * @author semancik
 *
 */
public class AssignmentTargetRelationsAsserter<RA> extends AbstractAsserter<RA> {
	
	private final List<AssignmentTargetRelation> assignmentTargetRelations;

	public AssignmentTargetRelationsAsserter(List<AssignmentTargetRelation> assignmentTargetRelations, RA returnAsserter, String details) {
		super(returnAsserter, details);
		this.assignmentTargetRelations = assignmentTargetRelations;
	}
		
	AssignmentTargetRelationAsserter<AssignmentTargetRelationsAsserter<RA>> forAssignmentTargetRelation(AssignmentTargetRelation view) {
		AssignmentTargetRelationAsserter<AssignmentTargetRelationsAsserter<RA>> asserter = new AssignmentTargetRelationAsserter<>(view, this, "assignment target relation in "+desc());
		copySetupTo(asserter);
		return asserter;
	}
	
	public  AssignmentTargetRelationFinder<RA> by() {
		return new  AssignmentTargetRelationFinder<>(this);
	}

	public List<AssignmentTargetRelation> getAssignmentTargetRelations() {
		return assignmentTargetRelations;
	}
	
	public AssignmentTargetRelationsAsserter<RA> assertItems(int expected) {
		assertEquals("Wrong number of assignment target relation in "+desc(), expected, getAssignmentTargetRelations().size());
		return this;
	}
	
	public AssignmentTargetRelationAsserter<AssignmentTargetRelationsAsserter<RA>> single() {
		assertItems(1);
		return forAssignmentTargetRelation(getAssignmentTargetRelations().get(0));
	}
	
	public AssignmentTargetRelationsAsserter<RA> display() {
		display(desc());
		return this;
	}
	
	public AssignmentTargetRelationsAsserter<RA> display(String message) {
		IntegrationTestTools.display(message, assignmentTargetRelations);
		return this;
	}
	
	@Override
	protected String desc() {
		return "assignment target relations of " + getDetails();
	}

	
}

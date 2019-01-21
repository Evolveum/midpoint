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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.xml.namespace.QName;

import org.testng.AssertJUnit;

import com.evolveum.midpoint.model.api.AssignmentTargetRelation;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;

/**
 * @author semancik
 *
 */
public class AssignmentTargetRelationFinder<RA> {
	
	private final AssignmentTargetRelationsAsserter<RA> collectionAsserter;
	private List<QName> targetTypes;
	private List<QName> relations;
	
	public AssignmentTargetRelationFinder(AssignmentTargetRelationsAsserter<RA> collectionAsserter) {
		this.collectionAsserter = collectionAsserter;
	}
	
	public AssignmentTargetRelationFinder<RA> targetType(QName qname) {
		targetTypes = new ArrayList<>();
		targetTypes.add(qname);
		return this;
	}
	
	public AssignmentTargetRelationFinder<RA> relation(QName qname) {
		relations = new ArrayList<>();
		relations.add(qname);
		return this;
	}
	
	public AssignmentTargetRelationFinder<RA> relations(QName... qnames) {
		relations = Arrays.asList(qnames);
		return this;
	}
	
	public AssignmentTargetRelationAsserter<AssignmentTargetRelationsAsserter<RA>> find() throws ObjectNotFoundException, SchemaException {
		AssignmentTargetRelation found = null;
		for (AssignmentTargetRelation item: collectionAsserter.getAssignmentTargetRelations()) {
			if (matches(item)) {
				if (found == null) {
					found = item;
				} else {
					fail("Found more than one assignment target relations that matches search criteria");
				}
			}
		}
		if (found == null) {
			fail("Found no assignment target relation that matches search criteria");
		}
		return collectionAsserter.forAssignmentTargetRelation(found);
	}
	
	public AssignmentTargetRelationsAsserter<RA> assertCount(int expectedCount) throws ObjectNotFoundException, SchemaException {
		int foundCount = 0;
		for (AssignmentTargetRelation item: collectionAsserter.getAssignmentTargetRelations()) {
			if (matches(item)) {
				foundCount++;
			}
		}
		assertEquals("Wrong number of assignment target relations for specified criteria in "+collectionAsserter.desc(), expectedCount, foundCount);
		return collectionAsserter;
	}
	
	private boolean matches(AssignmentTargetRelation item) throws ObjectNotFoundException, SchemaException {
		
		if (targetTypes != null) {
			if (!QNameUtil.unorderedCollectionMatch(targetTypes, item.getTargetTypes())) {
				return false;
			}
		}
		
		if (relations != null) {
			if (!QNameUtil.unorderedCollectionMatch(relations, item.getRelations())) {
				return false;
			}
		}
		
		// TODO: archetypes criterium
		
		return true;
	}
	
	protected void fail(String message) {
		AssertJUnit.fail(message);
	}

}

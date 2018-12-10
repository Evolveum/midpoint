/**
 * Copyright (c) 2018 Evolveum
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
package com.evolveum.midpoint.test.asserter;

import static org.testng.AssertJUnit.assertEquals;

import javax.xml.namespace.QName;

import org.testng.AssertJUnit;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;

/**
 * 
 * Note: considered to align this with ParentOrgRefFinder into some kind of common superclass.
 * But the resulting structure of generics is just too insane. It is lesser evil to have copy&pasted code. 
 * 
 * @author semancik
 */
public class ParentOrgRefFinder<O extends ObjectType, OA extends PrismObjectAsserter<O, RA>,RA> {

	private final ParentOrgRefsAsserter<O,OA,RA> refsAsserter;
	private String targetOid;
	private QName relation;
	
	public ParentOrgRefFinder(ParentOrgRefsAsserter<O,OA,RA> refsAsserter) {
		this.refsAsserter = refsAsserter;
	}
	
	public ParentOrgRefFinder<O,OA,RA> targetOid(String targetOid) {
		this.targetOid = targetOid;
		return this;
	}
	
	public ParentOrgRefFinder<O,OA,RA> relation(QName relation) {
		this.relation = relation;
		return this;
	}

	public ParentOrgRefAsserter<ParentOrgRefsAsserter<O, OA, RA>> find() throws ObjectNotFoundException, SchemaException {
		PrismReferenceValue found = null;
		PrismObject<OrgType> foundTarget = null;
		for (PrismReferenceValue ref: refsAsserter.getRefs()) {
			PrismObject<OrgType> refTarget = refsAsserter.getRefTarget(ref.getOid());
			if (matches(ref, refTarget)) {
				if (found == null) {
					found = ref;
					foundTarget = refTarget;
				} else {
					fail("Found more than one parentOrgRef that matches search criteria");
				}
			}
		}
		if (found == null) {
			fail("Found no parentOrgRef that matches search criteria");
		}
		return refsAsserter.forRef(found, foundTarget);
	}
	
	public ParentOrgRefsAsserter<O,OA,RA> assertCount(int expectedCount) throws ObjectNotFoundException, SchemaException {
		int foundCount = 0;
		for (PrismReferenceValue ref: refsAsserter.getRefs()) {
			PrismObject<OrgType> linkTarget = refsAsserter.getRefTarget(ref.getOid());
			if (matches(ref, linkTarget)) {
				foundCount++;
			}
		}
		assertEquals("Wrong number of links for specified criteria in "+refsAsserter.desc(), expectedCount, foundCount);
		return refsAsserter;
	}
	
	private boolean matches(PrismReferenceValue refVal, PrismObject<OrgType> refTarget) throws ObjectNotFoundException, SchemaException {
		OrgType refTargetType = refTarget.asObjectable();
		
		if (targetOid != null) {
			if (!targetOid.equals(refVal.getOid())) {
				return false;
			}
		}
		
		if (relation != null) {
			if (!QNameUtil.match(relation, refVal.getRelation())) {
				return false;
			}
		}		
		
		// TODO: more criteria
		return true;
	}

	protected void fail(String message) {
		AssertJUnit.fail(message);
	}

}

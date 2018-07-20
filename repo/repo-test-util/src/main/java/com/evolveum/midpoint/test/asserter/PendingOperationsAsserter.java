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
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertTrue;

import java.util.List;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PendingOperationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.prism.xml.ns._public.types_3.ChangeTypeType;

/**
 * @author semancik
 *
 */
public class PendingOperationsAsserter extends AbstractAsserter {
	
	private ShadowAsserter shadowAsserter;

	public PendingOperationsAsserter(ShadowAsserter shadowAsserter) {
		super();
		this.shadowAsserter = shadowAsserter;
	}
	
	public PendingOperationsAsserter(ShadowAsserter shadowAsserter, String details) {
		super(details);
		this.shadowAsserter = shadowAsserter;
	}
	
	public static PendingOperationsAsserter forShadow(PrismObject<ShadowType> shadow) {
		return new PendingOperationsAsserter(ShadowAsserter.forShadow(shadow));
	}
	
	List<PendingOperationType> getOperations() {
		return shadowAsserter.getObject().asObjectable().getPendingOperation();
	}
	
	public PendingOperationsAsserter assertOperations(int expectedNumber) {
		assertEquals("Unexpected number of pending operations in "+shadowAsserter.getObject(), expectedNumber, getOperations().size());
		return this;
	}
	
	PendingOperationAsserter forOperation(PendingOperationType operation) {
		return new PendingOperationAsserter(this, operation, idToString(operation.getId()), getDetails());
	}
	
	private String idToString(Long id) {
		if (id == null) {
			return "";
		} else {
			return id.toString();
		}
	}

	public PendingOperationAsserter singleOperation() {
		assertOperations(1);
		return forOperation(getOperations().get(0));
	}
	
	public PendingOperationsAsserter assertNone() {
		assertOperations(0);
		return this;
	}
	
	public PendingOperationAsserter modifyOperation() {
		return by()
			.changeType(ChangeTypeType.MODIFY)
			.find();
	}
	
	public PendingOperationFinder by() {
		return new PendingOperationFinder(this);
	}

	PrismObject<ShadowType> getShadow() {
		return shadowAsserter.getObject();
	}

	public ShadowAsserter end() {
		return shadowAsserter;
	}
}

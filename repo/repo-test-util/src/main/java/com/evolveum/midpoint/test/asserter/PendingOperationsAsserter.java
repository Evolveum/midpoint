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
import com.evolveum.midpoint.xml.ns._public.common.common_3.PendingOperationExecutionStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PendingOperationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.prism.xml.ns._public.types_3.ChangeTypeType;

/**
 * @author semancik
 *
 */
public class PendingOperationsAsserter<R> extends AbstractAsserter<ShadowAsserter<R>> {
	
	private ShadowAsserter<R> shadowAsserter;

	public PendingOperationsAsserter(ShadowAsserter<R> shadowAsserter) {
		super();
		this.shadowAsserter = shadowAsserter;
	}
	
	public PendingOperationsAsserter(ShadowAsserter<R> shadowAsserter, String details) {
		super(details);
		this.shadowAsserter = shadowAsserter;
	}
	
	public static PendingOperationsAsserter<Void> forShadow(PrismObject<ShadowType> shadow) {
		return new PendingOperationsAsserter<>(ShadowAsserter.forShadow(shadow));
	}
	
	List<PendingOperationType> getOperations() {
		return shadowAsserter.getObject().asObjectable().getPendingOperation();
	}
	
	public PendingOperationsAsserter<R> assertOperations(int expectedNumber) {
		assertEquals("Unexpected number of pending operations in "+shadowAsserter.getObject(), expectedNumber, getOperations().size());
		return this;
	}
	
	PendingOperationAsserter<R> forOperation(PendingOperationType operation) {
		PendingOperationAsserter<R> asserter = new PendingOperationAsserter<>(this, operation, idToString(operation.getId()), getDetails());
		copySetupTo(asserter);
		return asserter;
	}
	
	private String idToString(Long id) {
		if (id == null) {
			return "";
		} else {
			return id.toString();
		}
	}

	public PendingOperationAsserter<R> singleOperation() {
		assertOperations(1);
		return forOperation(getOperations().get(0));
	}
	
	public PendingOperationsAsserter<R> assertNone() {
		assertOperations(0);
		return this;
	}
	
	public PendingOperationAsserter<R> modifyOperation() {
		return by()
			.changeType(ChangeTypeType.MODIFY)
			.find();
	}
	
	public PendingOperationAsserter<R> addOperation() {
		return by()
			.changeType(ChangeTypeType.ADD)
			.find();
	}
	
	public PendingOperationAsserter<R> deleteOperation() {
		return by()
			.changeType(ChangeTypeType.DELETE)
			.find();
	}

	public PendingOperationsAsserter<R> assertUnfinishedOperation() {
		for (PendingOperationType operation: getOperations()) {
			if (isUnfinished(operation)) {
				return this;
			}
		}
		fail("No unfinished operations in "+desc());
		return null; // not reached
	}
	
	private boolean isUnfinished(PendingOperationType operation) {
		return operation.getExecutionStatus() != PendingOperationExecutionStatusType.COMPLETED;
	}

	public PendingOperationFinder<R> by() {
		return new PendingOperationFinder<>(this);
	}

	PrismObject<ShadowType> getShadow() {
		return shadowAsserter.getObject();
	}

	@Override
	public ShadowAsserter<R> end() {
		return shadowAsserter;
	}

	@Override
	protected String desc() {
		return descWithDetails("pending operations of "+shadowAsserter.getObject());
	}

}

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

/**
 * @author semancik
 *
 */
public class PendingOperationsAsserter extends AbstractAsserter {
	
	private PrismObject<ShadowType> shadow;

	public PendingOperationsAsserter(PrismObject<ShadowType> shadow) {
		super();
		this.shadow = shadow;
	}
	
	public PendingOperationsAsserter(PrismObject<ShadowType> shadow, String details) {
		super(details);
		this.shadow = shadow;
	}
	
	public static PendingOperationsAsserter forShadow(PrismObject<ShadowType> shadow) {
		return new PendingOperationsAsserter(shadow);
	}
	
	private List<PendingOperationType> getOperations() {
		return shadow.asObjectable().getPendingOperation();
	}
	
	public void assertOperations(int expectedNumber) {
		assertEquals("Unexpected number of pending operations in "+shadow, expectedNumber, getOperations().size());
	}
	
//	PendingOperationChecker 
	
//	PendingOperationAsserter find() {
//		
//	}
	
	public PendingOperationAsserter singleOperation() {
		assertOperations(1);
		return new PendingOperationAsserter(shadow, getOperations().get(0), "0", getDetails());
	}
	
	public PendingOperationsAsserter none() {
		assertOperations(0);
		return this;
	}

}

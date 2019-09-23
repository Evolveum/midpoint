/**
 * Copyright (c) 2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.test.asserter;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.xml.ns._public.common.common_3.IconType;
import org.testng.AssertJUnit;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

/**
 * @author semancik
 *
 */
public class OperationResultAsserter<RA> extends AbstractAsserter<RA> {

	private final OperationResult result;

	public OperationResultAsserter(OperationResult result, RA returnAsserter, String details) {
		super(returnAsserter, details);
		this.result = result;
	}

	public static final OperationResultAsserter<Void> forResult(OperationResult result) {
		return new OperationResultAsserter(result, null, null);
	}

	OperationResult getResult() {
		assertNotNull("Null " + desc(), result);
		return result;
	}
	
	public OperationResultAsserter<RA> assertNull() {
		AssertJUnit.assertNull("Unexpected " + desc(), result);
		return this;
	}
	
	public OperationResultAsserter<RA> assertStatus(OperationResultStatus expected) {
		assertEquals("Wrong status in "+desc(), expected, result.getStatus());
		return this;
	}

	public OperationResultAsserter<RA> assertSuccess() {
		assertEquals("Wrong status in "+desc(), OperationResultStatus.SUCCESS, result.getStatus());
		return this;
	}

	public OperationResultRepoSearchAsserter<OperationResultAsserter<RA>> repoSearches() {
		OperationResultRepoSearchAsserter<OperationResultAsserter<RA>> asserter = new OperationResultRepoSearchAsserter(result, this, getDetails());
		copySetupTo(asserter);
		return asserter;
	}

	public OperationResultAsserter<RA> display() {
		IntegrationTestTools.display(desc(), result);
		return this;
	}

	public OperationResultAsserter<RA> display(String message) {
		IntegrationTestTools.display(message, result);
		return this;
	}
	
	@Override
	protected String desc() {
		return descWithDetails("operation result " + result.getOperation());
	}
	
}

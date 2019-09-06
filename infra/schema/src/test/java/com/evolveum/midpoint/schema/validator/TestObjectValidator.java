/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schema.validator;

import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

import java.io.File;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.AbstractSchemaTest;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;

/**
 * @author semancik
 *
 */
public class TestObjectValidator extends AbstractSchemaTest {

	public static final File TEST_DIR = new File("src/test/resources/validator");
	
	protected static final File ROLE_ONE_FILE = new File(TEST_DIR, "role-one.xml");
	protected static final String ROLE_ONE_OID = "0d70504c-d094-11e8-b0cc-675c492577e7";

	@Test
	public void testValidateRoleOneDefault() throws Exception {
		final String TEST_NAME = "testValidateRoleOneDefault";
		displayTestTile(TEST_NAME);

		// GIVEN
		
		ObjectValidator validator = createValidator();
		
		PrismObject<RoleType> object = PrismTestUtil.getPrismContext().parseObject(ROLE_ONE_FILE);
		System.out.println("Object before validation:");
		System.out.println(object.debugDump(1));
		
		// WHEN
		ValidationResult validationResult = validator.validate(object);

		// THEN
		System.out.println("Validation result:");
		System.out.println(validationResult.debugDump(1));

		assertTrue("Unexpected rubbish in validation result", validationResult.isEmpty());
	}
	
	@Test
	public void testValidateRoleOneDeprecated() throws Exception {
		final String TEST_NAME = "testValidateRoleOneDeprecated";
		displayTestTile(TEST_NAME);

		// GIVEN
		
		ObjectValidator validator = createValidator();
		validator.setWarnDeprecated(true);
		
		PrismObject<RoleType> object = PrismTestUtil.getPrismContext().parseObject(ROLE_ONE_FILE);
		System.out.println("Object before validation:");
		System.out.println(object.debugDump(1));
		
		// WHEN
		ValidationResult validationResult = validator.validate(object);

		// THEN
		System.out.println("Validation result:");
		System.out.println(validationResult.debugDump(1));

		assertWarnings(validationResult, RoleType.F_ROLE_TYPE);
	}

	// We have no planned removal annotations in 4.0. Nothing to test.
//	@Test
//	public void testValidateRoleOnePlannedRemoval() throws Exception {
//		final String TEST_NAME = "testValidateRoleOnePlannedRemoval";
//		displayTestTile(TEST_NAME);
//
//		// GIVEN
//		
//		ObjectValidator validator = createValidator();
//		validator.setWarnPlannedRemoval(true);
//		
//		PrismObject<RoleType> object = PrismTestUtil.getPrismContext().parseObject(ROLE_ONE_FILE);
//		System.out.println("Object before validation:");
//		System.out.println(object.debugDump(1));
//		
//		// WHEN
//		ValidationResult validationResult = validator.validate(object);
//
//		// THEN
//		System.out.println("Validation result:");
//		System.out.println(validationResult.debugDump(1));
//
//		assertWarnings(validationResult, 
//				RoleType.F_APPROVER_EXPRESSION, RoleType.F_POLICY_CONSTRAINTS,
//				ItemPath.create(RoleType.F_POLICY_CONSTRAINTS, PolicyConstraintsType.F_MIN_ASSIGNEES, MultiplicityPolicyConstraintType.F_ENFORCEMENT));
//	}


	private ObjectValidator createValidator() {
		return new ObjectValidator(PrismTestUtil.getPrismContext());
	}

	private void assertWarnings(ValidationResult validationResult, Object... expectedItems) {
		for (Object expectedItem : expectedItems) {
			ItemPath expectedPath;
			if (expectedItem instanceof ItemPath) {
				expectedPath = (ItemPath)expectedItem;
			} else if (expectedItem instanceof QName) {
				expectedPath = ItemPath.create((QName)expectedItem);
			} else {
				throw new IllegalArgumentException("What? "+expectedItem);
			}
			ValidationItem valItem = findItem(validationResult, expectedPath);
			assertNotNull("No validation item for "+expectedPath, valItem);
			assertEquals("Wrong status in "+valItem, OperationResultStatus.WARNING, valItem.getStatus());
			PrismAsserts.assertPathEquivalent("Wrong path in "+valItem, expectedPath, valItem.getItemPath());
		}
		assertEquals("Unexpected size of validation result", expectedItems.length, validationResult.size());
	}

	private ValidationItem findItem(ValidationResult validationResult, ItemPath expectedPath) {
		for (ValidationItem valItem : validationResult.getItems()) {
			if (expectedPath.equivalent(valItem.getItemPath())) {
				return valItem;
			}
		}
		return null;
	}
}

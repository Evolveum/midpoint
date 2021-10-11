/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schema.validator;

import java.io.File;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.util.exception.SchemaException;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.AbstractSchemaTest;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;

import static org.testng.AssertJUnit.*;

/**
 * @author semancik
 */
public class TestObjectValidator extends AbstractSchemaTest {

    public static final File TEST_DIR = new File("src/test/resources/validator");

    // Contains elements that are deprecated and planned for removal, but still valid.
    protected static final File ROLE_ONE_FILE = new File(TEST_DIR, "role-one.xml");
    protected static final String ROLE_ONE_OID = "0d70504c-d094-11e8-b0cc-675c492577e7";

    // Contains elements that are already removed
    protected static final File ROLE_ONE_LEGACY_FILE = new File(TEST_DIR, "role-one-legacy.xml");
    protected static final String ROLE_ONE_LEGACY_OID = "50c00734-fee7-11e9-8f2f-4bf21a89fafb";

    @Test
    public void testValidateRoleOneDefault() throws Exception {
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

    /**
     * ROLE_ONE_LEGACY_FILE contains removed elements. This is COMPAT parsing, therefore
     * the parsing should go well.
     */
    @Test
    public void testValidateRoleOneLegacyCompat() throws Exception {
        // GIVEN

        ObjectValidator validator = createValidator();
        validator.setWarnDeprecated(true);

        PrismObject<RoleType> object = PrismTestUtil.getPrismContext()
                .parserFor(ROLE_ONE_LEGACY_FILE)
                .compat()
                .parse();
        System.out.println("Object before validation:");
        System.out.println(object.debugDump(1));

        // WHEN
        ValidationResult validationResult = validator.validate(object);

        // THEN
        System.out.println("Validation result:");
        System.out.println(validationResult.debugDump(1));

        assertWarnings(validationResult, RoleType.F_ROLE_TYPE);
    }

    /**
     * ROLE_ONE_LEGACY_FILE contains removed elements. This is STRICT parsing, therefore
     * the parsing should fail.
     */
    @Test
    public void testValidateRoleOneLegacyStrict() throws Exception {
        // GIVEN

        ObjectValidator validator = createValidator();
        validator.setWarnDeprecated(true);

        try {
            PrismObject<RoleType> object = PrismTestUtil.getPrismContext()
                    .parserFor(ROLE_ONE_LEGACY_FILE)
                    .strict()
                    .parse();
            System.out.println("Unexpected parsed object:");
            System.out.println(object.debugDump(1));
            fail("Unexpected success");
        } catch (SchemaException e) {
            // this is expected
        }

    }

    // We have no planned removal annotations in 4.0. Nothing to test.
//    @Test
//    public void testValidateRoleOnePlannedRemoval() throws Exception {
//        // GIVEN
//        ObjectValidator validator = createValidator();
//        validator.setWarnPlannedRemoval(true);
//
//        PrismObject<RoleType> object = PrismTestUtil.getPrismContext().parseObject(ROLE_ONE_FILE);
//        System.out.println("Object before validation:");
//        System.out.println(object.debugDump(1));
//
//        // WHEN
//        ValidationResult validationResult = validator.validate(object);
//
//        // THEN
//        System.out.println("Validation result:");
//        System.out.println(validationResult.debugDump(1));
//
//        assertWarnings(validationResult,
//                RoleType.F_APPROVER_EXPRESSION, RoleType.F_POLICY_CONSTRAINTS,
//                ItemPath.create(RoleType.F_POLICY_CONSTRAINTS, PolicyConstraintsType.F_MIN_ASSIGNEES, MultiplicityPolicyConstraintType.F_ENFORCEMENT));
//    }


    private ObjectValidator createValidator() {
        return new ObjectValidator(PrismTestUtil.getPrismContext());
    }

    private void assertWarnings(ValidationResult validationResult, Object... expectedItems) {
        for (Object expectedItem : expectedItems) {
            ItemPath expectedPath;
            if (expectedItem instanceof ItemPath) {
                expectedPath = (ItemPath)expectedItem;
            } else if (expectedItem instanceof QName) {
                expectedPath = ItemPath.create(expectedItem);
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

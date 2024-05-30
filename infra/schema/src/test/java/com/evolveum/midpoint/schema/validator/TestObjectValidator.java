/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schema.validator;

import static org.testng.AssertJUnit.*;

import java.io.File;
import java.util.Map;
import java.util.stream.Collectors;
import javax.xml.namespace.QName;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.AbstractSchemaTest;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CredentialsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PasswordType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;

/**
 * @author semancik
 */
public class TestObjectValidator extends AbstractSchemaTest {

    public static final File TEST_DIR = new File("src/test/resources/validator");

    public static final File ROLE = new File(TEST_DIR, "role.xml");

    // Contains elements that are deprecated and planned for removal, but still valid.
    protected static final File ROLE_ONE_FILE = new File(TEST_DIR, "role-one.xml");
    protected static final File ROLE_TWO_FILE = new File(TEST_DIR, "role-two.xml");
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
        validator.setTypeToCheck(ValidationItemType.DEPRECATED_ITEM, true);

        PrismObject<RoleType> object = PrismTestUtil.getPrismContext().parseObject(ROLE_ONE_FILE);
        System.out.println("Object before validation:");
        System.out.println(object.debugDump(1));

        // WHEN
        ValidationResult validationResult = validator.validate(object);

        // THEN
        System.out.println("Validation result:");
        System.out.println(validationResult.debugDump(1));

        assertWarnings(validationResult, RoleType.F_SUBTYPE);
    }

    /**
     * ROLE_ONE_LEGACY_FILE contains removed elements. This is COMPAT parsing, therefore
     * the parsing should go well.
     */
    @Test
    public void testValidateRoleOneLegacyCompat() throws Exception {
        // GIVEN

        ObjectValidator validator = createValidator();
        validator.setTypeToCheck(ValidationItemType.DEPRECATED_ITEM, true);

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

        assertWarnings(validationResult, RoleType.F_SUBTYPE);
    }

    /**
     * ROLE_ONE_LEGACY_FILE contains removed elements. This is STRICT parsing, therefore
     * the parsing should fail.
     */
    @Test
    public void testValidateRoleOneLegacyStrict() throws Exception {
        // GIVEN

        ObjectValidator validator = createValidator();
        validator.setTypeToCheck(ValidationItemType.DEPRECATED_ITEM, true);

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

    @Test
    public void testValidateRoleTwoOid() throws Exception {

        // GIVEN

        ObjectValidator validator = createValidator();
        validator.setTypeToCheck(ValidationItemType.INCORRECT_OID_FORMAT, true);

        PrismObject<RoleType> object = PrismTestUtil.getPrismContext().parseObject(ROLE_TWO_FILE);
        System.out.println("Object before validation:");
        System.out.println(object.debugDump(1));

        // WHEN
        ValidationResult validationResult = validator.validate(object);

        // THEN
        System.out.println("Validation result:");
        System.out.println(validationResult.debugDump(1));

        assertWarnings(validationResult, ItemPath.EMPTY_PATH);
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
        return new ObjectValidator();
    }

    private void assertWarnings(ValidationResult validationResult, Object... expectedItems) {
        for (Object expectedItem : expectedItems) {
            ItemPath expectedPath;
            if (expectedItem instanceof ItemPath) {
                expectedPath = (ItemPath) expectedItem;
            } else if (expectedItem instanceof QName) {
                expectedPath = ItemPath.create(expectedItem);
            } else {
                throw new IllegalArgumentException("What? " + expectedItem);
            }
            ValidationItem valItem = findItem(validationResult, expectedPath);
            assertNotNull("No validation item for " + expectedPath, valItem);
            assertEquals("Wrong status in " + valItem, ValidationItemStatus.WARNING, valItem.status());
            PrismAsserts.assertPathEquivalent("Wrong path in " + valItem, expectedPath, valItem.path());
        }
        assertEquals("Unexpected size of validation result", expectedItems.length, validationResult.size());
    }

    private ValidationItem findItem(ValidationResult validationResult, ItemPath expectedPath) {
        for (ValidationItem valItem : validationResult.getItems()) {
            if (expectedPath.equivalent(valItem.path())) {
                return valItem;
            }
        }
        return null;
    }

    private void assertValidationItem(ValidationResult result, ValidationItemType type, ItemPath itemPath) {
        long count = result.getItems().stream()
                .filter(i -> i.type() == type)
                .filter(i -> itemPath.equivalent(i.path()))
                .count();

        AssertJUnit.assertEquals(
                "Count for validation items of type=" + type + " and path=" + itemPath + " doesn't match", 1L, count);
    }

    @Test
    public void testNewValidations() throws Exception {
        ObjectValidator validator = new ObjectValidator();
        validator.setAllWarnings();
        validator.setSummarizeItemLifecycleState(false);
        validator.setWarnPlannedRemovalVersion("4.8");

        PrismObject<RoleType> object = PrismTestUtil.getPrismContext().parseObject(ROLE);
        ValidationResult result = validator.validate(object);

        AssertJUnit.assertNotNull(result);
        AssertJUnit.assertEquals(6, result.size());
        assertValidationItem(result, ValidationItemType.INCORRECT_OID_FORMAT, ItemPath.EMPTY_PATH);
        assertValidationItem(
                result,
                ValidationItemType.INCORRECT_OID_FORMAT,
                ItemPath.create(RoleType.F_INDUCEMENT, AssignmentType.F_TARGET_REF));
        assertValidationItem(
                result,
                ValidationItemType.PROTECTED_DATA_NOT_EXTERNAL,
                ItemPath.create(RoleType.F_CREDENTIALS, CredentialsType.F_PASSWORD, PasswordType.F_VALUE));
        assertValidationItem(result, ValidationItemType.DEPRECATED_ITEM, RoleType.F_SUBTYPE);
        assertValidationItem(result, ValidationItemType.MULTIVALUE_REF_WITHOUT_OID, RoleType.F_PARENT_ORG_REF);
        assertValidationItem(result, ValidationItemType.MISSING_NATURAL_KEY, RoleType.F_INDUCEMENT);

        Map<ValidationItemType, Long> countPerType = result.getItems().stream()
                .collect(Collectors.groupingBy(
                        i -> i.type(),
                        Collectors.counting()));

        AssertJUnit.assertEquals(1L, countPerType.get(ValidationItemType.PROTECTED_DATA_NOT_EXTERNAL).longValue());
        AssertJUnit.assertEquals(2L, countPerType.get(ValidationItemType.INCORRECT_OID_FORMAT).longValue());
//        AssertJUnit.assertEquals(1L, countPerType.get(ValidationItemType.MISSING_NATURAL_KEY));
        AssertJUnit.assertEquals(1L, countPerType.get(ValidationItemType.MULTIVALUE_REF_WITHOUT_OID).longValue());
        AssertJUnit.assertEquals(1L, countPerType.get(ValidationItemType.DEPRECATED_ITEM).longValue());
    }
}

/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.intest;

import static com.evolveum.midpoint.test.util.MidPointTestConstants.TEST_RESOURCES_DIR;

import java.io.File;
import java.io.IOException;

import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.icf.dummy.resource.DummyAccount;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyTestResource;
import com.evolveum.midpoint.test.TestResource;
import com.evolveum.midpoint.util.exception.CommonException;

/**
 * Tests the "multiple identities" feature. (Including smart correlation.)
 */
@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestIdentities extends AbstractEmptyModelIntegrationTest {

    private static final File TEST_DIR = new File(TEST_RESOURCES_DIR, "identities");

    private static final TestResource<ArchetypeType> ARCHETYPE_PERSON = new TestResource<>(
            TEST_DIR, "archetype-person.xml", "3a6f3ddd-ac72-4656-abac-0e306cd29645");
    private static final TestResource<ObjectTemplateType> OBJECT_TEMPLATE_PERSON = new TestResource<>(
            TEST_DIR, "object-template-person.xml", "c0d96ed0-bec7-4c6e-9a69-133b0301bdb8");

    private static final TestResource<UserType> USER_JOHN_SMITH = new TestResource<>(
            TEST_DIR, "user-john-smith.xml", "73566218-b455-4d8a-8c5b-326aab5c8291");
    private static final TestResource<UserType> USER_ALICE_GREEN = new TestResource<>(
            TEST_DIR, "user-alice-green.xml", "5ef2f22e-c1ea-459d-a7f0-1552eb49b1b0");

    private static final String ATTR_GIVEN_NAME = "givenName";
    private static final String ATTR_FAMILY_NAME = "familyName";
    private static final String ATTR_PERSONAL_NUMBER = "personalNumber";

    private static final String NS_ENT = "http://midpoint.evolveum.com/xml/ns/samples/enterprise";
    private static final ItemName ENT_PERSONAL_NUMBER = new ItemName(NS_ENT, "personalNumber");
    private static final ItemPath PATH_PERSONAL_NUMBER = ItemPath.create(ObjectType.F_EXTENSION, ENT_PERSONAL_NUMBER);

    public static final DummyTestResource RESOURCE_SINGLE = new DummyTestResource(
            TEST_DIR, "resource-single.xml", "157796ed-d4f2-429d-84f3-00ce4164263b", "single",
            controller -> {
                controller.addAttrDef(controller.getDummyResource().getAccountObjectClass(),
                        ATTR_GIVEN_NAME, String.class, false, false);
                controller.addAttrDef(controller.getDummyResource().getAccountObjectClass(),
                        ATTR_FAMILY_NAME, String.class, false, false);
                controller.addAttrDef(controller.getDummyResource().getAccountObjectClass(),
                        ATTR_PERSONAL_NUMBER, String.class, false, false);
            });

    public static final DummyTestResource RESOURCE_MULTI = new DummyTestResource(
            TEST_DIR, "resource-multi.xml", "7c75e7ed-ff61-4358-8023-61d85e93dcd4", "multi",
            controller -> {
                controller.addAttrDef(controller.getDummyResource().getAccountObjectClass(),
                        ATTR_GIVEN_NAME, String.class, false, false);
                controller.addAttrDef(controller.getDummyResource().getAccountObjectClass(),
                        ATTR_FAMILY_NAME, String.class, false, false);
                controller.addAttrDef(controller.getDummyResource().getAccountObjectClass(),
                        ATTR_PERSONAL_NUMBER, String.class, false, false);
            });

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);
        addObject(OBJECT_TEMPLATE_PERSON, initTask, initResult);
        addObject(ARCHETYPE_PERSON, initTask, initResult);
        initAndTestDummyResource(RESOURCE_SINGLE, initTask, initResult);
        initAndTestDummyResource(RESOURCE_MULTI, initTask, initResult);
    }

    /**
     * Checks identity data on user being added.
     */
    @Test
    public void test100AddJohnSmith() throws CommonException, IOException {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        when("John Smith is added");
        addObject(USER_JOHN_SMITH, task, result);

        then("status is success");
        assertThatOperationResult(result).isSuccess();

        and("smith1 is added");
        // @formatter:off
        assertUserAfterByUsername("smith1")
                .displayXml()
                .identities()
                    .assertSingle()
                    .withoutSource()
                        .assertNormalizedItem("givenName", "john")
                        .assertNormalizedItem("familyName", "smith")
                        .assertNormalizedItem("personalNumber", "1001234");
        // @formatter:on
    }

    /**
     * Checks identity data on user being added and modified.
     */
    @Test
    public void test110AddModifyAliceGreen() throws CommonException, IOException {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        when("Alice Green is added");
        addObject(USER_ALICE_GREEN, task, result);

        then("green1 is added");
        // @formatter:off
        assertUserAfterByUsername("green1")
                .displayXml()
                .identities()
                    .assertSingle()
                    .withoutSource()
                        .assertNormalizedItem("givenName", "alice")
                        .assertNormalizedItem("familyName", "green")
                        .assertNormalizedItem("personalNumber", "1005678");
        // @formatter:on

        when("Alice Green changed her name to Johnson");
        modifyObjectReplaceProperty(
                UserType.class,
                USER_ALICE_GREEN.oid,
                UserType.F_FAMILY_NAME,
                task,
                result,
                PolyString.fromOrig("Johnson"));

        then("status is success");
        assertThatOperationResult(result).isSuccess();

        and("johnson1 is there");
        // @formatter:off
        assertUserAfterByUsername("johnson1")
                .displayXml()
                .identities()
                    .assertSingle()
                    .withoutSource()
                        .assertNormalizedItem("givenName", "alice")
                        .assertNormalizedItem("familyName", "johnson")
                        .assertNormalizedItem("personalNumber", "1005678");
        // @formatter:on
    }

    /**
     * Imports Bob's account from `single` (no conflict). Checks that identity data related to an account is correctly imported.
     */
    @Test
    public void test120ImportAccountBobFromSingle() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        given("account of Bob White is added");
        DummyAccount account = RESOURCE_SINGLE.controller.addAccount("bob");
        account.addAttributeValue(ATTR_GIVEN_NAME, "Bob");
        account.addAttributeValue(ATTR_FAMILY_NAME, "White");
        account.addAttributeValue(ATTR_PERSONAL_NUMBER, "1003456");

        when("account is imported");
        importSingleAccountRequest()
                .withResourceOid(RESOURCE_SINGLE.oid)
                .withNameValue("bob")
                .execute(result);

        then("white1 is added");
        // @formatter:off
        assertUserAfterByUsername("white1")
                .displayXml()
                .identities()
                    .fromResource(RESOURCE_SINGLE.oid, ShadowKindType.ACCOUNT, "default", null)
                        .assertOriginalItem(UserType.F_GIVEN_NAME, createPolyString("Bob"))
                        .assertOriginalItem(UserType.F_FAMILY_NAME, createPolyString("White"))
                        .assertOriginalItem(PATH_PERSONAL_NUMBER, "1003456")
                    .end()
                    .withoutSource()
                        .assertNormalizedItem("givenName", "bob")
                        .assertNormalizedItem("familyName", "white")
                        .assertNormalizedItem("personalNumber", "1003456")
                    .end();
        // @formatter:on
    }

    /**
     * Imports Chuck's account (gradually) from two "multi" accounts and then a "single" account.
     * Checks that inbounds are correctly processed, including setting the identities.
     */
    @Test
    public void test130ImportAccountChuckFromVariousSources() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        given("first account of Chuck Brown is added");
        DummyAccount account1 = RESOURCE_MULTI.controller.addAccount("10700020");
        account1.addAttributeValue(ATTR_GIVEN_NAME, "Chuck");
        account1.addAttributeValue(ATTR_FAMILY_NAME, "Brown");
        account1.addAttributeValue(ATTR_PERSONAL_NUMBER, "1004444");

        when("account is imported");
        importSingleAccountRequest()
                .withResourceOid(RESOURCE_MULTI.oid)
                .withNameValue("10700020")
                .execute(result);

        then("brown1 is added");
        // @formatter:off
        assertUserAfterByUsername("brown1")
                .displayXml()
                .assertLiveLinks(1)
                .assertGivenName("Chuck")
                .identities()
                    .fromResource(RESOURCE_MULTI.oid, ShadowKindType.ACCOUNT, "default", "10700020")
                        .assertOriginalItem(UserType.F_GIVEN_NAME, createPolyString("Chuck"))
                        .assertOriginalItem(UserType.F_FAMILY_NAME, createPolyString("Brown"))
                        .assertOriginalItem(PATH_PERSONAL_NUMBER, "1004444")
                    .end()
                    .withoutSource()
                        .assertNormalizedItem("givenName", "chuck")
                        .assertNormalizedItem("familyName", "brown")
                        .assertNormalizedItem("personalNumber", "1004444")
                    .end();
        // @formatter:on

        when("second account of Chuck Brown is added");
        DummyAccount account2 = RESOURCE_MULTI.controller.addAccount("10700010");
        account2.addAttributeValue(ATTR_GIVEN_NAME, "Charles");
        account2.addAttributeValue(ATTR_FAMILY_NAME, "Brown");
        account2.addAttributeValue(ATTR_PERSONAL_NUMBER, "1004444");

        and("second account is imported");
        importSingleAccountRequest()
                .withResourceOid(RESOURCE_MULTI.oid)
                .withNameValue("10700010")
                .execute(result);

        then("brown1 is (still) there, and updated");
        // @formatter:off
        assertUserAfterByUsername("brown1")
                .displayXml()
                .assertLiveLinks(2)
                .assertGivenName("Charles")
                .identities()
                    .fromResource(RESOURCE_MULTI.oid, ShadowKindType.ACCOUNT, "default", "10700020")
                        .assertOriginalItem(UserType.F_GIVEN_NAME, createPolyString("Chuck"))
                        .assertOriginalItem(UserType.F_FAMILY_NAME, createPolyString("Brown"))
                        .assertOriginalItem(PATH_PERSONAL_NUMBER, "1004444")
                    .end()
                    .fromResource(RESOURCE_MULTI.oid, ShadowKindType.ACCOUNT, "default", "10700010")
                        .assertOriginalItem(UserType.F_GIVEN_NAME, createPolyString("Charles"))
                        .assertOriginalItem(UserType.F_FAMILY_NAME, createPolyString("Brown"))
                        .assertOriginalItem(PATH_PERSONAL_NUMBER, "1004444")
                    .end()
                    .withoutSource()
                        .assertNormalizedItem("givenName", "chuck", "charles")
                        .assertNormalizedItem("familyName", "brown")
                        .assertNormalizedItem("personalNumber", "1004444")
                    .end();
        // @formatter:on

        when("third account of Chuck Brown is added");
        DummyAccount account3 = RESOURCE_SINGLE.controller.addAccount("brown");
        account3.addAttributeValue(ATTR_GIVEN_NAME, "Karl");
        account3.addAttributeValue(ATTR_FAMILY_NAME, "Brown");
        account3.addAttributeValue(ATTR_PERSONAL_NUMBER, "1004444");

        and("third account is imported");
        importSingleAccountRequest()
                .withResourceOid(RESOURCE_SINGLE.oid)
                .withNameValue("brown")
                .execute(result);

        then("brown1 is (still) there, and updated");
        // @formatter:off
        assertUserAfterByUsername("brown1")
                .displayXml()
                .assertLiveLinks(3)
                .assertGivenName("Karl")
                .identities()
                    .fromResource(RESOURCE_MULTI.oid, ShadowKindType.ACCOUNT, "default", "10700020")
                        .assertOriginalItem(UserType.F_GIVEN_NAME, createPolyString("Chuck"))
                        .assertOriginalItem(UserType.F_FAMILY_NAME, createPolyString("Brown"))
                        .assertOriginalItem(PATH_PERSONAL_NUMBER, "1004444")
                    .end()
                    .fromResource(RESOURCE_MULTI.oid, ShadowKindType.ACCOUNT, "default", "10700010")
                        .assertOriginalItem(UserType.F_GIVEN_NAME, createPolyString("Charles"))
                        .assertOriginalItem(UserType.F_FAMILY_NAME, createPolyString("Brown"))
                        .assertOriginalItem(PATH_PERSONAL_NUMBER, "1004444")
                    .end()
                    .fromResource(RESOURCE_SINGLE.oid, ShadowKindType.ACCOUNT, "default", null)
                        .assertOriginalItem(UserType.F_GIVEN_NAME, createPolyString("Karl"))
                        .assertOriginalItem(UserType.F_FAMILY_NAME, createPolyString("Brown"))
                        .assertOriginalItem(PATH_PERSONAL_NUMBER, "1004444")
                    .end()
                    .withoutSource()
                        .assertNormalizedItem("givenName", "chuck", "charles", "karl")
                        .assertNormalizedItem("familyName", "brown")
                        .assertNormalizedItem("personalNumber", "1004444")
                    .end();
        // @formatter:on
    }
}

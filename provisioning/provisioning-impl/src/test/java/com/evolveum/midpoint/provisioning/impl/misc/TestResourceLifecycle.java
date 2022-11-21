/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.misc;

import static com.evolveum.midpoint.schema.constants.SchemaConstants.*;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType.ACCOUNT;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType.ENTITLEMENT;

import static org.assertj.core.api.Assertions.assertThat;

import static com.evolveum.midpoint.schema.TaskExecutionMode.*;
import static com.evolveum.midpoint.test.util.MidPointTestConstants.TEST_RESOURCES_DIR;

import java.io.File;

import com.evolveum.midpoint.schema.util.Resource;

import org.jetbrains.annotations.NotNull;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.provisioning.impl.AbstractProvisioningIntegrationTest;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.schema.TaskExecutionMode;
import com.evolveum.midpoint.test.DummyTestResource;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

@ContextConfiguration(locations = "classpath:ctx-provisioning-test-main.xml")
@DirtiesContext
@Listeners({ com.evolveum.midpoint.tools.testng.AlphabeticalMethodInterceptor.class })
public class TestResourceLifecycle extends AbstractProvisioningIntegrationTest {

    private static final File TEST_DIR = new File(TEST_RESOURCES_DIR, "misc/lifecycle");

    private static final DummyTestResource RESOURCE_DUMMY_PROPOSED = new DummyTestResource(
            TEST_DIR, "resource-dummy-proposed.xml", "e2180cc3-365a-4c3d-81c3-d3407fbb722f", "proposed");
    private static final DummyTestResource RESOURCE_DUMMY_ACTIVE = new DummyTestResource(
            TEST_DIR, "resource-dummy-active.xml", "7e07397d-392d-438b-91ea-00e53a6e521c", "active");
    private static final DummyTestResource RESOURCE_DUMMY_PROPOSED_CLASSES = new DummyTestResource(
            TEST_DIR, "resource-dummy-proposed-classes.xml", "68c10e8e-3d4a-42ff-9916-3f4ba8317fde",
            "proposed-classes", controller -> controller.setExtendedSchema());
    private static final DummyTestResource RESOURCE_DUMMY_PROPOSED_TYPES = new DummyTestResource(
            TEST_DIR, "resource-dummy-proposed-types.xml", "a193c17a-c755-4373-8a20-9827f2910c61", "proposed-types");

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        RESOURCE_DUMMY_PROPOSED.initAndTest(this, initTask, initResult);
        RESOURCE_DUMMY_ACTIVE.initAndTest(this, initTask, initResult);
        RESOURCE_DUMMY_PROPOSED_CLASSES.initAndTest(this, initTask, initResult);
        RESOURCE_DUMMY_PROPOSED_TYPES.initAndTest(this, initTask, initResult);
    }

    private static final String I_EMPLOYEE = "employee";
    private static final String I_ADMIN = "admin";
    private static final String I_DEMO = "demo";
    private static final String I_DEFAULT = "default";
    private static final String I_MAIL_GROUP = "mail-group";
    private static final String I_SECURITY_GROUP = "security-group";

    /**
     * Checks if the classification is executed according to the specification
     * in https://docs.evolveum.com/midpoint/devel/design/simulations/simulated-shadows/#shadow-classification.
     */
    @Test
    public void test100ClassificationOnActiveResource() throws Exception {

        // The production object type with production task
        checkClassification(
                RESOURCE_DUMMY_ACTIVE,
                PRODUCTION,
                ACCOUNT,
                "e_test100_1",
                I_EMPLOYEE,
                I_DEMO,
                I_DEMO); // no reclassification

        // The production object type with "simulated production" task
        checkClassification(
                RESOURCE_DUMMY_ACTIVE,
                SIMULATED_PRODUCTION,
                ACCOUNT,
                "e_test100_2",
                I_EMPLOYEE,
                I_DEMO,
                I_DEMO); // still no reclassification

        // The production object type with "simulated development" task
        checkClassification(
                RESOURCE_DUMMY_ACTIVE,
                SIMULATED_DEVELOPMENT,
                ACCOUNT,
                "e_test100_3",
                I_EMPLOYEE,
                I_DEMO,
                I_DEMO); // even here no reclassification (type is "production")
    }

    /**
     * As {@link #test100ClassificationOnActiveResource()} but on "proposed" (in-development) resource.
     */
    @Test
    public void test110ClassificationOnProposedResource() throws Exception {

        // The non-production object type with production task
        checkClassification(
                RESOURCE_DUMMY_PROPOSED,
                PRODUCTION,
                ACCOUNT,
                "e_test110_1",
                I_EMPLOYEE,
                I_DEMO,
                I_DEMO); // no reclassification - we do not want to destroy the simulation results

        // The non-production object type with "simulated production" task
        checkClassification(
                RESOURCE_DUMMY_PROPOSED,
                SIMULATED_PRODUCTION,
                ACCOUNT,
                "e_test110_2",
                I_EMPLOYEE,
                I_DEMO,
                I_DEMO); // no reclassification - we do not want to destroy the simulation results

        // The non-production object type with "simulated development" task
        checkClassification(
                RESOURCE_DUMMY_PROPOSED,
                SIMULATED_DEVELOPMENT,
                ACCOUNT,
                "e_test110_3",
                I_EMPLOYEE,
                I_DEMO,
                I_EMPLOYEE); // classification is re-executed because of simulation task and non-production object type
    }

    /**
     * As {@link #test100ClassificationOnActiveResource()} but on resource where only some object classes are proposed.
     *
     * Accounts are active, groups and entitlements are proposed.
     */
    @Test
    public void test120ClassificationOnResourceWithProposedClasses() throws Exception {

        // The production object type with production task
        checkClassification(
                RESOURCE_DUMMY_PROPOSED_CLASSES,
                PRODUCTION,
                ACCOUNT,
                "e_test120_1",
                I_EMPLOYEE,
                I_DEFAULT,
                I_DEFAULT); // no reclassification

        // The production object type with "simulated production" task
        checkClassification(
                RESOURCE_DUMMY_PROPOSED_CLASSES,
                SIMULATED_PRODUCTION,
                ACCOUNT,
                "e_test120_2",
                I_EMPLOYEE,
                I_DEFAULT,
                I_DEFAULT); // no reclassification

        // The production object type with "simulated development" task
        checkClassification(
                RESOURCE_DUMMY_PROPOSED_CLASSES,
                SIMULATED_DEVELOPMENT,
                ACCOUNT,
                "e_test120_3",
                I_EMPLOYEE,
                I_DEFAULT,
                I_DEFAULT); // no reclassification

        // The non-production object type with production task
        checkClassification(
                RESOURCE_DUMMY_PROPOSED_CLASSES,
                PRODUCTION,
                ENTITLEMENT,
                "m_group120_4",
                I_MAIL_GROUP,
                I_SECURITY_GROUP,
                I_SECURITY_GROUP); // no reclassification (production-like mode)

        // The non-production object type with "simulated production" task
        checkClassification(
                RESOURCE_DUMMY_PROPOSED_CLASSES,
                SIMULATED_PRODUCTION,
                ENTITLEMENT,
                "m_test120_5",
                I_MAIL_GROUP,
                I_SECURITY_GROUP,
                I_SECURITY_GROUP); // no reclassification (production-like mode)

        // The non-production object type with "simulated development" task
        // Note that the lifecycle state is of the type "entitlement/mail-group" is "proposed" in spite of the type inheritance,
        // because the class "proposed" state makes the whole class proposed, without considering the declared states of
        // individual object types.
        checkClassification(
                RESOURCE_DUMMY_PROPOSED_CLASSES,
                SIMULATED_DEVELOPMENT,
                ENTITLEMENT,
                "m_test120_6",
                I_MAIL_GROUP,
                I_SECURITY_GROUP,
                I_MAIL_GROUP); // reclassification

        // TODO write a test that would check for "proposed" OC not overwriting anything at the level of OT ... if needed.
    }

    /**
     * As {@link #test100ClassificationOnActiveResource()} but on resource where only some object types are proposed.
     *
     * Types `employee` and `default` are active, while `admin` and `demo` are not.
     */
    @Test
    public void test130ClassificationOnResourceWithProposedTypes() throws Exception {

        // The production object type with production task
        checkClassification(
                RESOURCE_DUMMY_PROPOSED_TYPES,
                PRODUCTION,
                ACCOUNT,
                "e_test130_1",
                I_EMPLOYEE,
                I_DEFAULT,
                I_DEFAULT); // no reclassification

        // The production object type with "simulated production" task
        checkClassification(
                RESOURCE_DUMMY_PROPOSED_TYPES,
                SIMULATED_PRODUCTION,
                ACCOUNT,
                "e_test130_2",
                I_EMPLOYEE,
                I_DEFAULT,
                I_DEFAULT); // no reclassification

        // The production object type with "simulated development" task
        checkClassification(
                RESOURCE_DUMMY_PROPOSED_TYPES,
                SIMULATED_DEVELOPMENT,
                ACCOUNT,
                "e_test130_3",
                I_EMPLOYEE,
                I_DEFAULT,
                I_DEFAULT); // no reclassification

        // The non-production object type with production task
        checkClassification(
                RESOURCE_DUMMY_PROPOSED_TYPES,
                PRODUCTION,
                ACCOUNT,
                "a_test130_4",
                I_ADMIN,
                I_DEMO,
                I_DEMO); // no reclassification (production-like mode)

        // The non-production object type with "simulated production" task
        checkClassification(
                RESOURCE_DUMMY_PROPOSED_TYPES,
                SIMULATED_PRODUCTION,
                ACCOUNT,
                "a_test130_5",
                I_ADMIN,
                I_DEMO,
                I_DEMO); // no reclassification (production-like mode)

        // The non-production object type with "simulated development" task
        checkClassification(
                RESOURCE_DUMMY_PROPOSED_TYPES,
                SIMULATED_DEVELOPMENT,
                ACCOUNT,
                "a_test130_6",
                I_ADMIN,
                I_DEMO,
                I_ADMIN); // reclassification
    }

    @SuppressWarnings("SameParameterValue")
    private void checkClassification(
            DummyTestResource resource,
            TaskExecutionMode taskExecutionMode,
            ShadowKindType kind,
            String objectName,
            String intentAfterCreation,
            String changeTo,
            String intentAfterChange) throws Exception {

        Task task = getTestTask();
        OperationResult result = task.getResult();
        String modeSuffix = " (" + taskExecutionMode + ")";

        task.setExecutionMode(taskExecutionMode);
        try {

            given(kind + " " + objectName);
            if (kind == ACCOUNT) {
                resource.controller.addAccount(objectName);
            } else {
                resource.controller.addGroup(objectName);
            }

            when("it is retrieved the first time" + modeSuffix);
            PrismObject<ShadowType> account = searchByName(resource, kind, objectName, task, result);

            then("it has intent of '" + intentAfterCreation + "'" + modeSuffix);
            assertIntent(account, kind, intentAfterCreation);

            changeIntentAndCheck(account, resource, null, intentAfterCreation, task, result);
            changeIntentAndCheck(account, resource, changeTo, intentAfterChange, task, result);

        } finally {
            task.setExecutionMode(PRODUCTION);
        }
    }

    private void changeIntentAndCheck(
            PrismObject<ShadowType> account,
            DummyTestResource resource,
            String newIntent,
            String expectedIntent,
            Task task,
            OperationResult result) throws CommonException {

        String accountName = account.getName().getOrig();
        ShadowKindType kind = account.asObjectable().getKind();
        String modeSuffix = " (" + task.getExecutionMode() + ")";

        when("intent is set to '" + newIntent + "' and account is searched for again" + modeSuffix);
        changeIntent(account.getOid(), newIntent, result);
        account = searchByName(resource, kind, accountName, task, result);

        then("it has intent of '" + expectedIntent + "'" + modeSuffix);
        assertIntent(account, kind, expectedIntent);

        when("intent is set to '" + newIntent + "' and account is got by OID" + modeSuffix);
        changeIntent(account.getOid(), newIntent, result);
        account = provisioningService.getObject(ShadowType.class, account.getOid(), null, task, result);

        then("it has intent of '" + expectedIntent + "'" + modeSuffix);
        assertIntent(account, kind, expectedIntent);
    }

    private void assertIntent(PrismObject<ShadowType> account, ShadowKindType kind, String expected) {
        assertShadowAfter(account)
                .assertKind(kind)
                .assertIntent(expected)
                .assertNotSimulated();
    }

    private PrismObject<ShadowType> searchByName(
            DummyTestResource resource, ShadowKindType kind, String accountName, Task task, OperationResult result)
            throws CommonException {
        var accounts = provisioningService.searchObjects(
                ShadowType.class,
                Resource.of(resource.getObjectable())
                        .queryFor(kind == ACCOUNT ? RI_ACCOUNT_OBJECT_CLASS : RI_GROUP_OBJECT_CLASS)
                        .and().item(ShadowType.F_ATTRIBUTES, ICFS_NAME).eq(accountName)
                        .build(),
                null,
                task,
                result);
        assertThat(accounts).as("objects named " + accountName).hasSize(1);
        return accounts.get(0);
    }

    /** Shadows created in production mode should be there. */
    @Test
    public void test200CreateAccountsInProduction() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        when("accounts are created");
        String oidEmployee = provisioningService.addObject(
                createShadow(RESOURCE_DUMMY_PROPOSED_TYPES, I_EMPLOYEE, "e_test200"),
                null, null, task, result);
        String oidAdmin = provisioningService.addObject(
                createShadow(RESOURCE_DUMMY_PROPOSED_TYPES, I_ADMIN, "a_test200"),
                null, null, task, result);

        then("their states are correct");
        assertRepoShadow(oidEmployee, "repo shadow after (employee)")
                .display()
                .assertKind(ACCOUNT)
                .assertIntent(I_EMPLOYEE)
                .assertIsExists()
                .assertNotSimulated();
        assertRepoShadow(oidAdmin, "repo shadow after (admin)")
                .display()
                .assertKind(ACCOUNT)
                .assertIntent(I_ADMIN)
                .assertIsExists()
                .assertNotSimulated();
    }

    private void changeIntent(String shadowOid, String newIntent, OperationResult result) throws CommonException {
        repositoryService.modifyObject(
                ShadowType.class,
                shadowOid,
                deltaFor(ShadowType.class)
                        .item(ShadowType.F_INTENT)
                        .replace(newIntent)
                        .asItemDeltas(),
                result);
    }

    @SuppressWarnings("SameParameterValue")
    private @NotNull PrismObject<ShadowType> createShadow(DummyTestResource resource, String intent, String name)
            throws SchemaException, ConfigurationException {
        var shadow = new ShadowType()
                .resourceRef(resource.oid, ResourceType.COMPLEX_TYPE)
                .objectClass(RI_ACCOUNT_OBJECT_CLASS)
                .kind(ACCOUNT)
                .intent(intent)
                .beginAttributes()
                .<ShadowType>end()
                .asPrismObject();
        ResourceAttribute<String> nameAttr = resource.controller.createAccountAttribute(SchemaConstants.ICFS_NAME);
        nameAttr.setRealValue(name);
        shadow.findContainer(ShadowType.F_ATTRIBUTES).getValue().add(nameAttr);
        return shadow;
    }
}

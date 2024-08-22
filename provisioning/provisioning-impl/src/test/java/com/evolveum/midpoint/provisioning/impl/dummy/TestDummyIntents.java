/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.impl.dummy;

import static org.assertj.core.api.Assertions.assertThat;

import static com.evolveum.midpoint.schema.constants.SchemaConstants.RI_ACCOUNT_OBJECT_CLASS;
import static com.evolveum.midpoint.test.util.MidPointTestConstants.RI_GROUP;
import static com.evolveum.midpoint.test.util.MidPointTestConstants.RI_PRIV;

import java.io.File;

import com.evolveum.midpoint.schema.internals.InternalsConfig;

import com.evolveum.midpoint.util.exception.*;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.icf.dummy.resource.DummyAccount;
import com.evolveum.icf.dummy.resource.DummyPrivilege;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.Resource;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyTestResource;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * Tests various scenarios involving multiple object types (intents).
 *
 * See also `TestIntent` in `model-intest` module.
 */
@ContextConfiguration(locations = "classpath:ctx-provisioning-test-main.xml")
@DirtiesContext
public class TestDummyIntents extends AbstractDummyTest {

    public static final File TEST_DIR = new File(TEST_DIR_DUMMY, "dummy-intents");

    private static final DummyTestResource RESOURCE_DUMMY_NO_DEFAULT_ACCOUNT = new DummyTestResource(
            TEST_DIR, "resource-no-default-account.xml", "ae8a1129-1e81-4097-a1cf-6dacfc03e5ff",
            "no-default-account");
    private static final DummyTestResource RESOURCE_DUMMY_NO_DEFAULT_ACCOUNT_FIXED = new DummyTestResource(
            TEST_DIR, "resource-no-default-account-fixed.xml", "f75a77ad-db18-445f-a477-30fac55ec1ef",
            "no-default-account-fixed");
    private static final DummyTestResource RESOURCE_DUMMY_WITH_DEFAULT_ACCOUNT = new DummyTestResource(
            TEST_DIR, "resource-with-default-account.xml", "744fadc5-d868-485c-b452-0d1727c5c1eb",
            "with-default-account");

    private static final String INTENT_MAIN = "main";

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        initResource(RESOURCE_DUMMY_NO_DEFAULT_ACCOUNT, initTask, initResult);
        initResource(RESOURCE_DUMMY_NO_DEFAULT_ACCOUNT_FIXED, initTask, initResult);
        initResource(RESOURCE_DUMMY_WITH_DEFAULT_ACCOUNT, initTask, initResult);
    }

    private void initResource(DummyTestResource resource, Task initTask, OperationResult initResult) throws Exception {
        initDummyResource(resource, initResult);
        testResourceAssertSuccess(resource, initTask, initResult);

        var alice = resource.controller.addAccount("alice");
        var wheel = resource.controller.addGroup("wheel");
        var create = new DummyPrivilege("create");
        resource.controller.getDummyResource().addPrivilege(create);

        wheel.addMember(alice.getName());
        alice.addAttributeValue(DummyAccount.ATTR_PRIVILEGES_NAME, create.getName());
    }

    @Test
    public void test100RetrievingAccountsByObjectClass() throws Exception {
        deleteAllShadows();

        // associations are not part of the OC definition, so they are not fetched
        executeSearchByObjectClass(RESOURCE_DUMMY_NO_DEFAULT_ACCOUNT, false);

        // here they are part of the object class definition
        executeSearchByObjectClass(RESOURCE_DUMMY_NO_DEFAULT_ACCOUNT_FIXED, true);
        executeSearchByObjectClass(RESOURCE_DUMMY_WITH_DEFAULT_ACCOUNT, true);
    }

    @Test
    public void test110RetrievingAccountsByKindAndIntent() throws Exception {

        deleteAllShadows();

        executeSearchByKindAndIntent(RESOURCE_DUMMY_NO_DEFAULT_ACCOUNT_FIXED);
        executeSearchByKindAndIntent(RESOURCE_DUMMY_WITH_DEFAULT_ACCOUNT);

        skipTestIf(InternalsConfig.isShadowCachingOnByDefault(), "fails when shadow caching is enabled");
        executeSearchByKindAndIntent(RESOURCE_DUMMY_NO_DEFAULT_ACCOUNT);
    }


    private void executeSearchByObjectClass(DummyTestResource resource, boolean associationsVisible) throws CommonException {
        var task = getTestTask();
        var result = task.getResult();

        when("shadows are retrieved by object class");
        var shadows = provisioningService.searchShadows(
                Resource.of(resource.get())
                        .queryFor(RI_ACCOUNT_OBJECT_CLASS)
                        .build(),
                null, task, result);

        assertThat(shadows).hasSize(1);
        var shadow = shadows.iterator().next().getBean();

        if (associationsVisible) {
            then("shadow contains the associations");
            assertAssociationsPresent(shadow);
        } else {
            then("shadow does not contain the associations");
            assertAssociationsNotPresent(shadow);
        }
    }

    private void executeSearchByKindAndIntent(DummyTestResource resource) throws CommonException {
        var task = getTestTask();
        var result = task.getResult();

        when("shadows are retrieved by kind and intent");
        var shadows = provisioningService.searchShadows(
                Resource.of(resource.get())
                        .queryFor(ShadowKindType.ACCOUNT, INTENT_MAIN)
                        .build(),
                null, task, result);

        assertThat(shadows).hasSize(1);
        var shadow = shadows.iterator().next().getBean();

        then("shadow contains the associations");
        assertAssociationsPresent(shadow);
    }

    private void assertAssociationsPresent(ShadowType shadow) {
        assertShadow(shadow, "")
                .display()
                .assertKind(ShadowKindType.ACCOUNT)
                .assertIntent(INTENT_MAIN)
                .associations()
                .association(RI_GROUP)
                .assertSize(1)
                .end()
                .association(RI_PRIV)
                .assertSize(1)
                .end();
    }

    private void assertAssociationsNotPresent(ShadowType shadow) {
        assertShadow(shadow, "")
                .display()
                .assertKind(ShadowKindType.ACCOUNT)
                .assertIntent(INTENT_MAIN)
                .associations()
                .assertValuesCount(0)
                .end();
    }

    private void deleteAllShadows() throws CommonException {
        var result = getTestOperationResult();
        var shadows = repositoryService.searchObjects(ShadowType.class, null, null, result);
        for (var shadow : shadows) {
            repositoryService.deleteObject(ShadowType.class, shadow.getOid(), result);
        }
    }
}

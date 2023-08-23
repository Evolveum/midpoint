/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.testing.story;

import java.io.File;

import com.evolveum.icf.dummy.resource.DummyAccount;
import com.evolveum.icf.dummy.resource.DummyGroup;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.test.TestObject;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyTestResource;
import com.evolveum.midpoint.test.util.MidPointTestConstants;

import javax.xml.namespace.QName;

import static com.evolveum.midpoint.schema.constants.SchemaConstants.RI_ACCOUNT_OBJECT_CLASS;
import static com.evolveum.midpoint.schema.constants.SchemaConstants.RI_GROUP_OBJECT_CLASS;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Currently tests midPoint behavior related to MID-6230: verifies that reconciliation task
 * fixes unknown/null intents in shadows.
 *
 * Intentionally not part of the test suite.
 */
@ContextConfiguration(locations = { "classpath:ctx-story-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestEnergy extends AbstractStoryTest {

    public static final File TEST_DIR = new File(MidPointTestConstants.TEST_RESOURCES_DIR, "energy");

    private static final File SYSTEM_CONFIGURATION_FILE = new File(TEST_DIR, "system-configuration.xml");

    private static final DummyTestResource RESOURCE_AD = new DummyTestResource(TEST_DIR, "resource-dummy-ad.xml", "88ea07a2-2bca-4746-8276-07bdb4b014d5", "ad",
            controller -> {
                controller.populateWithDefaultSchema();
                controller.addAttrDef(controller.getDummyResource().getAccountObjectClass(),
                        "dummy", String.class, false, false); // just to make resource feel its schema is extended
            });

    private static final TestObject<TaskType> TASK_RECONCILE_AD = TestObject.file(TEST_DIR, "task-reconcile-ad.xml", "51bbde22-9c4e-4d04-8daf-5fef7043a368");
    private static final String INTENT_GROUP = "group";
    private static final String INTENT_WRONG = "wrong";

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        initDummyResource(RESOURCE_AD, initTask, initResult);

        RESOURCE_AD.controller.addGroup("MP_ALPHA");
        RESOURCE_AD.controller.addGroup("MP_BETA");
        RESOURCE_AD.controller.addGroup("MP_GAMMA");
        RESOURCE_AD.controller.addGroup("EXT_ONE");
        RESOURCE_AD.controller.addGroup("EXT_TWO");
        RESOURCE_AD.controller.addGroup("EXT_THREE");

        addObject(TASK_RECONCILE_AD, initTask, initResult);

//        predefinedTestMethodTracing = PredefinedTestMethodTracing.MODEL_PROVISIONING_LOGGING;
    }

    @Override
    protected File getSystemConfigurationFile() {
        return SYSTEM_CONFIGURATION_FILE;
    }

    @Test
    public void test000Sanity() throws Exception {
        Task task = getTestTask();
        assertSuccess(modelService.testResource(RESOURCE_AD.oid, task, task.getResult()));
    }

    @Test
    public void test100ReconcileAd() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        when();
        rerunTask(TASK_RECONCILE_AD.oid, result);

        then();
        assertTask(TASK_RECONCILE_AD.oid, "Reconciliation after")
                .display();

        QName groupOcName = RI_GROUP_OBJECT_CLASS;

        PrismObject<ResourceType> ad = getObject(ResourceType.class, RESOURCE_AD.oid);

        assertShadow(findShadowByName(groupOcName, "MP_ALPHA", ad, result), "after")
                .assertKind(ShadowKindType.ENTITLEMENT)
                .assertIntent(INTENT_GROUP);
        assertShadow(findShadowByName(groupOcName, "MP_BETA", ad, result), "after")
                .assertKind(ShadowKindType.ENTITLEMENT)
                .assertIntent(INTENT_GROUP);
        assertShadow(findShadowByName(groupOcName, "MP_GAMMA", ad, result), "after")
                .assertKind(ShadowKindType.ENTITLEMENT)
                .assertIntent(INTENT_GROUP);
        assertShadow(findShadowByName(groupOcName, "EXT_ONE", ad, result), "after")
                .assertKind(ShadowKindType.UNKNOWN)
                .assertIntent(SchemaConstants.INTENT_UNKNOWN);
        assertShadow(findShadowByName(groupOcName, "EXT_TWO", ad, result), "after")
                .assertKind(ShadowKindType.UNKNOWN)
                .assertIntent(SchemaConstants.INTENT_UNKNOWN);
        assertShadow(findShadowByName(groupOcName, "EXT_THREE", ad, result), "after")
                .assertKind(ShadowKindType.UNKNOWN)
                .assertIntent(SchemaConstants.INTENT_UNKNOWN);
    }

    @Test
    public void test110BreakShadowsAndReconcile() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        setKindIntent("MP_ALPHA", ShadowKindType.ENTITLEMENT, null, result);
        setKindIntent("MP_BETA", ShadowKindType.ENTITLEMENT, SchemaConstants.INTENT_UNKNOWN, result);
        setKindIntent("MP_GAMMA", ShadowKindType.ENTITLEMENT, INTENT_WRONG, result);
        setKindIntent("EXT_ONE", ShadowKindType.ENTITLEMENT, null, result);
        setKindIntent("EXT_TWO", ShadowKindType.ENTITLEMENT, SchemaConstants.INTENT_UNKNOWN, result);
        setKindIntent("EXT_THREE", ShadowKindType.ENTITLEMENT, INTENT_WRONG, result);

        when();
        rerunTask(TASK_RECONCILE_AD.oid, result);

        then();
        assertTask(TASK_RECONCILE_AD.oid, "Reconciliation after")
                .display();

        QName groupOcName = RI_GROUP_OBJECT_CLASS;
        PrismObject<ResourceType> ad = getObject(ResourceType.class, RESOURCE_AD.oid);

        assertShadow(findShadowByName(groupOcName, "MP_ALPHA", ad, result), "after")
                .assertKind(ShadowKindType.ENTITLEMENT)
                .assertIntent(INTENT_GROUP);
        assertShadow(findShadowByName(groupOcName, "MP_BETA", ad, result), "after")
                .assertKind(ShadowKindType.ENTITLEMENT)
                .assertIntent(INTENT_GROUP);
        assertShadow(findShadowByName(groupOcName, "MP_GAMMA", ad, result), "after")
                .assertKind(ShadowKindType.ENTITLEMENT)
                .assertIntent(INTENT_WRONG); // assuming this will not be fixed by reconciliation
        assertShadow(findShadowByName(groupOcName, "EXT_ONE", ad, result), "after")
                .assertKind(ShadowKindType.ENTITLEMENT) // looks like this is not touched by reconciliation
                .assertIntent(SchemaConstants.INTENT_UNKNOWN);
        assertShadow(findShadowByName(groupOcName, "EXT_TWO", ad, result), "after")
                .assertKind(ShadowKindType.ENTITLEMENT) // looks like this is not touched by reconciliation
                .assertIntent(SchemaConstants.INTENT_UNKNOWN);
        assertShadow(findShadowByName(groupOcName, "EXT_THREE", ad, result), "after")
                .assertKind(ShadowKindType.ENTITLEMENT) // looks like this is not touched by reconciliation
                .assertIntent(INTENT_WRONG); // assuming this will not be fixed by reconciliation
    }

    @SuppressWarnings("SameParameterValue")
    private void setKindIntent(String shadowName, ShadowKindType kind, String intent, OperationResult result) throws CommonException {
        QName groupOcName = RI_GROUP_OBJECT_CLASS;
        PrismObject<ResourceType> ad = getObject(ResourceType.class, RESOURCE_AD.oid);
        PrismObject<ShadowType> shadow = findShadowByName(groupOcName, shadowName, ad, result);
        repositoryService.modifyObject(ShadowType.class, shadow.getOid(),
                prismContext.deltaFor(ShadowType.class)
                        .item(ShadowType.F_KIND).replace(kind)
                        .item(ShadowType.F_INTENT).replace(intent)
                        .asItemDeltas(),
                null, result);
    }

    @Test(enabled = false) // unfinished test
    public void test110GetAccount() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        DummyGroup delta = RESOURCE_AD.controller.addGroup("MP_DELTA");
        DummyGroup four = RESOURCE_AD.controller.addGroup("EXT_FOUR");
        DummyAccount jim = RESOURCE_AD.controller.addAccount("jim");
        delta.addMember(jim.getName());
        four.addMember(jim.getName());

        when();
        SearchResultList<PrismObject<ShadowType>> shadows = modelService.searchObjects(ShadowType.class,
                ObjectQueryUtil.createResourceAndObjectClassQuery(
                        RESOURCE_AD.oid,
                        RI_ACCOUNT_OBJECT_CLASS),
                null, task, result);
        display("shadows", shadows);

        assertThat(shadows).hasSize(1);

        PrismObject<ShadowType> shadow = modelService.getObject(ShadowType.class, shadows.get(0).getOid(), null, task, result);
        display("shadow via getObject", shadow);

        then();

        QName groupOcName = RI_GROUP_OBJECT_CLASS;
        PrismObject<ResourceType> ad = getObject(ResourceType.class, RESOURCE_AD.oid);

        PrismObject<ShadowType> mp_delta = findShadowByName(groupOcName, "MP_DELTA", ad, result);
        PrismObject<ShadowType> ext_four = findShadowByName(groupOcName, "EXT_FOUR", ad, result);

        assertShadow(mp_delta, "after");
//                .assertKind(ShadowKindType.ENTITLEMENT)
//                .assertIntent("group");
        assertShadow(ext_four, "after");
//                .assertKind(ShadowKindType.UNKNOWN)
//                .assertIntent(SchemaConstants.INTENT_UNKNOWN);
    }
}

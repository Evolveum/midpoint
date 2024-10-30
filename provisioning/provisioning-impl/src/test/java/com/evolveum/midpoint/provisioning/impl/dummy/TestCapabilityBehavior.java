/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.dummy;

import java.io.File;
import java.util.Date;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.evolveum.icf.dummy.resource.DummyAccount;
import com.evolveum.icf.dummy.resource.DummyResource;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.provisioning.impl.AbstractProvisioningIntegrationTest;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyTestResource;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CapabilitiesType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowBehaviorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.BehaviorCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.CapabilityCollectionType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.LastLoginTimestampCapabilityType;

@ContextConfiguration(locations = "classpath:ctx-provisioning-test-main.xml")
@DirtiesContext
@Listeners({ com.evolveum.midpoint.tools.testng.AlphabeticalMethodInterceptor.class })
public class TestCapabilityBehavior extends AbstractProvisioningIntegrationTest {

    private static final File TEST_DIR = new File("src/test/resources/dummy/capability");

    /**
     * Native behavior capability: no
     * Simulated behavior configured capability: yes
     */
    private static final DummyTestResource RESOURCE_DUMMY_BEHAVIOR_SIMULATED =
            createResource("resource-dummy-behavior-simulated.xml", "4bac305c-ed1f-4919-9670-e11863156811", "behavior-simulated");
    /**
     * Native behavior capability: yes
     * Simulated behavior configured capability: yes
     */
    private static final DummyTestResource RESOURCE_DUMMY_BEHAVIOR_NATIVE_SIMULATED =
            createResource("resource-dummy-behavior-native-simulated.xml", "20af4a72-f8b8-473f-a6e8-90db8d55542a", "behavior-native-simulated");

    /**
     * Native behavior capability: yes
     * Simulated behavior configured capability: no
     */
    private static final DummyTestResource RESOURCE_DUMMY_BEHAVIOR_NATIVE =
            createResource("resource-dummy-behavior-native.xml", "8dce1680-63b4-41ea-bd68-e37cc697c82c", "behavior-native");

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        initResource(RESOURCE_DUMMY_BEHAVIOR_NATIVE, initTask, initResult);
        initResource(RESOURCE_DUMMY_BEHAVIOR_SIMULATED, initTask, initResult);
        initResource(RESOURCE_DUMMY_BEHAVIOR_NATIVE_SIMULATED, initTask, initResult);
    }

    private static DummyTestResource createResource(String fileName, String oid, String name) {
        return new DummyTestResource(
                TEST_DIR,
                fileName,
                oid, name,
                c -> c.getAccountObjectClass()
                        .addAttributeDefinition("customLastLoginDate", Date.class, false, false));
    }

    private void initResource(DummyTestResource resource, Task initTask, OperationResult initResult) throws Exception {
        resource.initAndTest(this, initTask, initResult);

        resource.controller.getAccountObjectClass().addAttributeDefinition("customLastLoginDate", Long.class, false, false);
    }

    private void assertBehaviorCapability(BehaviorCapabilityType behaviorCapability, boolean enabled) {
        if (!enabled) {
            AssertJUnit.assertNull(behaviorCapability);
            return;
        }

        AssertJUnit.assertNotNull(behaviorCapability);

        LastLoginTimestampCapabilityType lastLoginTimestampCapability = behaviorCapability.getLastLoginTimestamp();
        AssertJUnit.assertNotNull(lastLoginTimestampCapability);
    }

    /**
     * @param resource
     * @param checkNative if true then native capabilities are checked, if false then configured capabilities are checked
     * @param enabled
     */
    private void assertBehaviorCapability(PrismObject<ResourceType> resource, boolean checkNative, boolean enabled) {
        CapabilitiesType capabilities = resource.asObjectable().getCapabilities();
        if (capabilities == null && !enabled) {
            return;
        }
        AssertJUnit.assertNotNull(capabilities);
        CapabilityCollectionType collection = checkNative ? capabilities.getNative() : capabilities.getConfigured();
        if (collection == null && !enabled) {
            return;
        }
        AssertJUnit.assertNotNull(collection);

        BehaviorCapabilityType behaviorCapability = collection.getBehavior();
        if (behaviorCapability == null && !enabled) {
            return;
        }
        assertBehaviorCapability(behaviorCapability, enabled);
    }

    private void assertResourceBehaviorCapability(PrismObject<ResourceType> resource, boolean nativeEnabled, boolean configuredEnabled) {
        assertBehaviorCapability(resource, true, nativeEnabled);
        assertBehaviorCapability(resource, false, configuredEnabled);
    }

    /**
     * Native behavior capability enabled
     */
    @Test
    public void test100TestNativeCapability() throws Exception {
        DummyResource dummyResource = RESOURCE_DUMMY_BEHAVIOR_NATIVE.getDummyResource();
//        Resource.of().getCompleteSchema().getObjectTypeDefinition().getca
        final Date lastLoginDate = new Date();

        DummyAccount account = new DummyAccount("jack");
        account.setLastLoginDate(lastLoginDate);
        dummyResource.addAccount(account);

        ShadowType shadowObj = getShadow(RESOURCE_DUMMY_BEHAVIOR_NATIVE.oid);
        ShadowBehaviorType behavior = shadowObj.getBehavior();
        AssertJUnit.assertNotNull(behavior);
        AssertJUnit.assertEquals(lastLoginDate, behavior.getLastLoginTimestamp().toGregorianCalendar().getTime());
    }

    @Test
    public void test200TestConfiguredCapability() throws Exception {
        DummyResource dummyResource = RESOURCE_DUMMY_BEHAVIOR_SIMULATED.getDummyResource();

        final Date lastLoginDate = new Date();

        DummyAccount account = new DummyAccount("jack");
        account.addAttributeValue("customLastLoginDate", lastLoginDate.getTime());
        dummyResource.addAccount(account);

        ShadowType shadowObj = getShadow(RESOURCE_DUMMY_BEHAVIOR_SIMULATED.oid);
        ShadowBehaviorType behavior = shadowObj.getBehavior();
        AssertJUnit.assertNotNull(behavior);
        AssertJUnit.assertEquals(lastLoginDate, behavior.getLastLoginTimestamp().toGregorianCalendar().getTime());
    }

    private ShadowType getShadow(String resourceOid) throws Exception {
        ObjectQuery query = PrismContext.get().queryFor(ShadowType.class)
                .item(ShadowType.F_RESOURCE_REF).ref(resourceOid)
                .and()
                .item(ShadowType.F_OBJECT_CLASS).eq(SchemaConstants.RI_ACCOUNT_OBJECT_CLASS)
                .build();

        Task task = getTestTask();
        OperationResult result = task.getResult();

        SearchResultList<PrismObject<ShadowType>> shadows =
                provisioningService.searchObjects(ShadowType.class, query, null, task, result);

        AssertJUnit.assertNotNull(shadows);
        AssertJUnit.assertEquals(1, shadows.size());

        PrismObject<ShadowType> shadow = shadows.get(0);
        return shadow.asObjectable();
    }
}

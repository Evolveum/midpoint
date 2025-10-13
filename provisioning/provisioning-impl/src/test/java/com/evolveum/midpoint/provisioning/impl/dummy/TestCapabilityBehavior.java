/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.provisioning.impl.dummy;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.xml.namespace.QName;

import org.assertj.core.api.Assertions;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.evolveum.icf.dummy.resource.DummyAccount;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.provisioning.impl.AbstractProvisioningIntegrationTest;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.Resource;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyTestResource;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowBehaviorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.BehaviorCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.LastLoginTimestampCapabilityType;

@ContextConfiguration(locations = "classpath:ctx-provisioning-test-main.xml")
@DirtiesContext
@Listeners({ com.evolveum.midpoint.tools.testng.AlphabeticalMethodInterceptor.class })
public class TestCapabilityBehavior extends AbstractProvisioningIntegrationTest {

    private static final File TEST_DIR = new File("src/test/resources/dummy/capability");

    /**
     * Native behavior capability: no
     * Simulated behavior configured capability: no
     */
    private static final DummyTestResource RESOURCE_DUMMY_BEHAVIOR_NONE =
            createResource("resource-dummy-behavior-none.xml", "36af3fcf-849c-4a34-9d14-ff4ca035a533", "behavior-none");

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

    private static final DummyTestResource RESOURCE_DUMMY_BEHAVIOR_SIMULATED_FORMAT =
            createResource("resource-dummy-behavior-simulated-format.xml", "2ff74f69-4107-495d-9fbd-9251e6996ed3", "behavior-simulated-format");

    private static final String ATTR_LAST_LOGIN_DATE = "customLastLoginDate";

    private static final QName ATTR_LAST_LOGIN_DATE_QNAME = new QName(SchemaConstants.NS_RI, ATTR_LAST_LOGIN_DATE);

    private static final String ATTR_LAST_LOGIN_DATE_FORMAT = "customLastLoginDateFormatted";

    private static final QName ATTR_LAST_LOGIN_DATE_FORMAT_QNAME = new QName(SchemaConstants.NS_RI, ATTR_LAST_LOGIN_DATE_FORMAT);

    private static final long LAST_LOGIN_DATE = 1234567890L;

    private static final long CUSTOM_LAST_LOGIN_DATE = 9876543210L;

    private static final String CUSTOM_LAST_LOGIN_DATE_FORMATTED = "2021-07-01T12:34";

    private static final String ACCOUNT_JACK = "jack";

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        initResource(RESOURCE_DUMMY_BEHAVIOR_NATIVE, initTask, initResult);
        initResource(RESOURCE_DUMMY_BEHAVIOR_SIMULATED, initTask, initResult);
        initResource(RESOURCE_DUMMY_BEHAVIOR_NATIVE_SIMULATED, initTask, initResult);
        initResource(RESOURCE_DUMMY_BEHAVIOR_NONE, initTask, initResult);
        initResource(RESOURCE_DUMMY_BEHAVIOR_SIMULATED_FORMAT, initTask, initResult);
    }

    private void initResource(DummyTestResource resource, Task initTask, OperationResult initResult) throws Exception {
        resource.initAndTest(this, initTask, initResult);

        DummyAccount account = new DummyAccount(ACCOUNT_JACK);
        account.setLastLoginDate(new Date(LAST_LOGIN_DATE));
        account.addAttributeValue(ATTR_LAST_LOGIN_DATE, CUSTOM_LAST_LOGIN_DATE);
        account.addAttributeValue(ATTR_LAST_LOGIN_DATE_FORMAT, CUSTOM_LAST_LOGIN_DATE_FORMATTED);
        resource.controller.getDummyResource().addAccount(account);
    }

    private static DummyTestResource createResource(String fileName, String oid, String name) {
        return new DummyTestResource(
                TEST_DIR,
                fileName,
                oid, name,
                c -> {
                    c.getAccountObjectClass()
                            .addAttributeDefinition(ATTR_LAST_LOGIN_DATE, long.class, false, false);
                    c.getAccountObjectClass()
                            .addAttributeDefinition(ATTR_LAST_LOGIN_DATE_FORMAT, String.class, false, false);
                });
    }

    /**
     * Native behavior capability enabled, no simulated behavior capability.
     */
    @Test
    public void test100TestNativeCapability() throws Exception {
        assertBehaviorCapability(RESOURCE_DUMMY_BEHAVIOR_NATIVE.get(), true);

        ShadowType shadowObj = getShadow(RESOURCE_DUMMY_BEHAVIOR_NATIVE.oid);
        ShadowBehaviorType behavior = shadowObj.getBehavior();
        Assertions.assertThat(behavior).isNotNull();
        Assertions.assertThat(behavior.getLastLoginTimestamp().toGregorianCalendar().getTimeInMillis())
                .isEqualTo(LAST_LOGIN_DATE);
    }

    /**
     * No native capability, simulated capability configured.
     */
    @Test
    public void test200TestConfiguredCapability() throws Exception {
        assertBehaviorCapability(RESOURCE_DUMMY_BEHAVIOR_NATIVE.get(), true);

        ShadowType shadowObj = getShadow(RESOURCE_DUMMY_BEHAVIOR_SIMULATED.oid);
        ShadowBehaviorType behavior = shadowObj.getBehavior();
        Assertions.assertThat(behavior).isNotNull();
        Assertions.assertThat(behavior.getLastLoginTimestamp().toGregorianCalendar().getTimeInMillis())
                .isEqualTo(CUSTOM_LAST_LOGIN_DATE);

        Long customValue = ShadowUtil.getAttributeValue(shadowObj.asPrismObject(), ATTR_LAST_LOGIN_DATE_QNAME);
        Assertions.assertThat(customValue).isNull();
    }

    /**
     * Native capability enabled, simulated capability also configured.
     */
    @Test
    public void test300TestNativeAndConfiguredCapability() throws Exception {
        assertBehaviorCapability(RESOURCE_DUMMY_BEHAVIOR_NATIVE_SIMULATED.get(), true);

        ShadowType shadowObj = getShadow(RESOURCE_DUMMY_BEHAVIOR_NATIVE_SIMULATED.oid);
        ShadowBehaviorType behavior = shadowObj.getBehavior();
        Assertions.assertThat(behavior).isNotNull();
        Assertions.assertThat(behavior.getLastLoginTimestamp().toGregorianCalendar().getTimeInMillis())
                .isEqualTo(CUSTOM_LAST_LOGIN_DATE);

        Long customValue = ShadowUtil.getAttributeValue(shadowObj.asPrismObject(), ATTR_LAST_LOGIN_DATE_QNAME);
        Assertions.assertThat(customValue).isNull();
    }

    /**
     * No native capability, no simulated capability.
     */
    @Test
    public void test400TestNoCapability() throws Exception {
        assertBehaviorCapability(RESOURCE_DUMMY_BEHAVIOR_NONE.get(), false);

        ShadowType shadowObj = getShadow(RESOURCE_DUMMY_BEHAVIOR_NONE.oid);
        ShadowBehaviorType behavior = shadowObj.getBehavior();

        Assertions.assertThat(behavior).isNull();

        Object customValue = ShadowUtil.getAttributeValue(shadowObj.asPrismObject(), ATTR_LAST_LOGIN_DATE_QNAME);
        Assertions.assertThat(customValue)
                .isEqualTo(CUSTOM_LAST_LOGIN_DATE);
    }

    @Test
    public void test500TestConfiguredWithCustomFormat() throws Exception {
        assertBehaviorCapability(RESOURCE_DUMMY_BEHAVIOR_SIMULATED_FORMAT.get(), true);

        LastLoginTimestampCapabilityType capability = RESOURCE_DUMMY_BEHAVIOR_SIMULATED_FORMAT.getObjectable()
                .getSchemaHandling().getObjectType().get(0).getConfiguredCapabilities().getBehavior().getLastLoginTimestamp();

        String formatString = capability.getFormat();
        DateFormat format = new SimpleDateFormat(formatString);

        Date date = format.parse(CUSTOM_LAST_LOGIN_DATE_FORMATTED);

        ShadowType shadowObj = getShadow(RESOURCE_DUMMY_BEHAVIOR_SIMULATED_FORMAT.oid);
        ShadowBehaviorType behavior = shadowObj.getBehavior();
        Assertions.assertThat(behavior).isNotNull();
        Assertions.assertThat(behavior.getLastLoginTimestamp().toGregorianCalendar().getTimeInMillis())
                .isEqualTo(date.getTime());

        String customValue = ShadowUtil.getAttributeValue(shadowObj.asPrismObject(), ATTR_LAST_LOGIN_DATE_FORMAT_QNAME);
        Assertions.assertThat(customValue).isNull();
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

        Assertions.assertThat(shadows).hasSize(1);

        PrismObject<ShadowType> shadow = shadows.get(0);
        return shadow.asObjectable();
    }

    private void assertBehaviorCapability(
            PrismObject<ResourceType> resource, boolean enabled)
            throws SchemaException, ConfigurationException {

        BehaviorCapabilityType behaviorCap = Resource.of(resource).getCompleteSchema()
                .getObjectTypeDefinition(ShadowKindType.ACCOUNT, "default")
                .getEnabledCapability(BehaviorCapabilityType.class, resource.asObjectable());

        if (!enabled) {
            Assertions.assertThat(behaviorCap).isNull();
            return;
        }

        Assertions.assertThat(behaviorCap).isNotNull();

        LastLoginTimestampCapabilityType lastLoginTimestampCapability = behaviorCap.getLastLoginTimestamp();
        Assertions.assertThat(lastLoginTimestampCapability).isNotNull();
    }
}

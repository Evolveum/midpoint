/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.impl.dummy;

import static org.assertj.core.api.Assertions.assertThat;

import static com.evolveum.midpoint.schema.constants.SchemaConstants.*;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType.ACCOUNT;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Collectors;

import com.evolveum.midpoint.test.TestObject;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.icf.dummy.resource.DummyGroup;
import com.evolveum.midpoint.repo.cache.RepositoryCache;
import com.evolveum.midpoint.schema.GetOperationOptionsBuilder;
import com.evolveum.midpoint.schema.ResourceShadowCoordinates;
import com.evolveum.midpoint.schema.cache.CacheConfigurationManager;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.Resource;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyTestResource;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.caching.CachePerformanceCollector;

/**
 * Any other dummy-based tests that require specific resource configuration, and are not easily integrable into
 * the {@link AbstractBasicDummyTest}.
 */
@ContextConfiguration(locations = "classpath:ctx-provisioning-test-main.xml")
@DirtiesContext
public class TestDummyMisc extends AbstractDummyTest {

    protected static final File TEST_DIR = new File(TEST_DIR_DUMMY, "misc");

    private static final TestObject<?> SYSTEM_CONFIGURATION = TestObject.file(
            TEST_DIR, "system-configuration.xml", "00000000-0000-0000-0000-000000000001");

    private static final String HIDDEN_ATTR_1 = "hiddenAttr1";
    private static final String HIDDEN_ATTR_2 = "hiddenAttr2";

    private static final String ATTR_TYPE = "type";

    private static final String TYPE_1 = "type-1";

    private final Collection<DummyGroup> groups = new ArrayList<>();

    @Autowired private CacheConfigurationManager cacheConfigurationManager;

    private static final DummyTestResource RESOURCE_DUMMY_ATTRIBUTES_TO_GET = new DummyTestResource(
            TEST_DIR, "resource-dummy-attributes-to-get.xml", "82291fe7-d509-491a-9491-0086361d7c77", "attributes-to-get",
            c -> {
                var accountObjectClass = c.getDummyResource().getAccountObjectClass();
                var def1 = c.addAttrDef(accountObjectClass, HIDDEN_ATTR_1, String.class, false, false);
                def1.setReturnedByDefault(false);
                var def2 = c.addAttrDef(accountObjectClass, HIDDEN_ATTR_2, String.class, false, false);
                def2.setReturnedByDefault(false);
            }
    );

    private static final DummyTestResource RESOURCE_DUMMY_MANY_ASSOCIATED_INTENTS = new DummyTestResource(
            TEST_DIR, "resource-dummy-many-associated-intents.xml", "1f40e5e2-eb25-4587-8d34-47220e4a0663",
            "many-associated-intents",
            c -> {
                c.populateWithDefaultSchema();
                var groupObjectClass = c.getDummyResource().getGroupObjectClass();
                c.addAttrDef(groupObjectClass, ATTR_TYPE, String.class, false, false);
            }
    );

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        repoAdd(SYSTEM_CONFIGURATION, initResult);

        initDummyResource(RESOURCE_DUMMY_ATTRIBUTES_TO_GET, initResult);
        testResourceAssertSuccess(RESOURCE_DUMMY_ATTRIBUTES_TO_GET, initTask, initResult);

        initDummyResource(RESOURCE_DUMMY_MANY_ASSOCIATED_INTENTS, initResult);
        testResourceAssertSuccess(RESOURCE_DUMMY_MANY_ASSOCIATED_INTENTS, initTask, initResult);
        createGroup(TYPE_1, "A");
    }

    private void createGroup(String type, String name) throws Exception {
        var group = RESOURCE_DUMMY_MANY_ASSOCIATED_INTENTS.controller
                .addGroup(("group-%s-%s".formatted(name, type)));
        group.addAttributeValue(ATTR_TYPE, type);
        groups.add(group);
    }

    /** Testing "attributes to get" with attributes not returned by default (MID-9774). */
    @Test
    public void test100ComputingAttributesToGet() throws Exception {
        var task = getTestTask();
        var result = task.getResult();

        var coords = new ResourceShadowCoordinates(RESOURCE_DUMMY_ATTRIBUTES_TO_GET.oid, ACCOUNT, INTENT_DEFAULT);
        var ctx = provisioningContextFactory.createForShadowCoordinates(coords, task, result);

        when("attributes to return are computed");
        var attributesToReturn = ctx.createAttributesToReturn();

        then("attributes to return are OK, especially hiddenAttr2 is not requested");

        displayValue("attributesToReturn", attributesToReturn);

        assertThat(attributesToReturn.isReturnDefaultAttributes())
                .as("'return default attributes' flag")
                .isFalse();

        var attrNames = attributesToReturn.getAttributesToReturn().stream()
                .map(def -> def.getItemName().getLocalPart())
                .collect(Collectors.toSet());

        // hiddenAttr1 should not be here, as it has strategy of MINIMAL;
        // hiddenAttr2 should not be here as well, as it is not returned by default anyway
        assertThat(attrNames)
                .as("names of attributes to return")
                .containsExactlyInAnyOrder("uid", "name");
    }

    /** Fetching specific attribute (not returned by default) while there are no ones marked as "minimal" (MID-10585). */
    @Test
    public void test110RequestingAttributeToFetch() throws Exception {
        var task = getTestTask();
        var result = task.getResult();

        var coords = new ResourceShadowCoordinates(RESOURCE_DUMMY_ATTRIBUTES_TO_GET.oid, ACCOUNT, "mid-10585");
        var ctx = provisioningContextFactory.createForShadowCoordinates(coords, task, result);
        ctx.setGetOperationOptions(
                GetOperationOptionsBuilder.create()
                        .item(ShadowType.F_ATTRIBUTES.append(HIDDEN_ATTR_1)).retrieve() // not returned by default
                        .item(ShadowType.F_ATTRIBUTES.append("name")).retrieve() // returned by default
                        .build());

        when("attributes to return are computed");
        var itemsToReturn = ctx.createAttributesToReturn();

        then("attributes to return are OK, especially hiddenAttr1 is requested");

        displayValue("attributesToReturn", itemsToReturn);

        assertThat(itemsToReturn.isReturnDefaultAttributes())
                .as("'return default attributes' flag")
                .isTrue();

        var attributeDefinitions = itemsToReturn.getAttributesToReturn();
        assertThat(attributeDefinitions).as("items definitions").isNotNull();
        var attrNames = attributeDefinitions.stream()
                .map(def -> def.getItemName().getLocalPart())
                .collect(Collectors.toSet());

        // "name" is not here, because it is returned by default, so it does not need to be mentioned in the options
        assertThat(attrNames)
                .as("names of attributes to return")
                .containsExactlyInAnyOrder(HIDDEN_ATTR_1);
    }

    /** Fetching an object with multiple entitlements covering multiple intents (MID-10600). */
    @Test
    public void test200GettingObjectsAssociatedToManyIntents() throws Exception {
        var task = getTestTask();
        var result = task.getResult();
        var accountName = "account-1";

        given("an account in 8 groups");
        RESOURCE_DUMMY_MANY_ASSOCIATED_INTENTS.controller.addAccount(accountName);
        for (DummyGroup group : groups) {
            group.addMember(accountName);
        }

        var shadows = provisioningService.searchObjects(
                ShadowType.class,
                Resource.of(RESOURCE_DUMMY_MANY_ASSOCIATED_INTENTS.get())
                        .queryFor(RI_ACCOUNT_OBJECT_CLASS)
                        .and()
                        .item(ShadowType.F_ATTRIBUTES, ICFS_NAME).eq(accountName)
                        .build(),
                null, task, result);
        var oid = MiscUtil.extractSingletonRequired(shadows).getOid();

        when("the account is fetched");
        CachePerformanceCollector.INSTANCE.clear();
        RepositoryCache.enterLocalCaches(cacheConfigurationManager);
        try {
            provisioningService.getObject(ShadowType.class, oid, null, task, result);
        } finally {
            RepositoryCache.exitLocalCaches();
        }
        displayDumpable("cache performance", CachePerformanceCollector.INSTANCE);

        // TODO some asserts here
    }
}

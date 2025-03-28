/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.impl.dummy;

import static org.assertj.core.api.Assertions.assertThat;

import static com.evolveum.midpoint.schema.constants.SchemaConstants.INTENT_DEFAULT;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType.ACCOUNT;

import java.io.File;
import java.util.Collection;
import java.util.stream.Collectors;

import com.evolveum.midpoint.schema.GetOperationOptionsBuilder;

import com.evolveum.midpoint.schema.processor.ShadowAttributeDefinition;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.schema.ResourceShadowCoordinates;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyTestResource;

/**
 * Any other dummy-based tests that require specific resource configuration, and are not easily integrable into
 * the {@link AbstractBasicDummyTest}.
 */
@ContextConfiguration(locations = "classpath:ctx-provisioning-test-main.xml")
@DirtiesContext
public class TestDummyMisc extends AbstractDummyTest {

    protected static final File TEST_DIR = new File(TEST_DIR_DUMMY, "misc");

    private static final String HIDDEN_ATTR_1 = "hiddenAttr1";
    private static final String HIDDEN_ATTR_2 = "hiddenAttr2";

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

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        initDummyResource(RESOURCE_DUMMY_ATTRIBUTES_TO_GET, initResult);
        testResourceAssertSuccess(RESOURCE_DUMMY_ATTRIBUTES_TO_GET, initTask, initResult);
    }

    /** Testing "attributes to get" with attributes not returned by default (MID-9774). */
    @Test
    public void test100ComputingAttributesToGet() throws Exception {
        var task = getTestTask();
        var result = task.getResult();

        var coords = new ResourceShadowCoordinates(RESOURCE_DUMMY_ATTRIBUTES_TO_GET.oid, ACCOUNT, INTENT_DEFAULT);
        var ctx = provisioningContextFactory.createForShadowCoordinates(coords, task, result);

        when("attributes to return are computed");
        var itemsToReturn = ctx.createItemsToReturn();

        then("attributes to return are OK, especially hiddenAttr2 is not requested");

        displayValue("attributesToReturn", itemsToReturn);

        assertThat(itemsToReturn.isReturnDefaultAttributes())
                .as("'return default attributes' flag")
                .isFalse();

        var attrNames = itemsToReturn.getItemsToReturn().stream()
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
        var itemsToReturn = ctx.createItemsToReturn();

        then("attributes to return are OK, especially hiddenAttr1 is requested");

        displayValue("attributesToReturn", itemsToReturn);

        assertThat(itemsToReturn.isReturnDefaultAttributes())
                .as("'return default attributes' flag")
                .isTrue();

        Collection<? extends ShadowAttributeDefinition<?, ?, ?, ?>> itemsDefinitions = itemsToReturn.getItemsToReturn();
        assertThat(itemsDefinitions).as("items definitions").isNotNull();
        var attrNames = itemsDefinitions.stream()
                .map(def -> def.getItemName().getLocalPart())
                .collect(Collectors.toSet());

        // "name" is not here, because it is returned by default, so it does not need to be mentioned in the options
        assertThat(attrNames)
                .as("names of attributes to return")
                .containsExactlyInAnyOrder(HIDDEN_ATTR_1);
    }
}

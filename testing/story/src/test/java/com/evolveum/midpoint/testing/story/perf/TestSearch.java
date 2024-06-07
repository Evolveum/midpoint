/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.testing.story.perf;

import static org.assertj.core.api.Assertions.assertThat;

import static com.evolveum.midpoint.schema.constants.SchemaConstants.RI_ACCOUNT_OBJECT_CLASS;

import java.io.File;
import java.util.*;

import com.evolveum.icf.dummy.resource.DummyObjectClass;

import com.evolveum.midpoint.tools.testng.UnusedTestElement;

import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.icf.dummy.resource.DummyAccount;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.internals.InternalsConfig;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.Resource;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyAuditService;
import com.evolveum.midpoint.test.DummyTestResource;
import com.evolveum.midpoint.test.util.MidPointTestConstants;
import com.evolveum.midpoint.testing.story.AbstractStoryTest;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * Tests provisioning-level search performance.
 *
 * Not part of the standard test suite. It is executed manually from time to time.
 */
@UnusedTestElement("executed manually")
@ContextConfiguration(locations = { "classpath:ctx-story-test-main.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestSearch extends AbstractStoryTest {

    public static final File TEST_DIR = new File(MidPointTestConstants.TEST_RESOURCES_DIR, "perf/search");

    public static final File SYSTEM_CONFIGURATION_FILE = new File(TEST_DIR, "system-configuration.xml");

    private static final int NUMBER_OF_ATTRIBUTES = 200;
    private static final int NUMBER_OF_AUX_CLASSES = 3;
    private static final String MAIN_CLASS_SYMBOL = "main";
    private static final String ATTR_NAME_TEMPLATE = "attr_%s_%s";
    private static final List<String> AUX_CLASSES_NAMES = new ArrayList<>();
    private static final Map<String, Set<String>> ATTRIBUTE_NAMES_PER_CLASS = new HashMap<>();

    private static final int NUMBER_OF_ACCOUNTS = 1_000;

    private static final DummyTestResource RESOURCE_DUMMY = new DummyTestResource(
            TEST_DIR, "resource-dummy.xml", "6a604402-d1cd-42a3-9b99-761c2c73e4c2", "search-perf",
            c -> {
                generateAttributeNames();
                for (Map.Entry<String, Set<String>> e : ATTRIBUTE_NAMES_PER_CLASS.entrySet()) {
                    var ocName = e.getKey();
                    var attributeNames = e.getValue();
                    if (ocName.equals(MAIN_CLASS_SYMBOL)) {
                        for (String attributeName : attributeNames) {
                            c.addAttrDef(c.getAccountObjectClass(), attributeName, String.class, false, false);
                        }
                    } else {
                        DummyObjectClass oc = DummyObjectClass.standard();
                        for (String attributeName : attributeNames) {
                            oc.addAttributeDefinition(attributeName, String.class, false, false);
                        }
                        c.getDummyResource().addAuxiliaryObjectClass(ocName, oc);
                    }
                }
            });

    private static void generateAttributeNames() {
        for (int classIdx = 0; classIdx <= NUMBER_OF_AUX_CLASSES; classIdx++) {
            String className;
            if (classIdx == 0) {
                className = "main";
            } else {
                className = "aux" + classIdx;
                AUX_CLASSES_NAMES.add(className);
            }
            Set<String> attrNames = new HashSet<>();
            for (int attrIdx = 0; attrIdx < NUMBER_OF_ATTRIBUTES; attrIdx++) {
                String attrName = ATTR_NAME_TEMPLATE.formatted(
                        className, RandomStringUtils.random(15, true, true));
                if (!attrNames.add(attrName)) {
                    // already exists, try again
                    attrIdx--;
                }
            }
            ATTRIBUTE_NAMES_PER_CLASS.put(className, attrNames);
        }
    }

    @Override
    protected boolean isAvoidLoggingChange() {
        return false; // we want logging from our system config
    }

    @Override
    protected File getSystemConfigurationFile() {
        return SYSTEM_CONFIGURATION_FILE;
    }

    @Override
    protected void importSystemTasks(OperationResult initResult) {
        // nothing here
    }

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        DummyAuditService.getInstance().setEnabled(false);
        InternalsConfig.turnOffAllChecks();

        RESOURCE_DUMMY.initAndTest(this, initTask, initResult);
        generateAccounts();
    }

    private void generateAccounts() throws Exception {
        for (int i = 0; i < NUMBER_OF_ACCOUNTS; i++) {
            String username = "user%08d".formatted(i);
            DummyAccount account = RESOURCE_DUMMY.controller.addAccount(username);
            account.addAuxiliaryObjectClassNames(AUX_CLASSES_NAMES);
            for (Set<String> attrNames : ATTRIBUTE_NAMES_PER_CLASS.values()) {
                for (String attributeName : attrNames) {
                    account.addAttributeValue(attributeName, RandomStringUtils.random(20, true, true));
                }
            }
        }
    }

    @Test
    public void test100ExecuteSearch() throws Exception {
        var task = getTestTask();
        var result = task.getResult();

        when("accounts are retrieved for the first time");
        executeSearchAccounts(task, result);

        when("accounts are retrieved for the second time (shadows are already there)");
        executeSearchAccounts(task, result);

        when("accounts are retrieved for the third time (to measure the performance stability)");
        executeSearchAccounts(task, result);
    }

    private void executeSearchAccounts(Task task, OperationResult result) throws Exception {
        var resource = Resource.of(RESOURCE_DUMMY.get());
        long start = System.currentTimeMillis();
        List<PrismObject<ShadowType>> accounts = provisioningService.searchObjects(
                ShadowType.class,
                resource.queryFor(RI_ACCOUNT_OBJECT_CLASS)
                        .build(),
                null, task, result);
        long duration = System.currentTimeMillis() - start;

        then("accounts are there");
        assertThat(accounts).as("accounts").hasSize(NUMBER_OF_ACCOUNTS);

        System.out.printf("Retrieved %,d accounts in %,d ms (avg: %,.3f ms)%n",
                accounts.size(), duration, (float) duration / accounts.size());

        and("they have all the attributes");
        for (PrismObject<ShadowType> account : accounts) {
            assertThat(account.asObjectable().getAttributes().getAny())
                    .as("attributes for " + account)
                    .hasSize(2 + (NUMBER_OF_AUX_CLASSES + 1) * NUMBER_OF_ATTRIBUTES);
        }
    }
}

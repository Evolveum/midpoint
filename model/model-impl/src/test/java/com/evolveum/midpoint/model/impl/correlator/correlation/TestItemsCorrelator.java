/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.correlator.correlation;

import com.evolveum.midpoint.model.impl.correlator.CorrelatorTestUtil;
import com.evolveum.midpoint.test.DummyTestResource;
import com.evolveum.midpoint.test.TestResource;
import com.evolveum.midpoint.test.util.MidPointTestConstants;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectTemplateType;

import org.testng.annotations.BeforeClass;

import java.io.File;

/**
 * Tests the `items` correlators, including fuzzy matching.
 *
 * See {@link TestTraditionalCorrelators} for more documentation.
 *
 * ONLY ON NATIVE REPOSITORY.
 */
public class TestItemsCorrelator extends AbstractCorrelatorsTest {

    protected static final File TEST_DIR =
            new File(MidPointTestConstants.TEST_RESOURCES_DIR, "correlator/correlation/items");

    private static final DummyTestResource RESOURCE_DUMMY_CORRELATION = new DummyTestResource(
            TEST_DIR, "resource-dummy-correlation-items.xml",
            "a849873e-e8e1-44e7-8836-13cb9146aa38", "correlation-items", CorrelatorTestUtil::createAttributeDefinitions);

    private static final TestResource<ObjectTemplateType> OBJECT_TEMPLATE_USER = new TestResource<>(
            TEST_DIR, "object-template-user.xml", "204f3615-bcd7-430d-93ec-c36f1db1dccd");

    private static final File FILE_ACCOUNTS = new File(TEST_DIR, "accounts.csv");
    private static final File FILE_USERS = new File(TEST_DIR, "users.xml");

    private static final File[] CORRELATOR_FILES = {
            new File(TEST_DIR, "correlator-by-name.xml")
    };

    @BeforeClass
    public void checkNative() {
        skipIfNotNativeRepository();
    }

    @Override
    protected DummyTestResource getResource() {
        return RESOURCE_DUMMY_CORRELATION;
    }

    @Override
    protected TestResource<ObjectTemplateType> getUserTemplateResource() {
        return OBJECT_TEMPLATE_USER;
    }

    @Override
    protected File getAccountsFile() {
        return FILE_ACCOUNTS;
    }

    @Override
    protected File[] getCorrelatorFiles() {
        return CORRELATOR_FILES;
    }

    @Override
    protected File getUsersFile() {
        return FILE_USERS;
    }

    @Override
    void initDummyIdMatchService() {
        // No-op here
    }
}

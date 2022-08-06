/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.correlator.correlation;

import com.evolveum.midpoint.model.impl.correlator.CorrelatorTestUtil;
import com.evolveum.midpoint.model.impl.correlator.idmatch.IdMatchCorrelatorFactory;
import com.evolveum.midpoint.model.test.idmatch.DummyIdMatchServiceImpl;
import com.evolveum.midpoint.test.DummyTestResource;
import com.evolveum.midpoint.test.TestResource;
import com.evolveum.midpoint.test.util.MidPointTestConstants;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectTemplateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;

/**
 * Tests "traditional" correlators:
 *
 * - filter correlator
 * - expression correlator
 * - ID Match correlator
 *
 * The expression correlator is tested also separately in {@link TestExpressionCorrelator}.
 */
public class TestTraditionalCorrelators extends AbstractCorrelatorsTest {

    protected static final File TEST_DIR =
            new File(MidPointTestConstants.TEST_RESOURCES_DIR, "correlator/correlation/traditional");

    private static final DummyTestResource RESOURCE_DUMMY_CORRELATION = new DummyTestResource(
            TEST_DIR, "resource-dummy-correlation.xml",
            "4a7f6b3e-64cc-4cd9-b5ba-64ecc47d7d10", "correlation", CorrelatorTestUtil::createAttributeDefinitions);

    /**
     * Contains data for executing the tests. Please see comments in the file itself.
     */
    private static final File FILE_ACCOUNTS = new File(TEST_DIR, "accounts.csv");

    /**
     * Users against which we correlate the accounts.
     */
    private static final File FILE_USERS = new File(TEST_DIR, "users.xml");

    private static final File[] CORRELATOR_FILES = {
            new File(TEST_DIR, "correlator-emp.xml"),
            new File(TEST_DIR, "correlator-emp-fn.xml"),
            new File(TEST_DIR, "correlator-emp-fn-opt.xml"),
            new File(TEST_DIR, "correlator-owner.xml"),
            new File(TEST_DIR, "correlator-owner-ref.xml"),
            new File(TEST_DIR, "correlator-id-match.xml")
    };

    @Autowired private IdMatchCorrelatorFactory idMatchCorrelatorFactory;

    /** Used by the `id-match` correlator instead of real ID Match Service. */
    private final DummyIdMatchServiceImpl dummyIdMatchService = new DummyIdMatchServiceImpl();

    @Override
    protected DummyTestResource getResource() {
        return RESOURCE_DUMMY_CORRELATION;
    }

    @Override
    protected TestResource<ObjectTemplateType> getUserTemplateResource() {
        return null;
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

    /**
     * We need specific records in our ID Match service.
     */
    @Override
    void initDummyIdMatchService() throws SchemaException {
        ShadowType ian200 = CorrelatorTestUtil.findAccount(allAccounts, 200).getShadow();
        dummyIdMatchService.addRecord("200", ian200.getAttributes(), "9481", null);
        idMatchCorrelatorFactory.setServiceOverride(dummyIdMatchService);
    }
}

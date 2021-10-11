/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.intest.manual;

import java.io.File;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Listeners;

import com.evolveum.midpoint.provisioning.ucf.impl.builtin.ManualConnectorInstance;
import com.evolveum.midpoint.repo.cache.RepositoryCache;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;

/**
 * Tests a slow semi manual resource with the use of proposed shadows.
 * The resource is "slow" in a way that it takes approx. a second to process a ticket.
 * This may cause all sorts of race conditions.
 * <p>
 * THIS TEST IS DISABLED MID-4166
 *
 * @author Radovan Semancik
 */
@ContextConfiguration(locations = { "classpath:ctx-model-intest-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
@Listeners({ com.evolveum.midpoint.tools.testng.AlphabeticalMethodInterceptor.class })
public class TestSemiManualSlowProposed extends TestSemiManual {

    @Autowired
    @Qualifier("cacheRepositoryService")
    private RepositoryCache repositoryCache;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);
        initManualConnector();

        repositoryCache.setModifyRandomDelayRange(150);
    }

    @Override
    protected String getResourceOid() {
        return RESOURCE_SEMI_MANUAL_SLOW_PROPOSED_OID;
    }

    @Override
    protected File getResourceFile() {
        return RESOURCE_SEMI_MANUAL_SLOW_PROPOSED_FILE;
    }

    @Override
    protected String getRoleOneOid() {
        return ROLE_ONE_SEMI_MANUAL_SLOW_PROPOSED_OID;
    }

    @Override
    protected File getRoleOneFile() {
        return ROLE_ONE_SEMI_MANUAL_SLOW_PROPOSED_FILE;
    }

    @Override
    protected String getRoleTwoOid() {
        return ROLE_TWO_SEMI_MANUAL_SLOW_PROPOSED_OID;
    }

    @Override
    protected File getRoleTwoFile() {
        return ROLE_TWO_SEMI_MANUAL_SLOW_PROPOSED_FILE;
    }

    // Make the test fast ...
    @Override
    protected int getConcurrentTestRandomStartDelayRangeAssign() {
        return 300;
    }

    @Override
    protected int getConcurrentTestRandomStartDelayRangeUnassign() {
        return 3;
    }

    // ... and intense ...
    @Override
    protected int getConcurrentTestNumberOfThreads() {
        return 10;
    }

    // .. and make the resource slow.
    protected void initManualConnector() {
        ManualConnectorInstance.setRandomDelayRange(1000);
    }

    @Override
    protected boolean are9xxTestsEnabled() {
        return true;
    }
}

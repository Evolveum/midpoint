/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.impl;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.provisioning.api.*;
import com.evolveum.midpoint.provisioning.impl.dummy.AbstractBasicDummyTest;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

import java.util.List;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.Holder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorType;

/**
 * The test of Provisioning service on the API level. The test is using dummy
 * resource for speed and flexibility.
 *
 * @author Radovan Semancik
 */
@ContextConfiguration(locations = "classpath:ctx-provisioning-test-main.xml")
@DirtiesContext
@Listeners({ com.evolveum.midpoint.tools.testng.AlphabeticalMethodInterceptor.class })
public class TestDummyFoundAgain extends AbstractBasicDummyTest {

    private static final String BLACKBEARD_USERNAME = "BlackBeard";
    private static final String DRAKE_USERNAME = "Drake";
    // Make this ugly by design. it check for some caseExact/caseIgnore cases
    private static final String ACCOUNT_MURRAY_USERNAME = "muRRay";

    static final long VALID_FROM_MILLIS = 12322342345435L;
    static final long VALID_TO_MILLIS = 3454564324423L;


    protected String getMurrayRepoIcfName() {
        return ACCOUNT_MURRAY_USERNAME;
    }

    protected String getBlackbeardRepoIcfName() {
        return BLACKBEARD_USERNAME;
    }

    protected String getDrakeRepoIcfName() {
        return DRAKE_USERNAME;
    }

    protected ItemComparisonResult getExpectedPasswordComparisonResultMatch() {
        return ItemComparisonResult.NOT_APPLICABLE;
    }

    protected ItemComparisonResult getExpectedPasswordComparisonResultMismatch() {
        return ItemComparisonResult.NOT_APPLICABLE;
    }

    @Test
    public void test101AddAccountWithoutName() throws Exception {
        // GIVEN
        Task syncTask = getTestTask();
        OperationResult result = syncTask.getResult();
        syncServiceMock.reset();

        List<PrismObject<ConnectorType>> connectors = repositoryService.searchObjects(ConnectorType.class,
                null, null, result);

        assertNotNull(connectors);
        // lets take first connector
        PrismObject<ConnectorType> toBeFoundAgain = connectors.get(0);
        connectorManager.invalidate(ConnectorType.class, toBeFoundAgain.getOid(), null);
        repositoryService.deleteObject(ConnectorType.class, toBeFoundAgain.getOid(), result);

        Holder<ConnectorType> discovered = new Holder<>();
        // Consumer is called after initial inRepo check, we add connector again
        // so add should fail.
        connectorManager.setNotFoundInRepoConsumer(c -> {
            discovered.setValue(c);
            try {
                repositoryService.addObject(toBeFoundAgain, null, result);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        provisioningService.discoverConnectors(null, result);
        assertNotNull(discovered.getValue());
        assertEquals(discovered.getValue().getConnectorType(), toBeFoundAgain.asObjectable().getConnectorType());
        List<PrismObject<ConnectorType>> connectorsAfter = repositoryService.searchObjects(ConnectorType.class,
                null, null, result);
        assertEquals(connectorsAfter.size(),connectors.size());

    }
}

/*
 * Copyright (c) 2015-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.testing.conntest.ad;

import java.io.File;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;

/**
 * Test for multi-domain AD (chimera-hydra) with some strange configuration.
 *
 * @author semancik
 */
@ContextConfiguration(locations = { "classpath:ctx-conntest-test-main.xml" })
@Listeners({ com.evolveum.midpoint.tools.testng.AlphabeticalMethodInterceptor.class })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestAdLdapChimeraStrange extends TestAdLdapChimera {

    @Override
    protected File getResourceFile() {
        return new File(getBaseDir(), "resource-chimera-strange.xml");
    }

    /**
     * This resource has "tree delete" disabled.
     * MID-5935
     */
    @Test
    public void test599DeleteOrgMeleeIsland() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        createUnderMeleeEntry();

        try {
            // WHEN
            when();
            deleteObject(OrgType.class, orgMeleeIslandOid, task, result);

            assertNotReached();
        } catch (IllegalArgumentException e) {
            // expected
        }

        // THEN
        then();
        assertFailure(result);

//        assertLdapConnectorInstances(2);
    }

}

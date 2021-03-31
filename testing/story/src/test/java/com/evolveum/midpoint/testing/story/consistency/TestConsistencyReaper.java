/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.testing.story.consistency;

import java.io.File;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.asserter.LinksAsserter;
import com.evolveum.midpoint.test.asserter.ShadowReferenceAsserter;
import com.evolveum.midpoint.util.exception.CommonException;

/**
 * Almost same sa TestConsistencyMechanism, but this reaps dead shadows immediately (deadShadowRetentionPeriod=0).
 *
 * @author Radovan Semancik
 */
@ContextConfiguration(locations = { "classpath:ctx-story-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestConsistencyReaper extends TestConsistencyMechanism {

    private static final File RESOURCE_OPENDJ_FILE = new File(TEST_DIR, "resource-opendj-reaper.xml");

    protected File getResourceFile() {
        return RESOURCE_OPENDJ_FILE;
    }

    @Override
    protected void checkTest130DeadShadow(Task task, OperationResult parentResult) {
        // nothing to do. Dead shadows are reaped immediately.
    }

    @Override
    protected void checkTest140DeadShadow(Task task, OperationResult parentResult) {
        // nothing to do. Dead shadows are reaped immediately.
    }

    @Override
    protected void assert800DeadShadows() throws CommonException {
        assertNoRepoShadow(ACCOUNT_DENIELS_OID);

        LinksAsserter<?, ?, ?> linksAsserter = assertUser(USER_ELAINE_OID, "User after recon")
                .assertLiveLinks(1)
                .links();

        ShadowReferenceAsserter<?> notDeadShadow = linksAsserter.by()
                .dead(false)
                .find();

        assertModelShadow(notDeadShadow.getOid())
                .display()
                .attributes()
                    .assertHasPrimaryIdentifier()
                    .assertHasSecondaryIdentifier()
                    .end()
                .end();
    }

    @Override
    protected boolean isReaper() {
        return true;
    }
}

/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.impl.dummy;

import static org.testng.AssertJUnit.*;

import java.io.File;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.evolveum.icf.dummy.resource.DummyAccount;

/**
 * Variation of TestDummyConsistency, but there is zero retention interval for dead shadows.
 * Everything should be reaped immediately.
 *
 * MID-6435
 *
 * @author Radovan Semancik
 */
@ContextConfiguration(locations = "classpath:ctx-provisioning-test-main.xml")
@DirtiesContext
@Listeners({ com.evolveum.midpoint.tools.testng.AlphabeticalMethodInterceptor.class })
public class TestDummyConsistencyReaper extends TestDummyConsistency {

    public static final File RESOURCE_DUMMY_FILE = new File(TEST_DIR, "resource-dummy-retry-reaper.xml");

    @Override
    protected File getResourceDummyFile() {
        return RESOURCE_DUMMY_FILE;
    }

    // There won't really be a dead shadow for Morgan. We are reaping everything immediately.
    // The shadow should be gone.
    @Override
    protected void assertMorganDead() throws Exception {
        assertNoRepoShadow(ACCOUNT_MORGAN_OID);

        dummyResource.resetBreakMode();

        DummyAccount dummyAccount = dummyResource.getAccountByUsername(transformNameFromResource(ACCOUNT_WILL_USERNAME));
        assertNotNull("No dummy account", dummyAccount);
        assertEquals("Username is wrong", transformNameFromResource(ACCOUNT_WILL_USERNAME), dummyAccount.getName());
        assertEquals("Fullname is wrong", "Will Turner", dummyAccount.getAttributeValue("fullname"));
        assertTrue("The account is not enabled", dummyAccount.isEnabled());
        assertEquals("Wrong password", ACCOUNT_WILL_PASSWORD, dummyAccount.getPassword());

        assertSteadyResources();
    }

    @Test
    @Override
    public void test109RefreshAccountMorganDead() {
        // Nothing to do.
        // We cannot refresh shadow. It is deleted already.
    }

    @Override
    protected void assertDeadShadowNotify() {
        // There may be more than one notification.
        // E.g. there may be notify that the shadow is dead, and then there may be notify that it was deleted.
        // We do not really care, as long as they are all successful.
        syncServiceMock.assertNotifySuccessOnly();
    }

    // When it is gone, it is gone. No tombstone, nothing.
    @Override
    protected void assertDeletedMorgan(int expectedAttemptNumber, int expectedNumberOfPendingOperations) throws Exception {

        assertNoRepoShadow(shadowMorganOid);

        dummyResource.resetBreakMode();
        dummyResourceCtl.assertNoAccountByUsername(ACCOUNT_MORGAN_NAME);
    }

    // The shadows is already gone, there is nothing to test.
    // There were some pending operation that were not expired.
    // But the account is deleted, shadow is dead and dead shadow reaping is merciless.
    @Test
    @Override
    public void test190AccountMorganDeadExpireOperation() {
        // nothing to do
    }

    // Same as above, the shadow is gone.
    @Test
    @Override
    public void test192AccountMorganSecondDeadExpireOperation() {
        // nothing to do
    }

    // Same as above, the shadow is gone.
    @Test
    @Override
    public void test194AccountMorganDeadExpireShadow() {
        // nothing to do
    }

    // The shadow is gone already.
    @Test
    public void test196AccountMorganSecondDeadExpireShadow() {
        // nothing to do
    }
}

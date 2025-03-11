/*
 * Copyright (c) 2016-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.testing.conntest.opendj;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SynchronizationSituationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNull;

/**
 * Almost same sa TestOpenDj, but there is unsafeNameHint setting and maybe
 * some other possibly risky and alternative connector settings.
 * Also, there is some pollution in that LDAP data, that is filtered out by additionalSearchFilter.
 *
 * @author semancik
 */
public abstract class AbstractOpenDjNoiseTest extends TestOpenDj {

    private static final String ACCOUNT_NOISE_RAMDOM_UID = "rnoise";
    private static final String ACCOUNT_NOISE_CHAOTIC_UID = "cnoise";

    private static final String SHADOW_BILBO_OID = "500ddd61-f4a1-4419-9de8-94db65f30f7d";

    protected File getShadowBilboFile() {
        return new File(getBaseDir(),  "shadow-bilbo.xml");
    }

    @Override
    protected void addAdditionalLdapEntries() throws Exception {
        addNoiseEntries();
    }

    // Adds a set of "noise" LDAP objects. We do not want to process these.
    // They are filtered out by additionalSearchFilter
    protected void addNoiseEntries() throws LdapException, IOException {
        Entry entry = createAccountEntry(ACCOUNT_NOISE_RAMDOM_UID, "Random Noise", "Random", "Noise");
        markInvisible(entry);
        addLdapEntry(entry);

        // TDOD
    }

    /**
     * Test for additional search filter.
     * MID-4925
     */
    @Test
    @Override
    public void test350SearchInvisibleAccount() throws Exception {
        // GIVEN
        createBilboEntry();

        SearchResultList<PrismObject<ShadowType>> shadows = searchBilbo();

        assertEquals("Unexpected search result: " + shadows, 0, shadows.size());
    }

    /**
     * Create fabricated shadow for Bilbo.
     * Try to get the account using the shadow.
     * Make sure that additional search filter is applies and "not found" error is returned.
     * MID-6438
     */
    @Test
    public void test352GetInvisibleAccount() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        repoAddShadowFromFile(getShadowBilboFile(), result);

        // WHEN
        when();
        PrismObject<ShadowType> shadow = modelService.getObject(ShadowType.class, SHADOW_BILBO_OID, null, task, result);

        // THEN
        then();
        assertSuccess(result);
        display("Bilbo", shadow);

        assertShadow(shadow, "Bilbo after")
                .assertDead()
                .assertIsNotExists();

        // We can no longer assert on the synchronization situation (being DELETED) in the returned shadow here. It was here
        // as a side-effect of the "discovery" operation on the dead shadow. Recent code changes related to performance
        // optimizations cause that the discovery operation is carried out on a copy of the shadow, hence none of its effects
        // propagate to the shadow being returned.
        //
        // The synchronization situation is still stored in the repository, so we can assert it there. (See below.)
        // The shadow returned here is marked as dead, anyway, and this is the key information for the client.

        assertRepoShadow(shadow.getOid(), "Bilbo after (repo)")
                .assertSynchronizationSituation(SynchronizationSituationType.DELETED);

        assertLdapConnectorReasonableInstances();
    }

    /**
     * This addition should be filtered out by additionalSearchFilter.
     */
    @Test
    public void test840SyncAddAccountChaos() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        Entry entry = createAccountEntry(ACCOUNT_NOISE_CHAOTIC_UID, "Chaotic Noise", "Chaotic", "Noise");
        markInvisible(entry);

        long tsStart = System.currentTimeMillis();

        // WHEN
        when();
        addLdapEntry(entry);

        syncWait();

        getSyncTask().rerun(result);

        // THEN
        then();
        assertSuccess(result);

        long tsEnd = System.currentTimeMillis();

        displayUsers();

        PrismObject<UserType> user = findUserByUsername(ACCOUNT_NOISE_CHAOTIC_UID);
        assertNull("Unexpected user " + user, user);

        assertStepSyncToken(getSyncTask().oid, 1, tsStart, tsEnd);
    }
}

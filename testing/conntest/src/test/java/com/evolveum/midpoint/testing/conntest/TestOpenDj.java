/*
 * Copyright (c) 2014-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.testing.conntest;

import java.io.File;

import org.testng.annotations.AfterClass;
import org.testng.annotations.Listeners;

import com.evolveum.midpoint.test.util.MidPointTestConstants;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;

/**
 * @author semancik
 *
 */
@Listeners({ com.evolveum.midpoint.tools.testng.AlphabeticalMethodInterceptor.class })
public class TestOpenDj extends AbstractLdapConnTest {

    private static final String OPENDJ_TEMPLATE_NAME = "opendj-4000.template";

    private static final int INITIAL_SYNC_TOKEN = 23;

    @Override
    protected String getResourceOid() {
        return "371ffc38-c424-11e4-8467-001e8c717e5b";
    }

    @Override
    protected File getBaseDir() {
        return new File(MidPointTestConstants.TEST_RESOURCES_DIR, "opendj");
    }

    @Override
    public String getStartSystemCommand() {
        return null;
    }

    @Override
    public String getStopSystemCommand() {
        return null;
    }

    @Override
    protected void startResources() throws Exception {
        super.startResources();
        openDJController.startCleanServer(OPENDJ_TEMPLATE_NAME);
    }

    @AfterClass
    public static void stopResources() throws Exception {
        AbstractLdapConnTest.stopResources();
        openDJController.stop();
    }

    @Override
    protected String getLdapServerHost() {
        return "localhost";
    }

    @Override
    protected int getLdapServerPort() {
        return 10389;
    }

    @Override
    protected String getLdapBindDn() {
        return "cn=directory manager";
    }

    @Override
    protected String getLdapBindPassword() {
        return "secret";
    }

    @Override
    protected String getPeopleLdapSuffix() {
        return "ou=people,"+getLdapSuffix();
    }

    @Override
    protected String getAccount0Cn() {
        return "Warlaz Kunjegjul (00000000)";
    }

    @Override
    protected boolean isIdmAdminInteOrgPerson() {
        return true;
    }

    @Override
    protected int getSearchSizeLimit() {
        return 1000;
    }

    @Override
    protected String getLdapGroupObjectClass() {
        return "groupOfUniqueNames";
    }

    @Override
    protected String getLdapGroupMemberAttribute() {
        return "uniqueMember";
    }

    @Override
    protected boolean needsGroupFakeMemeberEntry() {
        return true;
    }

    @Override
    protected String getSyncTaskOid() {
        return "cd1e0ff2-0099-11e5-9e22-001e8c717e5b";
    }

    protected int getInitialSyncToken() {
        return INITIAL_SYNC_TOKEN;
    }

    @Override
    protected boolean isAssertOpenFiles() {
        // Cannot do this here. OpenDJ is embedded, the
        // number of open files for the whole process may
        // vary significantly because of OpenDJ.
        return false;
    }

    @Override
    protected void assertStepSyncToken(String syncTaskOid, int step, long tsStart, long tsEnd)
            throws ObjectNotFoundException, SchemaException {
        assertSyncToken(syncTaskOid, (Integer)(step + getInitialSyncToken()));
    }

}

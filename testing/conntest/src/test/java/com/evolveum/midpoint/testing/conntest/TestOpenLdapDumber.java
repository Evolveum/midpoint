/*
 * Copyright (c) 2017-2018 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.testing.conntest;

import static org.testng.AssertJUnit.assertEquals;

import java.io.File;

import com.evolveum.midpoint.test.util.MidPointTestConstants;

/**
 * OpenLDAP, but without permissive modify, shortcut attributes, etc.
 *
 * @author semancik
 */
public class TestOpenLdapDumber extends TestOpenLdap {

    @Override
    protected File getBaseDir() {
        return new File(MidPointTestConstants.TEST_RESOURCES_DIR, "openldap-dumber");
    }

    @Override
    protected boolean hasAssociationShortcut() {
        return false;
    }

    @Override
    protected boolean isUsingGroupShortcutAttribute() {
        return false;
    }

    // This is a dumb resource. It cannot count.
    @Override
    protected void assertCountAllAccounts(Integer count) {
        assertEquals("Wrong account count", (Integer) null, count);
    }

}

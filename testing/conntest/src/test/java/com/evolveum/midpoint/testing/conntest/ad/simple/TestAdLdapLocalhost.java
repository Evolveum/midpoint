/*
 * Copyright (c) 2015-2016 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.testing.conntest.ad.simple;

import com.evolveum.midpoint.testing.conntest.ad.simple.AbstractAdLdapCookedTest;

import java.io.File;

/**
 * @author semancik
 */
public class TestAdLdapLocalhost extends AbstractAdLdapCookedTest {

    @Override
    protected String getResourceOid() {
        return "eced6d24-73e3-11e5-8457-93eff15a6b85";
    }

    @Override
    protected File getResourceFile() {
        return new File(getBaseDir(), "resource-localhost.xml");
    }

    @Override
    protected String getLdapServerHost() {
        return "localhost";
    }

    @Override
    protected int getLdapServerPort() {
        return 9636;
    }

}

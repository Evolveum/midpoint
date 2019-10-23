/**
 * Copyright (c) 2015-2016 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.testing.conntest.ad;

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
public class TestAdLdapRawMedusa extends AbstractAdLdapRawTest {

    @Override
    protected String getResourceOid() {
        return "eced6d24-73e3-11e5-8457-93eff15a6b85";
    }

    @Override
    protected File getResourceFile() {
        return new File(getBaseDir(), "resource-raw-medusa.xml");
    }

    @Override
    protected String getLdapServerHost() {
        return "medusa.lab.evolveum.com";
    }

    @Override
    protected int getLdapServerPort() {
        return 636;
    }

}

/*
 * Copyright (c) 2015 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
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
public class TestEDirAthena extends AbstractEDirTest {

    @Override
    protected String getResourceOid() {
        return "0893372c-3c42-11e5-9179-001e8c717e5b";
    }

    @Override
    protected File getResourceFile() {
        return new File(getBaseDir(), "resource-athena.xml");
    }

    @Override
    protected String getLdapServerHost() {
        return "athena.evolveum.com";
    }

    @Override
    protected int getLdapServerPort() {
        return 33636;
    }

}

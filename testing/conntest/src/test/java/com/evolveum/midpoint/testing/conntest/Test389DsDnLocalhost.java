/*
 * Copyright (C) 2014-2020 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.testing.conntest;

import java.io.File;

import org.testng.annotations.Listeners;

import com.evolveum.midpoint.tools.testng.UnusedTestElement;

/**
 * @author semancik
 */
@Listeners({ com.evolveum.midpoint.tools.testng.AlphabeticalMethodInterceptor.class })
@UnusedTestElement("does not initialize")
public class Test389DsDnLocalhost extends Abstract389DsDnTest {

    @Override
    protected File getResourceFile() {
        return new File(getBaseDir(), "resource-dn-localhost.xml");
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
    protected String getLdapServerHost() {
        return "localhost";
    }

}

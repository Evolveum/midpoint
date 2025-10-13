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
public class Test389DsDnPhobos extends Abstract389DsDnTest {

    @Override
    protected File getResourceFile() {
        return new File(getBaseDir(), "resource-dn-phobos.xml");
    }

    @Override
    public String getStartSystemCommand() {
        return getScriptDirectoryName() + "/389ds-phobos-start";
    }

    @Override
    public String getStopSystemCommand() {
        return getScriptDirectoryName() + "/389ds-phobos-stop";
    }

    @Override
    protected String getLdapServerHost() {
        return "phobos.lab.evolveum.com";
    }
}

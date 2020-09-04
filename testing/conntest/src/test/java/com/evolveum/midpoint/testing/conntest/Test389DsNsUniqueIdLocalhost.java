/*
 * Copyright (C) 2014-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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
public class Test389DsNsUniqueIdLocalhost extends Abstract389DsNsUniqueIdTest {

    @Override
    protected File getResourceFile() {
        return new File(getBaseDir(), "resource-nsuniqueid-localhost.xml");
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

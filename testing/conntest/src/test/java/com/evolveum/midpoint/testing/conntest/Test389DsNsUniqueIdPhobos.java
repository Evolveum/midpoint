/*
 * Copyright (C) 2014-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.testing.conntest;

import java.io.File;

import com.evolveum.midpoint.tools.testng.UnusedTestElement;

/**
 * @author semancik
 */
@UnusedTestElement("does not initialize")
public class Test389DsNsUniqueIdPhobos extends Abstract389DsNsUniqueIdTest {

    @Override
    protected File getResourceFile() {
        return new File(getBaseDir(), "resource-nsuniqueid-phobos.xml");
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

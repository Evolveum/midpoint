/*
 * Copyright (c) 2014-2016 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.testing.conntest;

import java.io.File;

import org.testng.annotations.AfterClass;

import com.evolveum.midpoint.test.util.MidPointTestConstants;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;

/**
 * @author semancik
 *
 */
public class Test389DsNsUniqueIdPhobos extends Abstract389DsNsUniqueIdTest {


    @Override
    protected File getResourceFile() {
        return new File(getBaseDir(), "resource-nsuniqueid-phobos.xml");
    }

    @Override
    public String getStartSystemCommand() {
        return getScriptDirectoryName()+"/389ds-phobos-start";
    }

    @Override
    public String getStopSystemCommand() {
        return getScriptDirectoryName()+"/389ds-phobos-stop";
    }

    @Override
    protected String getLdapServerHost() {
        return "phobos.lab.evolveum.com";
    }

}

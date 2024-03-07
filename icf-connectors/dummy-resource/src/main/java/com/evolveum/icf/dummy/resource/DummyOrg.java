/*
 * Copyright (c) 2016 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.icf.dummy.resource;

import org.jetbrains.annotations.NotNull;

import java.io.FileNotFoundException;
import java.net.ConnectException;

/**
 * @author Radovan Semancik
 *
 */
public class DummyOrg extends DummyObject {

    public DummyOrg() {
        super();
    }

    public DummyOrg(String orgName) {
        super(orgName);
    }

    @Override
    public @NotNull String getObjectClassName() {
        return DummyResource.OBJECTCLASS_ORG_NAME;
    }

    @Override
    public String getShortTypeName() {
        return "org";
    }

}

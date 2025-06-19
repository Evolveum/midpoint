/*
 * Copyright (c) 2016 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.icf.dummy.resource;

import org.jetbrains.annotations.NotNull;

/**
 * @author Radovan Semancik
 *
 */
public class DummyOrg extends DummyObject {

    public static final String OBJECT_CLASS_NAME = "org";
    public static final String OBJECT_CLASS_DESCRIPTION = "Structural entity which represents a collection of accounts, groups"
            + "or other organizations";

    public DummyOrg() {
        super();
    }

    public DummyOrg(String orgName) {
        super(orgName);
    }

    public DummyOrg(String orgName, DummyResource resource) {
        super(orgName, resource);
    }

    @Override
    public @NotNull String getObjectClassName() {
        return OBJECT_CLASS_NAME;
    }

    public String getObjectClassDescription() {
        return OBJECT_CLASS_NAME;
    }

    @Override
    public String getShortTypeName() {
        return "org";
    }

}

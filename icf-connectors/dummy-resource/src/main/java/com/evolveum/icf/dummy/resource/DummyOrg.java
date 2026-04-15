/*
 * Copyright (c) 2016 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
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

    @Override
    public String getShortTypeName() {
        return "org";
    }

}

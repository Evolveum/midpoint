/*
 * Copyright (c) 2010-2014 Evolveum and contributors
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
public class DummyPrivilege extends DummyObject {

    public static final String OBJECT_CLASS_NAME = "privilege";

    public DummyPrivilege() {
        super();
    }

    public DummyPrivilege(String username) {
        super(username);
    }

    public DummyPrivilege(String username, DummyResource resource) {
        super(username, resource);
    }

    @Override
    public @NotNull String getObjectClassName() {
        return OBJECT_CLASS_NAME;
    }

    @Override
    public String getShortTypeName() {
        return "priv";
    }

    @Override
    public String toStringContent() {
        return super.toStringContent();
    }
}

/*
 * Copyright (c) 2010-2014 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.icf.dummy.resource;


/**
 * @author Radovan Semancik
 *
 */
public class DummyPrivilege extends DummyObject {

    public DummyPrivilege() {
        super();
    }

    public DummyPrivilege(String username) {
        super(username);
    }

    @Override
    protected DummyObjectClass getObjectClass() {
        return resource.getPrivilegeObjectClass();
    }

    @Override
    protected DummyObjectClass getObjectClassNoExceptions() {
        return resource.getPrivilegeObjectClass();
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

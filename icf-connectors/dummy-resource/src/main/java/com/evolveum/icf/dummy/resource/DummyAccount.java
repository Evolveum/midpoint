/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.icf.dummy.resource;

import java.io.FileNotFoundException;
import java.net.ConnectException;

import com.evolveum.midpoint.util.DebugUtil;

/**
 * @author Radovan Semancik
 *
 */
public class DummyAccount extends DummyObject {

    public static final String ATTR_FULLNAME_NAME = "fullname";
    public static final String ATTR_DESCRIPTION_NAME = "description";
    public static final String ATTR_INTERESTS_NAME = "interests";
    public static final String ATTR_PRIVILEGES_NAME = "privileges";
    public static final String ATTR_INTERNAL_ID = "internalId";

    private String password = null;
    private Boolean lockout = null;

    public DummyAccount() {
        super();
    }

    public DummyAccount(String username) {
        super(username);
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) throws ConnectException, FileNotFoundException, SchemaViolationException, ConflictException {
        checkModifyBreak();
        this.password = password;
    }

    public Boolean isLockout() {
        return lockout;
    }

    public void setLockout(boolean lockout) throws ConnectException, FileNotFoundException, SchemaViolationException, ConflictException {
        checkModifyBreak();
        this.lockout = lockout;
    }

    @Override
    protected DummyObjectClass getObjectClass() throws ConnectException, FileNotFoundException, SchemaViolationException, ConflictException, InterruptedException {
        return resource.getAccountObjectClass();
    }

    @Override
    protected DummyObjectClass getObjectClassNoExceptions() {
        return resource.getAccountObjectClassNoExceptions();
    }

    @Override
    public String getShortTypeName() {
        return "account";
    }

    @Override
    public String toStringContent() {
        return super.toStringContent() + ", password=" + password;
    }

    @Override
    protected void extendDebugDump(StringBuilder sb, int indent) {
        sb.append("\n");
        DebugUtil.debugDumpWithLabelToStringLn(sb, "Password", password, indent + 1);
        DebugUtil.debugDumpWithLabelToString(sb, "Lockout", lockout, indent + 1);
    }

}

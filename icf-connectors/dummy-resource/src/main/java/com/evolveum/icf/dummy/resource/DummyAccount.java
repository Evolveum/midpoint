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

import org.jetbrains.annotations.NotNull;

/**
 * @author Radovan Semancik
 *
 */
public class DummyAccount extends DummyObject {

    /** BEWARE! The dummy controller may represent this class as `__ACCOUNT__` (if using legacy schema mode). */
    public static final String OBJECT_CLASS_NAME = "account";
    public static final String OBJECT_CLASS_DESCRIPTION = "Digital identity object that represents a single human user or,"
            + " a non-human agent.";

    public static final String ATTR_FULLNAME_NAME = "fullname";
    public static final String ATTR_DESCRIPTION_NAME = "description";
    public static final String ATTR_INTERESTS_NAME = "interests";
    public static final String ATTR_PRIVILEGES_NAME = "privileges";
    public static final String ATTR_INTERNAL_ID = "internalId";

    private String password = null;

    /** "True" means the account is locked-out. */
    private Boolean lockoutStatus = null;

    public DummyAccount() {
    }

    public DummyAccount(String username) {
        super(username);
    }

    public DummyAccount(String username, DummyResource dummyResource) {
        super(username, dummyResource);
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) throws ConnectException, FileNotFoundException, SchemaViolationException, ConflictException {
        checkModifyBreak();
        this.password = password;
    }

    public Boolean getLockoutStatus() {
        return lockoutStatus;
    }

    public void setLockoutStatus(boolean lockoutStatus) throws ConnectException, FileNotFoundException, SchemaViolationException, ConflictException {
        checkModifyBreak();
        this.lockoutStatus = lockoutStatus;
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
        DebugUtil.debugDumpWithLabelToString(sb, "Lockout status", lockoutStatus, indent + 1);
    }

    @Override
    public @NotNull String getObjectClassName() {
        return OBJECT_CLASS_NAME;
    }

    @Override
    public String getObjectClassDescription() {
        return OBJECT_CLASS_DESCRIPTION;
    }
}

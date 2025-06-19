/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.icf.dummy.resource;

import java.io.FileNotFoundException;
import java.net.ConnectException;
import java.util.Collection;

import com.evolveum.midpoint.util.DebugUtil;

import org.jetbrains.annotations.NotNull;

/**
 * @author Radovan Semancik
 *
 */
public class DummyGroup extends DummyObject {

    /** BEWARE! The dummy controller may represent this class as `__GROUP__` (if using legacy schema mode). */
    public static final String OBJECT_CLASS_NAME = "group";
    public static final String OBJECT_CLASS_DESCRIPTION = "A logical collection of accounts or other groups,"
            + "that are managed together for access control, policy enforcement and administration.";

    public static final String ATTR_MEMBERS_NAME = "members";

    public DummyGroup() {
        super();
    }

    public DummyGroup(String username) {
        super(username);
    }

    public DummyGroup(String username, DummyResource resource) {
        super(username, resource);
    }

    public Collection<String> getMembers() {
        return getAttributeValues(ATTR_MEMBERS_NAME, String.class);
    }

    public void addMember(String newMember)
            throws SchemaViolationException, ConnectException, FileNotFoundException, ConflictException, InterruptedException {
        addAttributeValue(ATTR_MEMBERS_NAME, newMember);
    }

    public boolean containsMember(String member) {
        Collection<String> members = getMembers();
        if (members == null) {
            return false;
        }
        return members.contains(member); // TODO ok? what about case ignoring scenarios?
    }

    public void removeMember(String newMember)
            throws SchemaViolationException, ConnectException, FileNotFoundException, ConflictException, InterruptedException {
        removeAttributeValue(ATTR_MEMBERS_NAME, newMember);
    }

    @Override
    public @NotNull String getObjectClassName() {
        return OBJECT_CLASS_NAME;
    }

    @Override
    public String getObjectClassDescription() {
        return OBJECT_CLASS_DESCRIPTION;
    }

    @Override
    public String getShortTypeName() {
        return "group";
    }

    @Override
    public String toStringContent() {
        return super.toStringContent() + ", members=" + getMembers();
    }

    @Override
    protected void extendDebugDump(StringBuilder sb, int indent) {
        sb.append("\n");
        DebugUtil.debugDumpWithLabelToStringLn(sb, "Members", getMembers(), indent + 1);
    }
}

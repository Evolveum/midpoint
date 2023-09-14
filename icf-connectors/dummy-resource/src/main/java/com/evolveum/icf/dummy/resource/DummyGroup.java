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

/**
 * @author Radovan Semancik
 *
 */
public class DummyGroup extends DummyObject {

    public static final String ATTR_MEMBERS_NAME = "members";

    public DummyGroup() {
        super();
    }

    public DummyGroup(String username) {
        super(username);
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
    protected DummyObjectClass getObjectClass() {
        return resource.getGroupObjectClass();
    }

    @Override
    protected DummyObjectClass getObjectClassNoExceptions() {
        return resource.getGroupObjectClass();
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

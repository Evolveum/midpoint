/*
 * Copyright (c) 2010-2017 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
	
	public void addMember(String newMember) throws SchemaViolationException, ConnectException, FileNotFoundException, ConflictException {
		addAttributeValue(ATTR_MEMBERS_NAME, newMember);
	}

	public boolean containsMember(String member) {
		Collection<String> members = getMembers();
		if (members == null) {
			return false;
		}
		return members.contains(member);			// TODO ok? what about case ignoring scenarios?
	}

	public void removeMember(String newMember) throws SchemaViolationException, ConnectException, FileNotFoundException, ConflictException {
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
	public String debugDump() {
		return debugDump(0);
	}

	@Override
	protected void extendDebugDump(StringBuilder sb, int indent) {
		sb.append("\n");
		DebugUtil.debugDumpWithLabelToStringLn(sb, "Members", getMembers(), indent + 1);
	}
	
}

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
	protected DummyObjectClass getObjectClass() throws ConnectException, FileNotFoundException, SchemaViolationException, ConflictException {
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
	public String debugDump() {
		return debugDump(0);
	}

	@Override
	protected void extendDebugDump(StringBuilder sb, int indent) {
		sb.append("\n");
		DebugUtil.debugDumpWithLabelToStringLn(sb, "Password", password, indent + 1);
		DebugUtil.debugDumpWithLabelToString(sb, "Lockout", lockout, indent + 1);
	}
	
}

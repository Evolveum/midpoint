/*
 * Copyright (c) 2010-2014 Evolveum
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

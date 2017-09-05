/*
 * Copyright (c) 2016 Evolveum
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

/**
 * @author Radovan Semancik
 *
 */
public class DummyOrg extends DummyObject {

	public DummyOrg() {
		super();
	}

	public DummyOrg(String username) {
		super(username);
	}

	@Override
	protected DummyObjectClass getObjectClass() throws ConnectException, FileNotFoundException, SchemaViolationException {
		return resource.getPrivilegeObjectClass();
	}

	@Override
	protected DummyObjectClass getObjectClassNoExceptions() {
		return resource.getPrivilegeObjectClass();
	}

	@Override
	public String getShortTypeName() {
		return "org";
	}

}

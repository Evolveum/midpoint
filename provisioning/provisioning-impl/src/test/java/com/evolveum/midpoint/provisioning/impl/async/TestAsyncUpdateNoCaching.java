/*
 * Copyright (c) 2010-2019 Evolveum
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

package com.evolveum.midpoint.provisioning.impl.async;

import com.evolveum.icf.dummy.resource.*;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.test.IntegrationTestTools;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.ConnectException;
import java.util.Arrays;
import java.util.List;

/**
 *
 */
public class TestAsyncUpdateNoCaching extends TestAsyncUpdate {

	protected static DummyResource dummyResource;
	protected static DummyResourceContoller dummyResourceCtl;

	@Override
	protected File getResourceFile() {
		return RESOURCE_ASYNC_NO_CACHING_FILE;
	}

	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		super.initSystem(initTask, initResult);
		dummyResourceCtl = DummyResourceContoller.create("async");
		dummyResourceCtl.setResource(resource);
		dummyResource = dummyResourceCtl.getDummyResource();
		dummyResourceCtl.addAttrDef(dummyResource.getAccountObjectClass(), "test", String.class, false, true);
	}

	@NotNull
	@Override
	public List<String> getConnectorTypes() {
		return Arrays.asList(ASYNC_CONNECTOR_TYPE, IntegrationTestTools.DUMMY_CONNECTOR_TYPE);
	}

	@Override
	protected int getNumberOfAccountAttributes() {
		return 2;
	}

	@Override
	protected void addDummyAccount(String name) {
		try {
			dummyResourceCtl.addAccount(name);
		} catch (ObjectAlreadyExistsException | SchemaViolationException | ConnectException | FileNotFoundException | ConflictException | InterruptedException e) {
			throw new AssertionError(e);
		}
	}

	@Override
	protected void setDummyAccountTestAttribute(String name, String... values) {
		try {
			DummyAccount account = dummyResource.getAccountByUsername(name);
			account.replaceAttributeValues("test", Arrays.asList(values));
		} catch (SchemaViolationException | ConnectException | FileNotFoundException | ConflictException | InterruptedException e) {
			throw new AssertionError(e);
		}
	}
}

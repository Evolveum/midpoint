/*
 * Copyright (c) 2010-2013 Evolveum
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
package com.evolveum.midpoint.model.impl.controller;

import java.util.ArrayList;

import org.testng.annotations.Test;
import org.testng.annotations.BeforeMethod;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;

import com.evolveum.midpoint.model.impl.ModelCrudService;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 *
 * @author lazyman
 *
 */
@ContextConfiguration(locations = { "classpath:ctx-model-test-no-repo.xml" })
public class ControllerModifyObjectTest extends AbstractTestNGSpringContextTests {

	//private static final Trace LOGGER = TraceManager.getTrace(ControllerModifyObjectTest.class);
	@Autowired
	private ModelCrudService controller;
	@Autowired
	@Qualifier("cacheRepositoryService")
	private RepositoryService repository;
	@Autowired
	private ProvisioningService provisioning;
	@Autowired
	private TaskManager taskManager;

	@BeforeMethod
	public void before() {
		Mockito.reset(repository, provisioning);
	}

	@Test(expectedExceptions = IllegalArgumentException.class)
	public void nullChange() throws Exception {
		controller.modifyObject(UserType.class, null, null, null, null, null);
	}

	@Test(expectedExceptions = IllegalArgumentException.class)
	public void nullChangeOid() throws Exception {
		controller.modifyObject(UserType.class, null, new ArrayList<>(), null, taskManager.createTaskInstance(), null);
	}

	@Test(expectedExceptions = IllegalArgumentException.class)
	public void emptyChangeOid() throws Exception {
		controller.modifyObject(UserType.class, "", new ArrayList<>(), null, taskManager.createTaskInstance(), null);
	}

	@Test(expectedExceptions = IllegalArgumentException.class)
	public void nullResult() throws Exception {
		controller.modifyObject(UserType.class, "1", new ArrayList<>(), null, taskManager.createTaskInstance(), null);
	}
}

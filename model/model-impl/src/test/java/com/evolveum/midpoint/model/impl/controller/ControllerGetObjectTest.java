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

import com.evolveum.midpoint.model.api.ModelService;
import org.testng.annotations.Test;
import org.testng.annotations.BeforeMethod;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.Collection;

import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;

import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 *
 * @author lazyman
 *
 */
@ContextConfiguration(locations = { "classpath:ctx-model-test-no-repo.xml" })
public class ControllerGetObjectTest extends AbstractTestNGSpringContextTests  {

	private static final File TEST_FOLDER = new File("./src/test/resources/controller/getObject");
	private static final File TEST_FOLDER_COMMON = new File("./src/test/resources/common");
	private static final Trace LOGGER = TraceManager.getTrace(ControllerGetObjectTest.class);
	@Autowired(required = true)
	private ModelService controller;
	@Autowired(required = true)
	@Qualifier("cacheRepositoryService")
	private RepositoryService repository;
	@Autowired(required = true)
	private TaskManager taskManager;

	@BeforeMethod
	public void before() {
		Mockito.reset(repository);
	}

	@Test(expectedExceptions = IllegalArgumentException.class)
	public void getObjectNullOid() throws Exception {
		controller.getObject(null, null, null, null, null);
	}

	@Test(expectedExceptions = IllegalArgumentException.class)
	public void getObjectNullPropertyReferenceListType() throws Exception {
		controller.getObject(null, "1", null, null, null);
	}

	@Test(expectedExceptions = IllegalArgumentException.class)
	public void getObjectNullResultType() throws Exception {
		controller.getObject(null, "1", null, null, null);
	}

	@Test(expectedExceptions = IllegalArgumentException.class)
	public void nullClass() throws Exception {
		Task task = taskManager.createTaskInstance("Get Object");
		controller.getObject(null, "abababab-abab-abab-abab-000000000001", null, task,
				task.getResult());
	}

	@SuppressWarnings("unchecked")
	@Test(expectedExceptions = ObjectNotFoundException.class)
	public void getNonExistingObject() throws Exception {
		final String oid = "abababab-abab-abab-abab-000000000001";
		Task task = taskManager.createTaskInstance("Get Object");
		when(repository.getObject(any(Class.class),eq(oid), any(Collection.class), any(OperationResult.class)))
				.thenThrow(new ObjectNotFoundException("Object with oid '" + oid + "' not found."));

		controller.getObject(ObjectType.class, oid, null, task, task.getResult());
	}

}

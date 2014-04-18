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
package com.evolveum.midpoint.model.controller;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;

import java.io.File;
import java.util.Collection;
import java.util.List;

import com.evolveum.midpoint.prism.util.JaxbTestUtil;
import com.evolveum.midpoint.test.util.TestUtil;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.OrderDirection;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.api_types_2.ObjectListType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;

/**
 * 
 * @author lazyman
 * 
 */
@ContextConfiguration(locations = { "classpath:ctx-model-test-no-repo.xml" })
public class ControllerListObjectsTest extends AbstractTestNGSpringContextTests {

	private static final File TEST_FOLDER = new File("./src/test/resources/controller/listObjects");
	private static final File TEST_FOLDER_COMMON = new File("./src/test/resources/common");
	private static final Trace LOGGER = TraceManager.getTrace(ControllerListObjectsTest.class);
	@Autowired(required = true)
	private ModelController controller;
	@Autowired(required = true)
	@Qualifier("cacheRepositoryService")
	private RepositoryService repository;
	@Autowired(required = true)
	private ProvisioningService provisioning;
	@Autowired(required = true)
	private TaskManager taskManager;

	@BeforeMethod
	public void before() {
		Mockito.reset(repository, provisioning);
	}

	@Test(expectedExceptions = IllegalArgumentException.class)
	public void nullClassType() throws Exception {
		controller.searchObjects(null, null, null, null, null);
	}

	@Test(expectedExceptions = IllegalArgumentException.class)
	public void nullPaging() throws Exception {
		controller.searchObjects(UserType.class, null, null, null, null);
	}

	@Test(expectedExceptions = IllegalArgumentException.class)
	public void nullResult() throws Exception {
		//original was PagingTypeFactory.createListAllPaging()
		ObjectPaging paging = ObjectPaging.createPaging(0, Integer.MAX_VALUE, ObjectType.F_NAME, OrderDirection.ASCENDING);
		ObjectQuery query = ObjectQuery.createObjectQuery(paging);
		controller.searchObjects(UserType.class, query, null, null, null);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void userList() throws Exception {
		final List<PrismObject<UserType>> expectedUserList = MiscSchemaUtil.toList(UserType.class,
				JaxbTestUtil.getInstance().unmarshalObject(new File(TEST_FOLDER, "user-list.xml"), ObjectListType.class));

		when(repository.searchObjects(eq(UserType.class), any(ObjectQuery.class), 
				any(Collection.class), any(OperationResult.class)))
				.thenReturn(expectedUserList);

		Task task = taskManager.createTaskInstance("List Users");
		try {
			final List<PrismObject<UserType>> returnedUserList = controller.searchObjects(UserType.class, new ObjectQuery(), null, task, task.getResult());

			verify(repository, times(1)).searchObjects(eq(ObjectTypes.USER.getClassDefinition()), any(ObjectQuery.class), 
					any(Collection.class), any(OperationResult.class));
			testObjectList((List)expectedUserList, (List)returnedUserList);
		} finally {
			LOGGER.debug(task.getResult().debugDump());
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test(enabled = false)
	private void testObjectList(List<PrismObject<? extends ObjectType>> expectedList,
			List<PrismObject<? extends ObjectType>> returnedList) {
		assertNotNull(expectedList);
		assertNotNull(returnedList);

		assertTrue(expectedList == null ? returnedList == null : returnedList != null);
		if (expectedList == null) {
			return;
		}
		assertEquals(expectedList.size(), returnedList.size());
		if (expectedList.size() == 0) {
			return;
		}

		assertEquals(expectedList, returnedList);
//		if (expectedList.get(0).asObjectable() instanceof UserType) {
//			testUserLists(new ArrayList(expectedList), new ArrayList(returnedList));
//		}
	}

//	@Test(enabled = false)
//	private void testUserLists(List<UserType> expected, List<UserType> returned) {
//		UserTypeComparator comp = new UserTypeComparator();
//		for (int i = 0; i < expected.size(); i++) {
//			UserType u1 = expected.get(i);
//			UserType u2 = returned.get(i);
//
//			assertTrue(comp.areEqual(u1, u2));
//		}
//	}
}

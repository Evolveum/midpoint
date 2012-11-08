/*
 * Copyright (c) 2011 Evolveum
 * 
 * The contents of this file are subject to the terms of the Common Development
 * and Distribution License (the License). You may not use this file except in
 * compliance with the License.
 * 
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or CDDLv1.0.txt file in the source
 * code distribution. See the License for the specific language governing
 * permission and limitations under the License.
 * 
 * If applicable, add the following below the CDDL Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * 
 * Portions Copyrighted 2011 [name of copyright owner]
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
import java.util.List;

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
import com.evolveum.midpoint.schema.PagingTypeFactory;
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
import com.evolveum.prism.xml.ns._public.query_2.PagingType;

/**
 * 
 * @author lazyman
 * 
 */
@ContextConfiguration(locations = { "classpath:application-context-model.xml",
		"classpath:application-context-model-unit-test.xml",
		"classpath:application-context-configuration-test-no-repo.xml",
		"classpath:application-context-task.xml",
		"classpath:application-context-audit.xml"})
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
				PrismTestUtil.unmarshalObject(new File(TEST_FOLDER, "user-list.xml"), ObjectListType.class));

		when(repository.searchObjects(eq(UserType.class), any(ObjectQuery.class), any(OperationResult.class)))
				.thenReturn(expectedUserList);

		Task task = taskManager.createTaskInstance("List Users");
		try {
			final List<PrismObject<UserType>> returnedUserList = controller.searchObjects(UserType.class, new ObjectQuery(), null, task, task.getResult());

			verify(repository, times(1)).searchObjects(eq(ObjectTypes.USER.getClassDefinition()), any(ObjectQuery.class), any(OperationResult.class));
			testObjectList((List)expectedUserList, (List)returnedUserList);
		} finally {
			LOGGER.debug(task.getResult().dump());
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

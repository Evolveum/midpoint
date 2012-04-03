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

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.PagingTypeFactory;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.api_types_2.ObjectListType;
import com.evolveum.midpoint.xml.ns._public.common.api_types_2.PagingType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserType;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.util.List;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;
import static org.testng.AssertJUnit.*;

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

	@BeforeMethod
	public void before() {
		Mockito.reset(repository, provisioning);
	}

	@Test(expectedExceptions = IllegalArgumentException.class)
	public void nullClassType() throws Exception {
		controller.listObjects(null, null, null);
	}

	@Test(expectedExceptions = IllegalArgumentException.class)
	public void nullPaging() throws Exception {
		controller.listObjects(UserType.class, null, null);
	}

	@Test(expectedExceptions = IllegalArgumentException.class)
	public void nullResult() throws Exception {
		controller.listObjects(UserType.class, PagingTypeFactory.createListAllPaging(), null);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void userList() throws Exception {
		final List<PrismObject<UserType>> expectedUserList = MiscSchemaUtil.toList(UserType.class,
				PrismTestUtil.unmarshalObject(new File(TEST_FOLDER, "user-list.xml"), ObjectListType.class));

		when(repository.listObjects(eq(UserType.class), any(PagingType.class), any(OperationResult.class)))
				.thenReturn(expectedUserList);

		OperationResult result = new OperationResult("List Users");
		try {
			final List<PrismObject<UserType>> returnedUserList = controller.listObjects(UserType.class, new PagingType(),
					result);

			verify(repository, times(1)).listObjects(eq(ObjectTypes.USER.getClassDefinition()),
					any(PagingType.class), any(OperationResult.class));
			testObjectList((List)expectedUserList, (List)returnedUserList);
		} finally {
			LOGGER.debug(result.dump());
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

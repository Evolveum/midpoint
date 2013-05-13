/*
 * Copyright (c) 2011 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2011 [name of copyright owner]
 */
package com.evolveum.midpoint.model.controller;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import org.testng.annotations.Test;
import org.testng.annotations.BeforeMethod;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileNotFoundException;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;

import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;

import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.JAXBUtil;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.api_types_2.PropertyReferenceListType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;
import com.evolveum.midpoint.xml.ns._public.common.fault_1_wsdl.FaultMessage;

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
	private ModelController controller;
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
		when(repository.getObject(any(Class.class),eq(oid), any(OperationResult.class)))
				.thenThrow(new ObjectNotFoundException("Object with oid '" + oid + "' not found."));

		controller.getObject(ObjectType.class, oid, null, task, task.getResult());
	}

	@Test
	@SuppressWarnings("unchecked")
	public void getUserCorrect() throws Exception {
		final UserType expectedUser = PrismTestUtil.unmarshalObject(new File(TEST_FOLDER,
				"get-user-correct.xml"), UserType.class);

		final String oid = "abababab-abab-abab-abab-000000000001";
		when(repository.getObject(eq(UserType.class),eq(oid), any(OperationResult.class)))
				.thenReturn(expectedUser.asPrismObject());

		Task task = taskManager.createTaskInstance("Get Object");
		try {
			final UserType user = controller.getObject(UserType.class, oid, null, task, task.getResult()
					).asObjectable();

			assertNotNull(user);
			assertEquals(expectedUser.getName(), user.getName());

			verify(repository, atLeastOnce()).getObject(eq(UserType.class), eq(oid),
					any(OperationResult.class));
		} finally {
			LOGGER.debug("getUserCorrect" + task.getResult().dump());
		}
	}
}

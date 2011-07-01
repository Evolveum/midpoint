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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.evolveum.midpoint.api.logging.Trace;
import com.evolveum.midpoint.common.jaxb.JAXBUtil;
import com.evolveum.midpoint.common.result.OperationResult;
import com.evolveum.midpoint.logging.TraceManager;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.exception.ObjectNotFoundException;
import com.evolveum.midpoint.schema.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyReferenceListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserType;
import com.evolveum.midpoint.xml.ns._public.model.model_1.FaultMessage;

/**
 * 
 * @author lazyman
 * 
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:application-context-model.xml",
		"classpath:application-context-model-unit-test.xml" })
public class ControllerGetObjectTest {

	private static final File TEST_FOLDER = new File("./src/test/resources/controller/getObject");
	private static final Trace LOGGER = TraceManager.getTrace(ControllerGetObjectTest.class);
	@Autowired(required = true)
	private ModelController controller;
	@Autowired(required = true)
	private RepositoryService repository;

	@Test(expected = IllegalArgumentException.class)
	public void getObjectNullOid() throws ObjectNotFoundException {
		controller.getObject(null, null, null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void getObjectNullPropertyReferenceListType() throws ObjectNotFoundException {
		controller.getObject("1", null, null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void getObjectNullResultType() throws ObjectNotFoundException {
		controller.getObject("1", new PropertyReferenceListType(), null);
	}

	@Test(expected = ObjectNotFoundException.class)
	public void getNonExistingObject() throws ObjectNotFoundException, SchemaException {
		final String oid = "abababab-abab-abab-abab-000000000001";
		when(repository.getObject(eq(oid), any(PropertyReferenceListType.class), any(OperationResult.class)))
				.thenThrow(new ObjectNotFoundException("Object with oid '" + oid + "' not found."));

		controller.getObject(oid, new PropertyReferenceListType(), new OperationResult("Get Object"));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void getUserCorrect() throws JAXBException, FaultMessage, ObjectNotFoundException, SchemaException {
		final UserType expectedUser = ((JAXBElement<UserType>) JAXBUtil.unmarshal(new File(TEST_FOLDER,
				"get-user-correct.xml"))).getValue();

		final String oid = "abababab-abab-abab-abab-000000000001";
		when(repository.getObject(eq(oid), any(PropertyReferenceListType.class), any(OperationResult.class)))
				.thenReturn(expectedUser);

		OperationResult result = new OperationResult("Get Object");
		try {
			final UserType user = (UserType) controller.getObject(oid, new PropertyReferenceListType(),
					result);

			assertNotNull(user);
			assertEquals(expectedUser.getName(), user.getName());

			verify(repository, atLeastOnce()).getObject(eq(oid), any(PropertyReferenceListType.class),
					any(OperationResult.class));
		} finally {
			LOGGER.debug("getUserCorrect" + result.debugDump());
		}
	}
}

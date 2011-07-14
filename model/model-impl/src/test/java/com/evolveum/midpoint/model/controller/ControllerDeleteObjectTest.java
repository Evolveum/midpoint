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

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.times;
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
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.exception.CommunicationException;
import com.evolveum.midpoint.schema.exception.ObjectNotFoundException;
import com.evolveum.midpoint.schema.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyReferenceListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ScriptsType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserType;
import com.evolveum.midpoint.xml.ns._public.model.model_1.FaultMessage;

/**
 * 
 * @author lazyman
 * 
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:application-context-model.xml",
		"classpath:application-context-model-unit-test.xml", "classpath:application-context-task.xml" })
public class ControllerDeleteObjectTest {

	private static final File TEST_FOLDER = new File("./src/test/resources/controller/deleteObject");
	private static final Trace LOGGER = TraceManager.getTrace(ControllerDeleteObjectTest.class);
	@Autowired(required = true)
	private ModelController controller;
	@Autowired(required = true)
	private RepositoryService repository;
	@Autowired(required = true)
	private ProvisioningService provisioning;

	@Test(expected = IllegalArgumentException.class)
	public void nullOid() throws Exception {
		controller.deleteObject(null, null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void emptyOid() throws Exception {
		controller.deleteObject("", null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void nullResult() throws Exception {
		controller.deleteObject("1", null);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testDeleteCorrectRepo() throws FaultMessage, JAXBException, ObjectNotFoundException,
			SchemaException {
		final UserType expectedUser = ((JAXBElement<UserType>) JAXBUtil.unmarshal(new File(TEST_FOLDER,
				"delete-user.xml"))).getValue();

		final String oid = "abababab-abab-abab-abab-000000000001";
		when(repository.getObject(eq(oid), any(PropertyReferenceListType.class), any(OperationResult.class)))
				.thenReturn(expectedUser);
		OperationResult result = new OperationResult("Delete Object From Repo");
		try {
			controller.deleteObject(oid, result);
		} finally {
			LOGGER.debug(result.debugDump());
		}
		verify(repository, atLeastOnce()).getObject(eq(oid), any(PropertyReferenceListType.class),
				any(OperationResult.class));
		verify(repository, times(1)).deleteObject(eq(oid), any(OperationResult.class));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testDeleteCorrectProvisioning() throws FaultMessage, JAXBException, ObjectNotFoundException,
			SchemaException, CommunicationException {
		final ResourceType expectedUser = ((JAXBElement<ResourceType>) JAXBUtil.unmarshal(new File(
				TEST_FOLDER, "delete-resource.xml"))).getValue();

		final String oid = "abababab-abab-abab-abab-000000000001";
		when(repository.getObject(eq(oid), any(PropertyReferenceListType.class), any(OperationResult.class)))
				.thenReturn(expectedUser);
		OperationResult result = new OperationResult("Delete Object From Provisioning");
		try {
			controller.deleteObject(oid, result);
		} finally {
			LOGGER.debug(result.debugDump());
		}

		verify(repository, atLeastOnce()).getObject(eq(oid), any(PropertyReferenceListType.class),
				any(OperationResult.class));
		verify(provisioning, times(1)).deleteObject(eq(oid), any(ScriptsType.class),
				any(OperationResult.class));
	}
}

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
 * Portions Copyrighted 2010 Forgerock
 */

package com.evolveum.midpoint.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.ws.Holder;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.evolveum.midpoint.common.jaxb.JAXBUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectContainerType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.OperationResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyReferenceListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserType;
import com.evolveum.midpoint.xml.ns._public.common.fault_1.ObjectNotFoundFaultType;
import com.evolveum.midpoint.xml.ns._public.model.model_1.FaultMessage;
import com.evolveum.midpoint.xml.ns._public.model.model_1.ModelPortType;
import com.evolveum.midpoint.xml.ns._public.provisioning.provisioning_1.ProvisioningPortType;
import com.evolveum.midpoint.xml.ns._public.repository.repository_1.RepositoryPortType;

/**
 * 
 * @author lazyman
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:application-context-model.xml",
		"classpath:application-context-model-unit-test.xml" })
public class ModelGetObjectTest {

	private static final File TEST_FOLDER = new File("./src/test/resources/service/model/get");
	@Autowired(required = true)
	ModelPortType modelService;
	@Autowired(required = true)
	ProvisioningPortType provisioningService;
	@Autowired(required = true)
	RepositoryPortType repositoryService;

	// @Autowired(required = true)
	// SchemaHandling schemaHandling;

	@Before
	public void before() {
		Mockito.reset(provisioningService, repositoryService);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testGetNullOid() throws FaultMessage {
		modelService.getObject(null, new PropertyReferenceListType(), new Holder<OperationResultType>(
				new OperationResultType()));
		fail("get must fail");
	}

	@Test(expected = IllegalArgumentException.class)
	public void testGetEmptyOid() throws FaultMessage {
		modelService.getObject("", new PropertyReferenceListType(), new Holder<OperationResultType>(
				new OperationResultType()));
		fail("get must fail");
	}

	@Test(expected = IllegalArgumentException.class)
	public void testGetNullOidAndPropertyRef() throws FaultMessage {
		modelService.getObject(null, null, new Holder<OperationResultType>(new OperationResultType()));
		fail("get must fail");
	}

	@Test(expected = IllegalArgumentException.class)
	public void testGetNullPropertyRef() throws FaultMessage {
		modelService.getObject("001", null, new Holder<OperationResultType>(new OperationResultType()));
		fail("get must fail");
	}

	@Test(expected = FaultMessage.class)
	public void getNonexistingObject() throws FaultMessage,
			com.evolveum.midpoint.xml.ns._public.repository.repository_1.FaultMessage {

		final String oid = "abababab-abab-abab-abab-000000000001";
		when(repositoryService.getObject(eq(oid), any(PropertyReferenceListType.class))).thenThrow(
				new com.evolveum.midpoint.xml.ns._public.repository.repository_1.FaultMessage(
						"Object with oid '' not found.", new ObjectNotFoundFaultType()));

		modelService.getObject(oid, new PropertyReferenceListType(), new Holder<OperationResultType>(
				new OperationResultType()));
		fail("get must fail");
	}

	@Test
	@SuppressWarnings("unchecked")
	public void getUserCorrect() throws JAXBException, FaultMessage,
			com.evolveum.midpoint.xml.ns._public.repository.repository_1.FaultMessage {

		ObjectContainerType container = new ObjectContainerType();
		final UserType expectedUser = ((JAXBElement<UserType>) JAXBUtil.unmarshal(new File(TEST_FOLDER,
				"get-user-correct.xml"))).getValue();
		container.setObject(expectedUser);

		final String oid = "abababab-abab-abab-abab-000000000001";
		when(repositoryService.getObject(eq(oid), any(PropertyReferenceListType.class)))
				.thenReturn(container);
		final UserType user = (UserType) modelService.getObject(oid, new PropertyReferenceListType(),
				new Holder<OperationResultType>(new OperationResultType()));

		assertNotNull(user);
		assertEquals(expectedUser.getName(), user.getName());

		verify(repositoryService, atLeastOnce()).getObject(eq(oid), any(PropertyReferenceListType.class));

	}
}

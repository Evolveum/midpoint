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

import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.times;
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
import com.evolveum.midpoint.common.result.OperationResult;
import com.evolveum.midpoint.model.test.util.ModelServiceUtil;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.exception.CommunicationException;
import com.evolveum.midpoint.schema.exception.ObjectNotFoundException;
import com.evolveum.midpoint.schema.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_1.OperationResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyReferenceListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ScriptsType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserType;
import com.evolveum.midpoint.xml.ns._public.model.model_1.FaultMessage;
import com.evolveum.midpoint.xml.ns._public.model.model_1.ModelPortType;

/**
 * 
 * @author lazyman
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:application-context-model.xml",
		"classpath:application-context-model-unit-test.xml" })
public class ModelDeleteObjectTest {

	private static final File TEST_FOLDER = new File("./src/test/resources/service/model/delete");
	@Autowired(required = true)
	ModelPortType modelService;
	@Autowired(required = true)
	ProvisioningService provisioningService;
	@Autowired(required = true)
	RepositoryService repositoryService;

	@Before
	public void before() {
		Mockito.reset(provisioningService, repositoryService);
	}

	@Test(expected = FaultMessage.class)
	public void testDeleteNullOid() throws FaultMessage {
		try {
			modelService.deleteObject(null, new Holder<OperationResultType>(new OperationResultType()));
		} catch (FaultMessage ex) {
			ModelServiceUtil.assertIllegalArgumentFault(ex);
		}
		fail("delete must fail");
	}

	@Test(expected = FaultMessage.class)
	public void testDeleteEmptyOid() throws FaultMessage {
		try {
			modelService.deleteObject("", null);
		} catch (FaultMessage ex) {
			ModelServiceUtil.assertIllegalArgumentFault(ex);
		}
		fail("delete must fail");
	}

	@Test(expected = FaultMessage.class)
	public void testDeleteNullResult() throws FaultMessage {
		try {
			modelService.deleteObject("1", null);
		} catch (FaultMessage ex) {
			ModelServiceUtil.assertIllegalArgumentFault(ex);
		}
		fail("delete must fail");
	}

	@Test(expected = FaultMessage.class)
	public void testDeleteNonExisting() throws FaultMessage, ObjectNotFoundException, SchemaException {
		final String oid = "abababab-abab-abab-abab-000000000001";
		when(
				repositoryService.getObject(eq(oid), any(PropertyReferenceListType.class),
						any(OperationResult.class))).thenThrow(
				new ObjectNotFoundException("Object with oid '' not found."));

		modelService.deleteObject(oid, new Holder<OperationResultType>(new OperationResultType()));
		fail("delete must fail");
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testDeleteCorrectRepo() throws FaultMessage, JAXBException, ObjectNotFoundException,
			SchemaException {
		final UserType expectedUser = ((JAXBElement<UserType>) JAXBUtil.unmarshal(new File(TEST_FOLDER,
				"delete-user.xml"))).getValue();

		final String oid = "abababab-abab-abab-abab-000000000001";
		when(
				repositoryService.getObject(eq(oid), any(PropertyReferenceListType.class),
						any(OperationResult.class))).thenReturn(expectedUser);
		modelService.deleteObject(oid, new Holder<OperationResultType>(new OperationResultType()));

		verify(repositoryService, atLeastOnce()).getObject(eq(oid), any(PropertyReferenceListType.class),
				any(OperationResult.class));
		verify(repositoryService, times(1)).deleteObject(eq(oid), any(OperationResult.class));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testDeleteCorrectProvisioning() throws FaultMessage, JAXBException, ObjectNotFoundException,
			SchemaException, CommunicationException {
		final ResourceType expectedUser = ((JAXBElement<ResourceType>) JAXBUtil.unmarshal(new File(
				TEST_FOLDER, "delete-resource.xml"))).getValue();

		final String oid = "abababab-abab-abab-abab-000000000001";
		when(
				repositoryService.getObject(eq(oid), any(PropertyReferenceListType.class),
						any(OperationResult.class))).thenReturn(expectedUser);
		modelService.deleteObject(oid, new Holder<OperationResultType>(new OperationResultType()));

		verify(repositoryService, atLeastOnce()).getObject(eq(oid), any(PropertyReferenceListType.class),
				any(OperationResult.class));
		verify(provisioningService, times(1)).deleteObject(eq(oid), any(ScriptsType.class),
				any(OperationResult.class));
	}
}

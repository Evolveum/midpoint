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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.List;

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

import com.evolveum.midpoint.api.logging.Trace;
import com.evolveum.midpoint.common.Utils;
import com.evolveum.midpoint.common.jaxb.JAXBUtil;
import com.evolveum.midpoint.logging.TraceManager;
import com.evolveum.midpoint.model.test.util.ResourceObjectShadowTypeComparator;
import com.evolveum.midpoint.model.xpath.SchemaHandling;
import com.evolveum.midpoint.schema.ObjectTypes;
import com.evolveum.midpoint.xml.ns._public.common.common_1.OperationResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceObjectShadowListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceObjectShadowType;
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
public class ModelListResourceObjectShadowsTest {

	private static final Trace trace = TraceManager.getTrace(ModelListResourceObjectShadowsTest.class);
	private static final File TEST_FOLDER = new File("./src/test/resources/service/model/list");
	@Autowired(required = true)
	ModelPortType modelService;
	@Autowired(required = true)
	ProvisioningPortType provisioningService;
	@Autowired(required = true)
	RepositoryPortType repositoryService;
	@Autowired(required = true)
	SchemaHandling schemaHandling;

	@Before
	public void before() {
		Mockito.reset(provisioningService, repositoryService);
	}

	@Test(expected = IllegalArgumentException.class)
	public void nullResourceOid() throws FaultMessage {
		modelService.listResourceObjectShadows(null, "notRelevant", new Holder<OperationResultType>(
				new OperationResultType()));
		fail("Illegal argument excetion must be thrown");
	}

	@Test(expected = IllegalArgumentException.class)
	public void emptyResourceOid() throws FaultMessage {
		modelService.listResourceObjectShadows(null, "notRelevant", new Holder<OperationResultType>(
				new OperationResultType()));
		fail("Illegal argument excetion must be thrown");
	}

	@Test(expected = IllegalArgumentException.class)
	public void nullShadowType() throws FaultMessage {
		modelService.listResourceObjectShadows("1", null, new Holder<OperationResultType>(
				new OperationResultType()));
		fail("Illegal argument excetion must be thrown");
	}

	@Test(expected = IllegalArgumentException.class)
	public void emptyShadowType() throws FaultMessage {
		modelService.listResourceObjectShadows("1", "", new Holder<OperationResultType>(
				new OperationResultType()));
		fail("Illegal argument excetion must be thrown");
	}

	@Test(expected = FaultMessage.class)
	public void nonexistingResourceOid() throws FaultMessage,
			com.evolveum.midpoint.xml.ns._public.repository.repository_1.FaultMessage {

		final String resourceOid = "abababab-abab-abab-abab-000000000001";
		when(repositoryService.listResourceObjectShadows(resourceOid, ObjectTypes.ACCOUNT.getObjectTypeUri()))
				.thenThrow(
						new com.evolveum.midpoint.xml.ns._public.repository.repository_1.FaultMessage(
								"Resource with oid '" + resourceOid + "' not found.",
								new ObjectNotFoundFaultType()));

		modelService.listResourceObjectShadows(resourceOid, ObjectTypes.ACCOUNT.getObjectTypeUri(),
				new Holder<OperationResultType>(new OperationResultType()));

		fail("Fault must be thrown");
	}

	@Test(expected = IllegalArgumentException.class)
	public void badResourceShadowType() throws FaultMessage {
		modelService.listResourceObjectShadows("abababab-abab-abab-abab-000000000001",
				ObjectTypes.GENERIC_OBJECT.getObjectTypeUri(), new Holder<OperationResultType>(
						new OperationResultType()));

		fail("Fault must be thrown");
	}

	@Test
	@SuppressWarnings("unchecked")
	public void correctList() throws FaultMessage, JAXBException,
			com.evolveum.midpoint.xml.ns._public.repository.repository_1.FaultMessage {

		final String resourceOid = "abababab-abab-abab-abab-000000000001";
		final ResourceObjectShadowListType expected = ((JAXBElement<ResourceObjectShadowListType>) JAXBUtil
				.unmarshal(new File(TEST_FOLDER, "resource-object-shadow-list.xml"))).getValue();
		trace.warn("TODO: File resource-object-shadow-list.xml doesn't contain proper resource object shadow list.");

		when(repositoryService.listResourceObjectShadows(resourceOid, ObjectTypes.ACCOUNT.getObjectTypeUri()))
				.thenReturn(expected);

		final ResourceObjectShadowListType returned = modelService.listResourceObjectShadows(resourceOid,
				ObjectTypes.ACCOUNT.getObjectTypeUri(), new Holder<OperationResultType>(
						new OperationResultType()));

		assertNotNull(expected);
		assertNotNull(returned);
		testShadowListType(expected, returned);
	}

	private void testShadowListType(ResourceObjectShadowListType expected,
			ResourceObjectShadowListType returned) {
		List<ResourceObjectShadowType> expectedList = expected.getObject();
		List<ResourceObjectShadowType> returnedList = returned.getObject();

		assertTrue(expectedList == null ? returnedList == null : returnedList != null);
		assertEquals(expected.getObject().size(), returned.getObject().size());
		ResourceObjectShadowTypeComparator comp = new ResourceObjectShadowTypeComparator();
		for (int i = 0; i < expectedList.size(); i++) {
			assertTrue(comp.areEqual(expectedList.get(i), returnedList.get(i)));
		}
	}
}

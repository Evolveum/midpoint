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
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.ws.Holder;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.evolveum.midpoint.common.jaxb.JAXBUtil;
import com.evolveum.midpoint.common.result.OperationResult;
import com.evolveum.midpoint.model.test.util.ModelServiceUtil;
import com.evolveum.midpoint.model.test.util.mock.ObjectTypeNameMatcher;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.exception.CommunicationException;
import com.evolveum.midpoint.schema.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.schema.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectContainerType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.OperationResultType;
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
@ContextConfiguration(locations = { "classpath:application-context-model-unit-test.xml",
		"classpath:application-context-model.xml" })
public class ModelAddObjectTest {

	private static final File TEST_FOLDER = new File("./src/test/resources/service/model/add");
	private static final File TEST_FOLDER_CONTROLLER = new File("./src/test/resources/controller/addObject");
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
	public void addNullObject() throws FaultMessage {
		modelService.addObject(null, new Holder<OperationResultType>(new OperationResultType()));
		fail("Add must fail.");
	}

	@Test(expected = FaultMessage.class)
	@SuppressWarnings("unchecked")
	public void addUserWithoutName() throws Exception {
		final UserType expectedUser = ((JAXBElement<UserType>) JAXBUtil.unmarshal(new File(
				TEST_FOLDER_CONTROLLER, "add-user-without-name.xml"))).getValue();
		try {
			modelService.addObject(expectedUser, new Holder<OperationResultType>(new OperationResultType()));
		} catch (FaultMessage ex) {
			ModelServiceUtil.assertIllegalArgumentFault(ex);
		}
		fail("add must fail.");
	}

	@Ignore
	// FIXME: fix test
	@Test
	@SuppressWarnings("unchecked")
	public void addResourceCorrect() throws JAXBException, FaultMessage, ObjectAlreadyExistsException,
			SchemaException, CommunicationException {
		ObjectContainerType container = new ObjectContainerType();
		final ResourceType expectedResource = ((JAXBElement<ResourceType>) JAXBUtil.unmarshal(new File(
				TEST_FOLDER, "add-resource-correct.xml"))).getValue();
		container.setObject(expectedResource);

		final String oid = "abababab-abab-abab-abab-000000000002";
		when(
				provisioningService.addObject(argThat(new ObjectTypeNameMatcher(expectedResource.getName())),
						any(ScriptsType.class), any(OperationResult.class))).thenAnswer(new Answer<String>() {

			@Override
			public String answer(InvocationOnMock invocation) throws Throwable {
				ObjectContainerType container = (ObjectContainerType) invocation.getArguments()[0];
				ResourceType resource = (ResourceType) container.getObject();

				assertEquals(expectedResource.getName(), resource.getName());
				assertEquals(expectedResource.getNamespace(), resource.getNamespace());
				assertEquals(expectedResource.getSchema(), resource.getSchema());
				assertEquals(expectedResource.getScripts(), resource.getScripts());
				assertEquals(expectedResource.getType(), resource.getType());

				return oid;
			}
		});
		String result = modelService.addObject(expectedResource, new Holder<OperationResultType>(
				new OperationResultType()));
		verify(provisioningService, times(1)).addObject(
				argThat(new ObjectTypeNameMatcher(expectedResource.getName())), any(ScriptsType.class),
				any(OperationResult.class));
		assertEquals(oid, result);
	}
}

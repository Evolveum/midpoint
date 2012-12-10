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
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;

import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

import com.evolveum.midpoint.model.api.PolicyViolationException;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.MidPointPrismContextFactory;
import com.evolveum.midpoint.schema.ObjectOperationOption;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.JAXBUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ConsistencyViolationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.api_types_2.PropertyReferenceListType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ProvisioningScriptsType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;
import com.evolveum.midpoint.xml.ns._public.common.fault_1_wsdl.FaultMessage;

/**
 * 
 * @author lazyman
 * 
 */
@ContextConfiguration(locations = { "classpath:ctx-model.xml",
		"classpath:ctx-model-unit-test.xml",
		"classpath:ctx-configuration-test-no-repo.xml",
		"classpath:ctx-task.xml",
		"classpath:ctx-audit.xml"})
public class ControllerDeleteObjectTest extends AbstractTestNGSpringContextTests {

	private static final File TEST_FOLDER = new File("./src/test/resources/controller/deleteObject");
	private static final File TEST_FOLDER_COMMON = new File("./src/test/resources/common");
	private static final Trace LOGGER = TraceManager.getTrace(ControllerDeleteObjectTest.class);
	@Autowired(required = true)
	private ModelController controller;
	@Autowired(required = true)
	@Qualifier("cacheRepositoryService")
	private RepositoryService repository;
	@Autowired(required = true)
	private ProvisioningService provisioning;
	@Autowired(required = true)
	private TaskManager taskManager;

	@BeforeSuite
	public void setup() throws SchemaException, SAXException, IOException {
		PrettyPrinter.setDefaultNamespacePrefix(MidPointConstants.NS_MIDPOINT_PUBLIC_PREFIX);
		PrismTestUtil.resetPrismContext(MidPointPrismContextFactory.FACTORY);
	}
	
	@BeforeMethod
	public void before() {
		Mockito.reset(repository, provisioning);
	}

	@Test(expectedExceptions = IllegalArgumentException.class)
	public void nullOid() throws Exception {
		controller.deleteObject(UserType.class, null, null, null);
	}

	@Test(expectedExceptions = IllegalArgumentException.class)
	public void emptyOid() throws Exception {
		controller.deleteObject(UserType.class, "", null, null);
	}

	@Test(expectedExceptions = IllegalArgumentException.class)
	public void nullResult() throws Exception {
		controller.deleteObject(UserType.class, "1", null, null);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testDeleteCorrectRepo() throws FaultMessage, JAXBException, ObjectNotFoundException, SchemaException,
			ConsistencyViolationException, CommunicationException, FileNotFoundException, ConfigurationException, 
			PolicyViolationException, SecurityViolationException {
		final UserType expectedUser = PrismTestUtil.unmarshalObject(new File(TEST_FOLDER,
				"delete-user.xml"), UserType.class);

		final String oid = "abababab-abab-abab-abab-000000000001";
		when(
				repository.getObject(any(Class.class), eq(oid),
						any(OperationResult.class))).thenReturn(expectedUser.asPrismObject());
		OperationResult result = new OperationResult("Delete Object From Repo");
		try {
			controller.deleteObject(UserType.class, oid, taskManager.createTaskInstance(), result);
		} finally {
			LOGGER.debug(result.dump());
		}
		verify(repository, times(1)).deleteObject(any(Class.class), eq(oid), any(OperationResult.class));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testDeleteCorrectProvisioning() throws FaultMessage, JAXBException, ObjectNotFoundException,
			SchemaException, CommunicationException, ConsistencyViolationException, FileNotFoundException, ConfigurationException, 
			PolicyViolationException, SecurityViolationException {
		final PrismObject<ResourceType> expectedResource = PrismTestUtil.parseObject(new File(TEST_FOLDER_COMMON,
				"resource-opendj.xml"));

		final String oid = "abababab-abab-abab-abab-000000000001";
		when(
				repository.getObject(any(Class.class), eq(oid),
						any(OperationResult.class))).thenReturn(expectedResource);
		OperationResult result = new OperationResult("Delete Object From Provisioning");
		try {
			controller.deleteObject(ResourceType.class, oid, taskManager.createTaskInstance(), result);
		} finally {
			LOGGER.debug(result.dump());
		}

		verify(provisioning, times(1)).deleteObject(any(Class.class), eq(oid), any(ObjectOperationOption.class), any(ProvisioningScriptsType.class),
				any(OperationResult.class));
	}
}

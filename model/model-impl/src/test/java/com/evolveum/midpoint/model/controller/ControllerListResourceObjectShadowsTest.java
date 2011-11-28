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

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;
import org.testng.annotations.Test;
import org.testng.annotations.BeforeMethod;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.List;

import javax.xml.bind.JAXBElement;

import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;

import com.evolveum.midpoint.model.test.util.equal.ResourceObjectShadowTypeComparator;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.ResultArrayList;
import com.evolveum.midpoint.schema.ResultList;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.JAXBUtil;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_1.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceObjectShadowListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceObjectShadowType;

/**
 * 
 * @author lazyman
 * 
 */
@ContextConfiguration(locations = { "classpath:application-context-model.xml",
		"classpath:application-context-model-unit-test.xml",
		"classpath:application-context-configuration-test-no-repo.xml", 
		"classpath:application-context-task.xml" })
public class ControllerListResourceObjectShadowsTest extends AbstractTestNGSpringContextTests {

	private static final File TEST_FOLDER = new File("./src/test/resources/controller/listObjects");
	private static final File TEST_FOLDER_COMMON = new File("./src/test/resources/common");
	private static final Trace LOGGER = TraceManager.getTrace(ControllerListResourceObjectShadowsTest.class);
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
	public void nullResourceOid() throws Exception {
		controller.listResourceObjectShadows(null, null, null);
	}

	@Test(expectedExceptions = IllegalArgumentException.class)
	public void emptyResourceOid() throws Exception {
		controller.listResourceObjectShadows("", null, null);
	}

	@Test(expectedExceptions = IllegalArgumentException.class)
	public void nullClassType() throws Exception {
		controller.listResourceObjectShadows("1", null, null);
	}

	@Test(expectedExceptions = IllegalArgumentException.class)
	public void nullResult() throws Exception {
		controller.listResourceObjectShadows("1", AccountShadowType.class, null);
	}

	@Test
	@SuppressWarnings("unchecked")
	public <T extends ResourceObjectShadowType> void correctList() throws Exception {

		final String resourceOid = "abababab-abab-abab-abab-000000000001";
		final ResourceObjectShadowListType expected = ((JAXBElement<ResourceObjectShadowListType>) JAXBUtil
				.unmarshal(new File(TEST_FOLDER, "resource-object-shadow-list.xml"))).getValue();
		LOGGER.warn("TODO: File resource-object-shadow-list.xml doesn't contain proper resource object shadow list.");

		when(
				repository.listResourceObjectShadows(eq(resourceOid),
						eq((Class<T>) ObjectTypes.ACCOUNT.getClassDefinition()), any(OperationResult.class)))
				.thenReturn(MiscSchemaUtil.toResultList((Class<T>) ObjectTypes.ACCOUNT.getClassDefinition(), expected.getObject()));

		OperationResult result = new OperationResult("List Resource Object Shadows");
		try {
			List<T> returned = controller.listResourceObjectShadows(resourceOid,
					(Class<T>) ObjectTypes.ACCOUNT.getClassDefinition(), result);

			assertNotNull(expected);
			assertNotNull(returned);
			testShadowListType(expected, returned);
		} finally {
			LOGGER.debug(result.dump());
		}
	}

	@Test(enabled = false)
	private <T extends ResourceObjectShadowType> void testShadowListType(ResourceObjectShadowListType expected,
			List<T> returnedList) {
		List<ResourceObjectShadowType> expectedList = expected.getObject();

		assertTrue(expectedList == null ? returnedList == null : returnedList != null);
		assertEquals(expected.getObject().size(), expectedList.size());
		ResourceObjectShadowTypeComparator comp = new ResourceObjectShadowTypeComparator();
		for (int i = 0; i < expectedList.size(); i++) {
			assertTrue(comp.areEqual(expectedList.get(i), returnedList.get(i)));
		}
	}
}

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
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.List;

import javax.xml.bind.JAXBElement;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.evolveum.midpoint.api.logging.Trace;
import com.evolveum.midpoint.common.jaxb.JAXBUtil;
import com.evolveum.midpoint.common.result.OperationResult;
import com.evolveum.midpoint.logging.TraceManager;
import com.evolveum.midpoint.model.test.util.equal.ResourceObjectShadowTypeComparator;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.ObjectTypes;
import com.evolveum.midpoint.xml.ns._public.common.common_1.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceObjectShadowListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceObjectShadowType;

/**
 * 
 * @author lazyman
 * 
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:application-context-model.xml",
		"classpath:application-context-model-unit-test.xml", "classpath:application-context-task.xml" })
public class ControllerListResourceObjectShadowsTest {

	private static final File TEST_FOLDER = new File("./src/test/resources/controller/listObjects");
	private static final Trace LOGGER = TraceManager.getTrace(ControllerListResourceObjectShadowsTest.class);
	@Autowired(required = true)
	private ModelController controller;
	@Autowired(required = true)
	private RepositoryService repository;
	@Autowired(required = true)
	private ProvisioningService provisioning;

	@Test(expected = IllegalArgumentException.class)
	public void nullResourceOid() throws Exception {
		controller.listResourceObjectShadows(null, null, null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void emptyResourceOid() throws Exception {
		controller.listResourceObjectShadows("", null, null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void nullClassType() throws Exception {
		controller.listResourceObjectShadows("1", null, null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void nullResult() throws Exception {
		controller.listResourceObjectShadows("1", AccountShadowType.class, null);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void correctList() throws Exception {

		final String resourceOid = "abababab-abab-abab-abab-000000000001";
		final ResourceObjectShadowListType expected = ((JAXBElement<ResourceObjectShadowListType>) JAXBUtil
				.unmarshal(new File(TEST_FOLDER, "resource-object-shadow-list.xml"))).getValue();
		LOGGER.warn("TODO: File resource-object-shadow-list.xml doesn't contain proper resource object shadow list.");

		when(
				repository.listResourceObjectShadows(eq(resourceOid),
						eq(ObjectTypes.ACCOUNT.getClassDefinition()), any(OperationResult.class)))
				.thenReturn(expected.getObject());

		OperationResult result = new OperationResult("List Resource Object Shadows");
		try {
			List<ResourceObjectShadowType> returned = controller.listResourceObjectShadows(resourceOid,
					ObjectTypes.ACCOUNT.getClassDefinition(), result);

			assertNotNull(expected);
			assertNotNull(returned);
			testShadowListType(expected, returned);
		} finally {
			LOGGER.debug(result.debugDump());
		}
	}

	private void testShadowListType(ResourceObjectShadowListType expected,
			List<ResourceObjectShadowType> returnedList) {
		List<ResourceObjectShadowType> expectedList = expected.getObject();

		assertTrue(expectedList == null ? returnedList == null : returnedList != null);
		assertEquals(expected.getObject().size(), expectedList.size());
		ResourceObjectShadowTypeComparator comp = new ResourceObjectShadowTypeComparator();
		for (int i = 0; i < expectedList.size(); i++) {
			assertTrue(comp.areEqual(expectedList.get(i), returnedList.get(i)));
		}
	}
}

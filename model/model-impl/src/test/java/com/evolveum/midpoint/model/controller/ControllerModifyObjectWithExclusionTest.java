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
import org.testng.annotations.Test;
import org.testng.annotations.BeforeMethod;
import org.testng.Assert;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.List;

import javax.xml.bind.JAXBElement;

import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.w3c.dom.Element;

import com.evolveum.midpoint.api.logging.Trace;
import com.evolveum.midpoint.common.result.OperationResult;
import com.evolveum.midpoint.logging.TraceManager;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.holder.XPathHolder;
import com.evolveum.midpoint.schema.holder.XPathSegment;
import com.evolveum.midpoint.schema.util.JAXBUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_1.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.GenericObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyReferenceListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ScriptsType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserType;

/**
 * 
 * @author lazyman
 * 
 */
@ContextConfiguration(locations = { "classpath:application-context-model.xml",
		"classpath:application-context-model-unit-test.xml", "classpath:application-context-task.xml" })
public class ControllerModifyObjectWithExclusionTest extends AbstractTestNGSpringContextTests  {

	private static final File TEST_FOLDER = new File("./src/test/resources/controller/modify");
	private static final Trace LOGGER = TraceManager.getTrace(ControllerModifyObjectWithExclusionTest.class);
	@Autowired(required = true)
	private ModelController controller;
	@Autowired(required = true)
	private RepositoryService repository;
	@Autowired(required = true)
	private ProvisioningService provisioning;

	@BeforeMethod
	public void before() {
		Mockito.reset(repository, provisioning);
	}

	@Test(expectedExceptions = IllegalArgumentException.class)
	public void nullChange() throws Exception {
		controller.modifyObjectWithExclusion(null, null, null);
	}

	@Test(expectedExceptions = IllegalArgumentException.class)
	public void nullChangeOid() throws Exception {
		controller.modifyObjectWithExclusion(new ObjectModificationType(), null, null);
	}

	@Test(expectedExceptions = IllegalArgumentException.class)
	public void emptyChangeOid() throws Exception {
		ObjectModificationType change = new ObjectModificationType();
		change.setOid("");
		controller.modifyObjectWithExclusion(change, null, null);
	}

	@Test(expectedExceptions = IllegalArgumentException.class)
	public void nullResult() throws Exception {
		ObjectModificationType change = new ObjectModificationType();
		change.setOid("1");
		controller.modifyObjectWithExclusion(change, null, null);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void disableUser() throws Exception {
		UserType user = ((JAXBElement<UserType>) JAXBUtil.unmarshal(new File(TEST_FOLDER, "user.xml")))
				.getValue();
		AccountShadowType account = ((JAXBElement<AccountShadowType>) JAXBUtil.unmarshal(new File(
				TEST_FOLDER, "account.xml"))).getValue();
		GenericObjectType object = ((JAXBElement<GenericObjectType>) JAXBUtil.unmarshal(new File(TEST_FOLDER,
				"../../generic-object-my-config.xml"))).getValue();

		ObjectModificationType change = ((JAXBElement<ObjectModificationType>) JAXBUtil.unmarshal(new File(
				TEST_FOLDER, "change.xml"))).getValue();

		when(
				repository.getObject(eq(user.getOid()), any(PropertyReferenceListType.class),
						any(OperationResult.class))).thenReturn(user);
		when(
				repository.getObject(eq(object.getOid()), any(PropertyReferenceListType.class),
						any(OperationResult.class))).thenReturn(object);
		when(
				repository.getObject(eq(account.getOid()), any(PropertyReferenceListType.class),
						any(OperationResult.class))).thenReturn(account);
		when(
				provisioning.getObject(eq(account.getOid()), any(PropertyReferenceListType.class),
						any(OperationResult.class))).thenReturn(account);
		when(
				provisioning.getObject(eq(account.getResource().getOid()),
						any(PropertyReferenceListType.class), any(OperationResult.class))).thenReturn(
				account.getResource());

		doAnswer(new Answer<Void>() {
			@Override
			public Void answer(InvocationOnMock invocation) throws Throwable {
				ObjectModificationType modification = (ObjectModificationType) invocation.getArguments()[0];
				assertEquals("358bba7d-1176-45cf-8f32-b92125ea6fb1", modification.getOid());
				assertEquals(8, modification.getPropertyModification().size());
				assertActivation(modification);

				return null;
			}
		}).when(provisioning).modifyObject(any(ObjectModificationType.class), any(ScriptsType.class),
				any(OperationResult.class));

		OperationResult result = new OperationResult("disableUser");
		try {
			controller.modifyObject(change, result);
		} finally {
			LOGGER.debug(result.dump());
		}

		verify(provisioning).modifyObject(any(ObjectModificationType.class), any(ScriptsType.class),
				any(OperationResult.class));
	}

	private void assertActivation(ObjectModificationType modification) {
		boolean foundActivation = false;
		for (PropertyModificationType property : modification.getPropertyModification()) {
			XPathHolder xpath = new XPathHolder(property.getPath());
			List<XPathSegment> segments = xpath.toSegments();
			if (segments == null || segments.isEmpty() || segments.size() > 1) {
				continue;
			}

			if (!SchemaConstants.ACTIVATION.equals(segments.get(0).getQName())) {
				continue;
			}

			Element element = property.getValue().getAny().get(0);
			assertEquals("false", element.getTextContent());
			foundActivation = true;
			break;
		}

		if (!foundActivation) {
			Assert.fail("Activation property modification was not found.");
		}
	}
}

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
package com.evolveum.midpoint.model.sync.action;

import static org.testng.AssertJUnit.assertNotNull;
import org.testng.annotations.Test;
import org.testng.annotations.BeforeMethod;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;

import javax.xml.bind.JAXBElement;

import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.test.context.ContextConfiguration;

import com.evolveum.midpoint.api.logging.Trace;
import com.evolveum.midpoint.common.jaxb.JAXBUtil;
import com.evolveum.midpoint.common.result.OperationResult;
import com.evolveum.midpoint.logging.TraceManager;
import com.evolveum.midpoint.model.sync.SynchronizationException;
import com.evolveum.midpoint.model.test.util.ModelTUtil;
import com.evolveum.midpoint.schema.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.xml.ns._public.common.common_1.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectChangeAdditionType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyReferenceListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceObjectShadowChangeDescriptionType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceObjectShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ScriptsType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.SynchronizationSituationType;

/**
 * 
 * @author lazyman
 * 
 */
@ContextConfiguration(locations = { "classpath:application-context-model.xml",
		"classpath:application-context-model-unit-test.xml", 
		"classpath:application-context-task.xml" })
public class AddAccountActionTest extends BaseActionTest {

	private static final File TEST_FOLDER = new File("./src/test/resources/sync/action/account");
	private static final Trace LOGGER = TraceManager.getTrace(AddAccountActionTest.class);

	@BeforeMethod
	public void before() {
		Mockito.reset(provisioning, repository);
		before(new AddAccountAction());
	}

	@Test
	@SuppressWarnings("unchecked")
	public void nonAccountShadow() throws Exception {
		ResourceObjectShadowChangeDescriptionType change = ((JAXBElement<ResourceObjectShadowChangeDescriptionType>) JAXBUtil
				.unmarshal(new File(TEST_FOLDER, "group-change.xml"))).getValue();
		OperationResult result = new OperationResult("Add Account Action Test");

		String userOid = ModelTUtil.mockUser(repository, new File(TEST_FOLDER, "user.xml"), null);

		try {
			ObjectChangeAdditionType addition = (ObjectChangeAdditionType) change.getObjectChange();
			action.executeChanges(userOid, change, SynchronizationSituationType.CONFIRMED,
					(ResourceObjectShadowType) addition.getObject(), result);
		} finally {
			LOGGER.debug(result.dump());
		}

		verify(provisioning, times(0)).addObject(any(AccountShadowType.class), any(ScriptsType.class),
				any(OperationResult.class));
	}

	@SuppressWarnings("unchecked")
	@Test(expectedExceptions = SynchronizationException.class)
	public void accountExists() throws Exception {
		ResourceObjectShadowChangeDescriptionType change = ((JAXBElement<ResourceObjectShadowChangeDescriptionType>) JAXBUtil
				.unmarshal(new File(TEST_FOLDER, "../user/existing-user-change.xml"))).getValue();

		ObjectChangeAdditionType addition = (ObjectChangeAdditionType) change.getObjectChange();

		when(
				provisioning.addObject(any(AccountShadowType.class), any(ScriptsType.class),
						any(OperationResult.class))).thenThrow(
				new ObjectAlreadyExistsException("resource object shadow not found."));
		assertNotNull(change.getResource());
		when(
				provisioning.getObject(eq(change.getResource().getOid()),
						any(PropertyReferenceListType.class), any(OperationResult.class))).thenReturn(
				change.getResource());

		OperationResult result = new OperationResult("Add Account Action Test");
		try {
			action.executeChanges(null, change, SynchronizationSituationType.CONFIRMED,
					(ResourceObjectShadowType) addition.getObject(), result);
		} finally {
			LOGGER.debug(result.dump());
		}

		verify(provisioning, times(1)).addObject(any(AccountShadowType.class), any(ScriptsType.class),
				any(OperationResult.class));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void correctAdd() throws Exception {
		ResourceObjectShadowChangeDescriptionType change = ((JAXBElement<ResourceObjectShadowChangeDescriptionType>) JAXBUtil
				.unmarshal(new File(TEST_FOLDER, "../user/existing-user-change.xml"))).getValue();

		ObjectChangeAdditionType addition = (ObjectChangeAdditionType) change.getObjectChange();

		when(
				provisioning.addObject(any(AccountShadowType.class), any(ScriptsType.class),
						any(OperationResult.class))).thenAnswer(new Answer<String>() {
			@Override
			public String answer(InvocationOnMock invocation) throws Throwable {
				AccountShadowType account = (AccountShadowType) invocation.getArguments()[0];

				// TODO: test added account

				return "1";
			}
		});
		assertNotNull(change.getResource());
		when(
				provisioning.getObject(eq(change.getResource().getOid()),
						any(PropertyReferenceListType.class), any(OperationResult.class))).thenReturn(
				change.getResource());

		OperationResult result = new OperationResult("Add Account Action Test");
		try {
			action.executeChanges(null, change, SynchronizationSituationType.CONFIRMED,
					(ResourceObjectShadowType) addition.getObject(), result);
		} finally {
			LOGGER.debug(result.dump());
		}

		verify(provisioning, times(1)).addObject(any(AccountShadowType.class), any(ScriptsType.class),
				any(OperationResult.class));
	}
}

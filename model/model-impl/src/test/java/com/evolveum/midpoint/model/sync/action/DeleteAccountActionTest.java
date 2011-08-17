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

import org.testng.annotations.Test;
import org.testng.annotations.BeforeMethod;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.*;

import java.io.File;

import javax.xml.bind.JAXBElement;

import org.mockito.Mockito;
import org.springframework.test.context.ContextConfiguration;

import com.evolveum.midpoint.api.logging.Trace;
import com.evolveum.midpoint.common.jaxb.JAXBUtil;
import com.evolveum.midpoint.common.result.OperationResult;
import com.evolveum.midpoint.logging.TraceManager;
import com.evolveum.midpoint.model.sync.SynchronizationException;
import com.evolveum.midpoint.schema.exception.ObjectNotFoundException;
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
		"classpath:application-context-model-unit-test.xml", "classpath:application-context-task.xml" })
public class DeleteAccountActionTest extends BaseActionTest {

	private static final File TEST_FOLDER = new File("./src/test/resources/sync/action/account");
	private static final Trace LOGGER = TraceManager.getTrace(DeleteAccountActionTest.class);

	@BeforeMethod
	public void before() {
		Mockito.reset(provisioning, repository);
		before(new DeleteAccountAction());
	}

	@SuppressWarnings("unchecked")
	@Test(expectedExceptions = SynchronizationException.class)
	public void problemInProvisioning() throws Exception {
		ResourceObjectShadowChangeDescriptionType change = ((JAXBElement<ResourceObjectShadowChangeDescriptionType>) JAXBUtil
				.unmarshal(new File(TEST_FOLDER, "../user/existing-user-change.xml"))).getValue();

		ObjectChangeAdditionType addition = (ObjectChangeAdditionType) change.getObjectChange();
		String shadowOid = addition.getObject().getOid();

		when(
				repository.getObject(eq(shadowOid), any(PropertyReferenceListType.class),
						any(OperationResult.class))).thenThrow(
				new ObjectNotFoundException("resource object shadow not found."));

		OperationResult result = new OperationResult("Delete Account Action Test");
		try {
			action.executeChanges(null, change, SynchronizationSituationType.CONFIRMED,
					(ResourceObjectShadowType) addition.getObject(), result);
		} finally {
			LOGGER.debug(result.dump());
		}

		verify(provisioning, times(1)).deleteObject(eq(shadowOid), any(ScriptsType.class),
				any(OperationResult.class));
	}

	@SuppressWarnings("unchecked")
	@Test(expectedExceptions = SynchronizationException.class)
	public void notExistingResourceForScripts() throws Exception {
		ResourceObjectShadowChangeDescriptionType change = ((JAXBElement<ResourceObjectShadowChangeDescriptionType>) JAXBUtil
				.unmarshal(new File(TEST_FOLDER, "../user/existing-user-change.xml"))).getValue();

		ObjectChangeAdditionType addition = (ObjectChangeAdditionType) change.getObjectChange();
		String shadowOid = addition.getObject().getOid();

		when(
				repository.getObject(eq(shadowOid), any(PropertyReferenceListType.class),
						any(OperationResult.class))).thenReturn(addition.getObject());

		OperationResult result = new OperationResult("Delete Account Action Test");
		try {
			action.executeChanges(null, change, SynchronizationSituationType.CONFIRMED,
					(ResourceObjectShadowType) addition.getObject(), result);
		} finally {
			LOGGER.debug(result.dump());
		}
		verify(provisioning, times(1)).deleteObject(eq(shadowOid), any(ScriptsType.class),
				any(OperationResult.class));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void correctDeleteAccount() throws Exception {
		ResourceObjectShadowChangeDescriptionType change = ((JAXBElement<ResourceObjectShadowChangeDescriptionType>) JAXBUtil
				.unmarshal(new File(TEST_FOLDER, "../user/existing-user-change.xml"))).getValue();

		ObjectChangeAdditionType addition = (ObjectChangeAdditionType) change.getObjectChange();
		String shadowOid = addition.getObject().getOid();

		when(
				repository.getObject(eq(shadowOid), any(PropertyReferenceListType.class),
						any(OperationResult.class))).thenReturn(addition.getObject());
		when(
				repository.getObject(eq("c0c010c0-d34d-b44f-f11d-333222111111"), any(PropertyReferenceListType.class),
						any(OperationResult.class))).thenReturn(change.getResource());
		when(							  
				provisioning.getObject(eq("c0c010c0-d34d-b44f-f11d-333222111111"),
						any(PropertyReferenceListType.class), any(OperationResult.class))).thenReturn(
				change.getResource());

		OperationResult result = new OperationResult("Delete Account Action Test");
		try {
			action.executeChanges(null, change, SynchronizationSituationType.CONFIRMED,
					(ResourceObjectShadowType) addition.getObject(), result);
		} finally {
			LOGGER.debug(result.dump());
		}
		verify(provisioning, times(1)).deleteObject(eq(shadowOid), any(ScriptsType.class),
				any(OperationResult.class));
	}
}

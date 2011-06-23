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
package com.evolveum.midpoint.model.action;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.List;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.evolveum.midpoint.common.jaxb.JAXBUtil;
import com.evolveum.midpoint.common.result.OperationResult;
import com.evolveum.midpoint.model.test.util.RepositoryUtils;
import com.evolveum.midpoint.provisioning.objects.ResourceObject;
import com.evolveum.midpoint.provisioning.schema.ResourceSchema;
import com.evolveum.midpoint.provisioning.schema.util.ObjectValueWriter;
import com.evolveum.midpoint.provisioning.service.BaseResourceIntegration;
import com.evolveum.midpoint.provisioning.service.ResourceAccessInterface;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.xml.ns._public.common.common_1.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectChangeModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.OperationalResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyReferenceListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceObjectShadowChangeDescriptionType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceObjectShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserType;
import com.evolveum.midpoint.xml.ns._public.provisioning.resource_object_change_listener_1.ResourceObjectChangeListenerPortType;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:application-context-model.xml",
		"classpath:application-context-repository.xml", "classpath:application-context-provisioning.xml" })
public class ModifyUserActionTest {

	@Autowired(required = true)
	private ResourceObjectChangeListenerPortType resourceObjectChangeService;
	@Autowired(required = true)
	private RepositoryService repositoryService;
	@Autowired(required = true)
	private ResourceAccessInterface rai;

	@SuppressWarnings("unchecked")
	private ResourceObjectShadowChangeDescriptionType createChangeDescription(String file)
			throws JAXBException {
		ResourceObjectShadowChangeDescriptionType change = ((JAXBElement<ResourceObjectShadowChangeDescriptionType>) JAXBUtil
				.unmarshal(new File(file))).getValue();
		return change;
	}

	private ResourceObject createSampleResourceObject(ResourceSchema schema, ResourceObjectShadowType shadow)
			throws ParserConfigurationException {
		ObjectValueWriter valueWriter = ObjectValueWriter.getInstance();
		return valueWriter.buildResourceObject(shadow, schema);
	}

	@Test
	public void testModifyUserAction() throws Exception {

		final String resourceOid = "87654321-d34d-b33f-f00d-333222111111";
		final String userOid = "87654321-d34d-b33f-f00d-987987987987";
		final String accountOid = "87654321-d34d-b44f-f11d-333222111111";

		// UserType addedUser = null;

		try {
			// create additional change
			ResourceObjectShadowChangeDescriptionType change = createChangeDescription("src/test/resources/account-change-modify-user.xml");
			// adding objects to repo
			final ResourceType resourceType = (ResourceType) RepositoryUtils.addObjectToRepo(
					repositoryService, change.getResource());
			final AccountShadowType accountType = (AccountShadowType) RepositoryUtils.addObjectToRepo(
					repositoryService, change.getShadow());
			UserType userType = (UserType) RepositoryUtils.addObjectToRepo(repositoryService,
					"src/test/resources/user-modify-action.xml");

			// setting resource for ResourceObjectShadowType
			ObjectModificationType objChange = ((ObjectChangeModificationType) change.getObjectChange())
					.getObjectModification();

			assertNotNull(resourceType);
			// setup provisioning mock
			BaseResourceIntegration bri = new BaseResourceIntegration(resourceType);
			ResourceObject ro = createSampleResourceObject(bri.getSchema(), accountType);

			when(rai.get(any(OperationalResultType.class), any(ResourceObject.class))).thenReturn(ro);

			when(rai.getConnector()).thenReturn(bri);

			resourceObjectChangeService.notifyChange(change);

			UserType changedUser = (UserType) repositoryService.getObject(userOid,
					new PropertyReferenceListType(), new OperationResult("Get Object"));
			List<ObjectReferenceType> accountRefs = changedUser.getAccountRef();
			assertNotNull(changedUser);
			assertEquals(accountOid, accountRefs.get(0).getOid());

			assertEquals("First", changedUser.getFamilyName());

		} finally {
			// cleanup repo
			RepositoryUtils.deleteObject(repositoryService, accountOid);
			RepositoryUtils.deleteObject(repositoryService, resourceOid);
			RepositoryUtils.deleteObject(repositoryService, userOid);
		}
	}
}

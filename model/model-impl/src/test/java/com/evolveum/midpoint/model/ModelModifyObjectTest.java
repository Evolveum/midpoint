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
import static org.mockito.Mockito.when;

import java.io.File;

import javax.xml.bind.JAXBException;
import javax.xml.ws.Holder;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.OperationResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyModificationTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyReferenceListType;
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
public class ModelModifyObjectTest {

	private static final File TEST_FOLDER = new File("./src/test/resources/service/model/modify");
	@Autowired(required = true)
	ModelPortType modelService;
	@Autowired(required = true)
	ProvisioningPortType provisioningService;
	@Autowired(required = true)
	RepositoryPortType repositoryService;

	// @Autowired(required = true)
	// SchemaHandling schemaHandling;

	@Before
	public void before() {
		Mockito.reset(provisioningService, repositoryService);
	}

	@Test(expected = FaultMessage.class)
	public void nullChange() throws FaultMessage {
		modelService.modifyObject(null, new Holder<OperationResultType>(new OperationResultType()));
		fail("Illegal argument excetion must be thrown");
	}

	@Test(expected = FaultMessage.class)
	public void nonExistingUid() throws FaultMessage,
			com.evolveum.midpoint.xml.ns._public.repository.repository_1.FaultMessage {

		final String oid = "1";
		ObjectModificationType modification = new ObjectModificationType();
		PropertyModificationType mod1 = new PropertyModificationType();
		mod1.setModificationType(PropertyModificationTypeType.add);
		mod1.setValue(new PropertyModificationType.Value());

		modification.getPropertyModification().add(mod1);
		modification.setOid(oid);

		when(repositoryService.getObject(eq(oid), any(PropertyReferenceListType.class))).thenThrow(
				new com.evolveum.midpoint.xml.ns._public.repository.repository_1.FaultMessage("Oid '" + oid
						+ "' not found.", new ObjectNotFoundFaultType()));
		try {
			modelService.modifyObject(modification,
					new Holder<OperationResultType>(new OperationResultType()));
		} catch (FaultMessage ex) {
			if (!(ex.getFaultInfo() instanceof ObjectNotFoundFaultType)) {
				fail("Bad exceptiong fault info was thrown.");
			}

			throw ex;
		}
	}

	@Ignore
	@Test
	@SuppressWarnings("unchecked")
	public void correctModifyUser() throws JAXBException {
		// final String oid = "1";
		// ObjectModificationType modification =
		// ((JAXBElement<ObjectModificationType>) JAXBUtil.unmarshal(
		// new File(TEST_FOLDER, "modify-user-correct.xml"))).getValue();
		fail("not implemented yet.");
	}
}

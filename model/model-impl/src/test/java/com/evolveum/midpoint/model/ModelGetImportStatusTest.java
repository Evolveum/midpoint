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
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;

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

import com.evolveum.midpoint.common.jaxb.JAXBUtil;
import com.evolveum.midpoint.common.result.OperationResult;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.exception.ObjectNotFoundException;
import com.evolveum.midpoint.xml.ns._public.common.common_1.OperationResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.TaskStatusType;
import com.evolveum.midpoint.xml.ns._public.model.model_1.FaultMessage;
import com.evolveum.midpoint.xml.ns._public.model.model_1.ModelPortType;

/**
 * 
 * @author lazyman
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:application-context-model.xml",
		"classpath:application-context-model-unit-test.xml" })
public class ModelGetImportStatusTest {

	private static final File TEST_FOLDER = new File("./src/test/resources/service/model/import");
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
	public void nullResourceOid() throws FaultMessage {
		modelService.getImportStatus(null, new Holder<OperationResultType>(new OperationResultType()));
		fail("Illegal argument exception was not thrown.");
	}

	@Test(expected = FaultMessage.class)
	public void emptyResourceOid() throws FaultMessage {
		modelService.getImportStatus("", new Holder<OperationResultType>(new OperationResultType()));
		fail("Illegal argument exception was not thrown.");
	}

	@Test(expected = FaultMessage.class)
	public void nonExistingResourceOid() throws FaultMessage, ObjectNotFoundException {

		final String nonExistingUid = "1";
		when(provisioningService.getImportStatus(eq(nonExistingUid), any(OperationResult.class))).thenThrow(
				new ObjectNotFoundException("Resource with uid '" + nonExistingUid + "' doesn't exist."));

		modelService.getImportStatus(nonExistingUid, new Holder<OperationResultType>(
				new OperationResultType()));
		fail("Fault exception was not thrown.");
	}

	@Test
	@SuppressWarnings("unchecked")
	public void correctResourceOid() throws FaultMessage, JAXBException, ObjectNotFoundException {
		final String resourceOid = "abababab-abab-abab-abab-000000000001";
		final TaskStatusType expectedTaskStatus = ((JAXBElement<TaskStatusType>) JAXBUtil.unmarshal(new File(
				TEST_FOLDER, "import-status-correct.xml"))).getValue();
		when(provisioningService.getImportStatus(eq(resourceOid), any(OperationResult.class))).thenReturn(
				expectedTaskStatus);

		final TaskStatusType taskStatus = modelService.getImportStatus(resourceOid,
				new Holder<OperationResultType>(new OperationResultType()));
		assertNotNull(taskStatus);
		assertEquals(expectedTaskStatus.getName(), taskStatus.getName());
		assertEquals(expectedTaskStatus.getFinishTime(), taskStatus.getFinishTime());
		assertEquals(expectedTaskStatus.getLastStatus(), taskStatus.getLastStatus());
		assertEquals(expectedTaskStatus.getLaunchTime(), taskStatus.getLaunchTime());
		assertEquals(expectedTaskStatus.getNumberOfErrors(), taskStatus.getNumberOfErrors());
		assertEquals(expectedTaskStatus.getProgress(), taskStatus.getProgress());

		verify(provisioningService, atLeastOnce()).getImportStatus(eq(resourceOid),
				any(OperationResult.class));
	}
}

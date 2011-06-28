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
package com.evolveum.midpoint.model.test.util;

import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import java.io.File;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;

import com.evolveum.midpoint.common.jaxb.JAXBUtil;
import com.evolveum.midpoint.common.result.OperationResult;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.exception.ObjectNotFoundException;
import com.evolveum.midpoint.schema.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyReferenceListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.SystemConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.SystemObjectsType;
import com.evolveum.midpoint.xml.ns._public.common.fault_1.IllegalArgumentFaultType;
import com.evolveum.midpoint.xml.ns._public.model.model_1.FaultMessage;

/**
 * 
 * @author lazyman
 * 
 */
public class ModelServiceUtil {

	@SuppressWarnings("unchecked")
	public static void mockGetSystemConfiguration(RepositoryService repository, File file)
			throws JAXBException, ObjectNotFoundException, SchemaException {
		SystemConfigurationType systemConfiguration = ((JAXBElement<SystemConfigurationType>) JAXBUtil
				.unmarshal(file)).getValue();

		when(
				repository.getObject(eq(SystemObjectsType.SYSTEM_CONFIGURATION.value()),
						any(PropertyReferenceListType.class), any(OperationResult.class))).thenReturn(
				systemConfiguration);
	}

	public static void assertIllegalArgumentFault(FaultMessage ex) throws FaultMessage {
		if (!(ex.getFaultInfo() instanceof IllegalArgumentFaultType)) {
			fail("not illegal argument fault.");
		}
		throw ex;
	}

	public static ObjectType addObjectToRepo(RepositoryService repositoryService, ObjectType object)
			throws Exception {
		repositoryService.addObject(object, new OperationResult("Add Object"));
		return object;
	}

	public static void deleteObject(RepositoryService repositoryService, String oid) {
		try {
			repositoryService.deleteObject(oid, new OperationResult("Delete Object"));
		} catch (Exception e) {
		}
	}

	@SuppressWarnings("unchecked")
	public static ObjectType addObjectToRepo(RepositoryService repositoryService, String fileString)
			throws Exception {
		ObjectType object = ((JAXBElement<ObjectType>) JAXBUtil.unmarshal(new File(fileString))).getValue();
		repositoryService.addObject(object, new OperationResult("Add Object"));
		return object;
	}
}

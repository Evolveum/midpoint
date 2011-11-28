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

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import java.io.File;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;

import org.testng.Assert;

import org.apache.commons.lang.StringUtils;

import com.evolveum.midpoint.common.patch.PatchXml;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.exception.ObjectNotFoundException;
import com.evolveum.midpoint.schema.exception.SchemaException;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.JAXBUtil;
import com.evolveum.midpoint.util.patch.PatchException;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyReferenceListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.SystemConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.SystemObjectsType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserType;
import com.evolveum.midpoint.xml.ns._public.common.fault_1.IllegalArgumentFaultType;
import com.evolveum.midpoint.xml.ns._public.common.fault_1.ObjectNotFoundFaultType;
import com.evolveum.midpoint.xml.ns._public.common.fault_1_wsdl.FaultMessage;

/**
 * 
 * @author lazyman
 * 
 */
public class ModelTUtil {

	@SuppressWarnings("unchecked")
	public static <T extends ObjectType> T patchXml(ObjectModificationType changes, ObjectType object,
			Class<T> clazz) throws PatchException, JAXBException {

		PatchXml patchXml = new PatchXml();
		String xml = patchXml.applyDifferences(changes, object);
		return ((JAXBElement<T>) JAXBUtil.unmarshal(xml)).getValue();
	}

	@SuppressWarnings("unchecked")
	public static void mockGetSystemConfiguration(RepositoryService repository, File file)
			throws JAXBException, ObjectNotFoundException, SchemaException {
		SystemConfigurationType systemConfiguration = ((JAXBElement<SystemConfigurationType>) JAXBUtil
				.unmarshal(file)).getValue();

		when(
				repository.getObject(eq(SystemConfigurationType.class),
						eq(SystemObjectsType.SYSTEM_CONFIGURATION.value()),
						any(PropertyReferenceListType.class), any(OperationResult.class))).thenReturn(
				systemConfiguration);
	}

	public static void assertObjectNotFoundFault(FaultMessage ex) throws FaultMessage {
		if (!(ex.getFaultInfo() instanceof ObjectNotFoundFaultType)) {
			Assert.fail("not object not found fault.");
		}
		throw ex;
	}

	public static void assertIllegalArgumentFault(FaultMessage ex) throws FaultMessage {
		if (!(ex.getFaultInfo() instanceof IllegalArgumentFaultType)) {
			Assert.fail("not illegal argument fault.");
		}
		throw ex;
	}

	public static ObjectType addObjectToRepo(RepositoryService repositoryService, ObjectType object)
			throws Exception {
		repositoryService.addObject(object, new OperationResult("Add Object"));
		return object;
	}

	public static <T extends ObjectType> void deleteObject(RepositoryService repositoryService, Class<T> type, String oid) {
		if (StringUtils.isEmpty(oid)) {
			return;
		}
		try {
			repositoryService.deleteObject(type, oid, new OperationResult("Delete Object"));
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

	/**
	 * 
	 * @param repository
	 * @param file
	 *            - user to be found
	 * @param userOid
	 *            - if file == null then userOid when repo should throw
	 *            ObjectNotFoundException
	 * @return
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	public static String mockUser(RepositoryService repository, File file, String userOid) throws Exception {
		String userOidExpected = userOid;
		if (file != null) {
			UserType user = ((JAXBElement<UserType>) JAXBUtil.unmarshal(file)).getValue();
			userOidExpected = user.getOid();
			when(
					repository.getObject(any(Class.class), eq(user.getOid()),
							any(PropertyReferenceListType.class), any(OperationResult.class))).thenReturn(
					user);
		} else {
			when(
					repository.getObject(any(Class.class), eq(userOid),
							any(PropertyReferenceListType.class), any(OperationResult.class))).thenThrow(
					new ObjectNotFoundException("user not found."));
		}

		return userOidExpected;
	}
}

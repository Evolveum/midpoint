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
package com.evolveum.midpoint.model.util;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileNotFoundException;

import javax.xml.bind.JAXBException;

import org.testng.Assert;

import org.apache.commons.lang.StringUtils;

import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.api_types_2.ObjectModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.SystemConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.SystemObjectsType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;
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
	public static <T extends ObjectType> T patchXml(ObjectModificationType changes, T object,
			Class<T> clazz) throws SchemaException {

		ObjectDelta<T> objectDelta = DeltaConvertor.createObjectDelta(changes, clazz, PrismTestUtil.getPrismContext());
		objectDelta.applyTo(object.asPrismObject());
		return object;
	}

	@SuppressWarnings("unchecked")
	public static void mockGetSystemConfiguration(RepositoryService repository, File file)
			throws JAXBException, ObjectNotFoundException, SchemaException, FileNotFoundException {
		SystemConfigurationType systemConfiguration = PrismTestUtil.unmarshalObject(file, SystemConfigurationType.class);

		when(
				repository.getObject(eq(SystemConfigurationType.class),
						eq(SystemObjectsType.SYSTEM_CONFIGURATION.value()),
						any(OperationResult.class))).thenReturn(
				systemConfiguration.asPrismObject());
	}

	public static void assertObjectNotFoundFault(FaultMessage ex) throws FaultMessage {
		if (!(ex.getFaultInfo() instanceof ObjectNotFoundFaultType)) {
			System.err.println("Assertion error: not object not found fault");
			ex.printStackTrace();
			Assert.fail("not object not found fault, it is: "+ex.getFaultInfo());
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
		repositoryService.addObject(object.asPrismObject(), new OperationResult("Add Object"));
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
		ObjectType object = PrismTestUtil.unmarshalObject(new File(fileString), ObjectType.class);
		repositoryService.addObject(object.asPrismObject(), new OperationResult("Add Object"));
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
			UserType user = PrismTestUtil.unmarshalObject(file, UserType.class);
			userOidExpected = user.getOid();
			when(
					repository.getObject(any(Class.class), eq(user.getOid()),
							any(OperationResult.class))).thenReturn(
					user.asPrismObject());
		} else {
			when(
					repository.getObject(any(Class.class), eq(userOid),
							any(OperationResult.class))).thenThrow(
					new ObjectNotFoundException("user not found."));
		}

		return userOidExpected;
	}
}

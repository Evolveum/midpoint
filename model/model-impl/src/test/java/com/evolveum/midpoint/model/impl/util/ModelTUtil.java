/*
 * Copyright (c) 2010-2013 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evolveum.midpoint.model.impl.util;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import javax.xml.bind.JAXBException;

import com.evolveum.midpoint.prism.PrismObject;
import org.testng.Assert;
import org.apache.commons.lang.StringUtils;

import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.ObjectModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemObjectsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.midpoint.xml.ns._public.common.fault_3.FaultMessage;
import com.evolveum.midpoint.xml.ns._public.common.fault_3.IllegalArgumentFaultType;
import com.evolveum.midpoint.xml.ns._public.common.fault_3.ObjectNotFoundFaultType;

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
		objectDelta.applyTo((PrismObject<T>) object.asPrismObject());
		return object;
	}

	@SuppressWarnings("unchecked")
	public static void mockGetSystemConfiguration(RepositoryService repository, File file)
            throws JAXBException, ObjectNotFoundException, SchemaException, IOException {
		SystemConfigurationType systemConfiguration = (SystemConfigurationType) PrismTestUtil.parseObject(file).asObjectable();

		when(
				repository.getObject(eq(SystemConfigurationType.class),
						eq(SystemObjectsType.SYSTEM_CONFIGURATION.value()),
						any(Collection.class),
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
			IntegrationTestTools.display("Unexpected exception in fault", ex.getFaultInfo());
			Assert.fail("not illegal argument fault. Was: "+ex.getFaultInfo());
		}
		throw ex;
	}

	public static ObjectType addObjectToRepo(RepositoryService repositoryService, ObjectType object)
			throws Exception {
		repositoryService.addObject(object.asPrismObject(), null, new OperationResult("Add Object"));
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
		ObjectType object = (ObjectType) PrismTestUtil.parseObject(new File(fileString)).asObjectable();
		repositoryService.addObject(object.asPrismObject(), null, new OperationResult("Add Object"));
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
			UserType user = (UserType) PrismTestUtil.parseObject(file).asObjectable();
			userOidExpected = user.getOid();
			when(
					repository.getObject(any(Class.class), eq(user.getOid()),
							any(Collection.class),
							any(OperationResult.class))).thenReturn(
					user.asPrismObject());
		} else {
			when(
					repository.getObject(any(Class.class), eq(userOid),
							any(Collection.class),
							any(OperationResult.class))).thenThrow(
					new ObjectNotFoundException("user not found."));
		}

		return userOidExpected;
	}
}

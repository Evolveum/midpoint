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

package com.evolveum.midpoint.web.model.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.xml.namespace.QName;
import javax.xml.ws.Holder;

import org.apache.commons.lang.Validate;

import com.evolveum.midpoint.api.logging.LoggingUtils;
import com.evolveum.midpoint.api.logging.Trace;
import com.evolveum.midpoint.common.result.OperationResult;
import com.evolveum.midpoint.logging.TraceManager;
import com.evolveum.midpoint.schema.ObjectTypes;
import com.evolveum.midpoint.web.model.ResourceManager;
import com.evolveum.midpoint.web.model.dto.ConnectorDto;
import com.evolveum.midpoint.web.model.dto.PropertyChange;
import com.evolveum.midpoint.web.model.dto.ResourceDto;
import com.evolveum.midpoint.web.model.dto.ResourceObjectShadowDto;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ConnectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.OperationResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PagingType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyReferenceListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceObjectShadowListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceObjectShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.TaskStatusType;
import com.evolveum.midpoint.xml.ns._public.model.model_1.FaultMessage;

/**
 * 
 * @author katuska
 */
public class ResourceTypeManager extends ResourceManager {

	private static final long serialVersionUID = 8238616310118713517L;
	private static final Trace LOGGER = TraceManager.getTrace(ResourceTypeManager.class);
	private Class<? extends ResourceDto> constructResourceType;

	public ResourceTypeManager(Class<? extends ResourceDto> constructResourceType) {
		this.constructResourceType = constructResourceType;
	}

	@Override
	public ResourceDto create() {
		ResourceDto resource = null;
		try {
			resource = constructResourceType.newInstance();
			// resource.setXmlObject(new ResourceType());
		} catch (Exception ex) {
			throw new IllegalStateException("Couldn't create instance of '" + constructResourceType + "'.");
		}

		return resource;
	}

	@Override
	public Set<PropertyChange> submit(ResourceDto changedObject) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public <T extends ResourceObjectShadowType> List<ResourceObjectShadowDto<T>> listObjectShadows(
			String oid, Class<T> resourceObjectShadowType) {
		Validate.notNull(oid);
		try {
			ResourceObjectShadowListType resourceObjectShadowListType = getModel().listResourceObjectShadows(
					oid, resourceObjectShadowType.getName(),
					new Holder<OperationResultType>(new OperationResultType()));
			List<ResourceObjectShadowDto<T>> resourceObjectShadowDtoList = new ArrayList<ResourceObjectShadowDto<T>>();
			for (ResourceObjectShadowType resourceObjectShadow : resourceObjectShadowListType.getObject()) {
				ResourceObjectShadowDto<T> resourceObjectShadowDto = new ResourceObjectShadowDto<T>(
						resourceObjectShadow);
				resourceObjectShadowDtoList.add(resourceObjectShadowDto);
			}
			return resourceObjectShadowDtoList;
		} catch (Exception ex) {
			LOGGER.error("Delete user failed for oid = {}", oid);
			LOGGER.error("Exception was: ", ex);
			return null;
		}

	}

	@Override
	public Collection<ResourceDto> list(PagingType paging) {
		return list(paging, ObjectTypes.RESOURCE);
	}

	@Override
	public OperationResult testConnection(String resourceOid) {
		Validate.notEmpty(resourceOid, "Resource oid must not be null or empty.");
		LOGGER.debug("Testing resource with oid {}.", new Object[] { resourceOid });

		OperationResult result = new OperationResult("Test Resource");
		Holder<OperationResultType> holder = new Holder<OperationResultType>(
				result.createOperationResultType());

		try {
			getModel().testResource(resourceOid, holder);

			result = OperationResult.createOperationResult(holder.value);
			result.recordSuccess();
		} catch (FaultMessage ex) {
			LoggingUtils.logException(LOGGER, "Couldn't test resource {}", ex, resourceOid);

			OperationResultType resultType = (ex.getFaultInfo() != null && ex.getFaultInfo()
					.getOperationResult() == null) ? holder.value : ex.getFaultInfo().getOperationResult();
			result = OperationResult.createOperationResult(resultType);
			result.recordFatalError(ex);
		} catch (Exception ex) {
			LoggingUtils.logException(LOGGER, "Couldn't test resource {}", ex, resourceOid);

			result = OperationResult.createOperationResult(holder.value);
			result.recordFatalError(ex);
		}

		printResults(LOGGER, result);
		LOGGER.error("***RESOURCE TEST CONNECTION IS BROKEN, WE'RE NOT USING RESOUCE "
				+ "TEST TYPE BUT OPERATION RESULT AS RETURN VALUE.***");
		return result;
	}

	@Override
	public void launchImportFromResource(String resourceOid, QName objectClass) {
		Validate.notEmpty(resourceOid, "Resource oid must not be null or empty.");
		Validate.notNull(objectClass, "Object class must not be null.");
		LOGGER.debug("Launching import from resource with oid {} and object class {}.", new Object[] {
				resourceOid, objectClass });

		OperationResult result = new OperationResult("Launch Import On Resource");
		Holder<OperationResultType> holder = new Holder<OperationResultType>(
				result.createOperationResultType());

		try {
			getModel().launchImportFromResource(resourceOid, objectClass, holder);

			result = OperationResult.createOperationResult(holder.value);
			result.recordSuccess();
		} catch (FaultMessage ex) {
			LoggingUtils.logException(LOGGER, "Couldn't launch import on resource {} and object class {}",
					ex, resourceOid, objectClass);

			OperationResultType resultType = (ex.getFaultInfo() != null && ex.getFaultInfo()
					.getOperationResult() == null) ? holder.value : ex.getFaultInfo().getOperationResult();
			result = OperationResult.createOperationResult(resultType);
			result.recordFatalError(ex);
		} catch (Exception ex) {
			LoggingUtils.logException(LOGGER, "Couldn't launch import on resource {} and object class {}",
					ex, resourceOid, objectClass);

			result = OperationResult.createOperationResult(holder.value);
			result.recordFatalError(ex);
		}

		printResults(LOGGER, result);
	}

	@Override
	public TaskStatusType getImportStatus(String resourceOid) {
		Validate.notEmpty(resourceOid, "Resource oid must not be null or empty.");
		LOGGER.debug("Getting import status for resource with oid {}", new Object[] { resourceOid });

		OperationResult result = new OperationResult("Get Import Status");
		Holder<OperationResultType> holder = new Holder<OperationResultType>(
				result.createOperationResultType());

		TaskStatusType task = null;
		try {
			getModel().getImportStatus(resourceOid, holder);
			result = OperationResult.createOperationResult(holder.value);
			result.recordSuccess();
		} catch (FaultMessage ex) {
			LoggingUtils.logException(LOGGER, "Couldn't get import status from resource {}", ex, resourceOid);

			OperationResultType resultType = (ex.getFaultInfo() != null && ex.getFaultInfo()
					.getOperationResult() == null) ? holder.value : ex.getFaultInfo().getOperationResult();
			result = OperationResult.createOperationResult(resultType);
			result.recordFatalError(ex);
		} catch (Exception ex) {
			LoggingUtils.logException(LOGGER, "Couldn't get import status from resource {}", ex, resourceOid);

			result = OperationResult.createOperationResult(holder.value);
			result.recordFatalError(ex);
		}

		printResults(LOGGER, result);

		return task;
	}

	@Override
	public Collection<ResourceObjectShadowDto<ResourceObjectShadowType>> listResourceObjects(
			String resourceOid, QName objectClass, PagingType paging) {
		Validate.notEmpty(resourceOid, "Resource oid must not be null or empty.");
		Validate.notNull(objectClass, "Object class must not be null.");
		Validate.notNull(paging, "Paging must not be null.");
		LOGGER.debug("Listing resource objects from resource with oid {} and object class {}.", new Object[] {
				resourceOid, objectClass });

		OperationResult result = new OperationResult("Launch Import On Resource");
		Holder<OperationResultType> holder = new Holder<OperationResultType>(
				result.createOperationResultType());

		Collection<ResourceObjectShadowDto<ResourceObjectShadowType>> collection = new ArrayList<ResourceObjectShadowDto<ResourceObjectShadowType>>();
		try {
			ObjectListType list = getModel().listResourceObjects(resourceOid, objectClass, paging, holder);
			if (list != null) {
				for (ObjectType objectType : list.getObject()) {
					collection.add(new ResourceObjectShadowDto<ResourceObjectShadowType>(
							(ResourceObjectShadowType) objectType));
				}
			}
			result = OperationResult.createOperationResult(holder.value);
			result.recordSuccess();
		} catch (FaultMessage ex) {
			LoggingUtils.logException(LOGGER, "Couldn't get import status from resource {}", ex, resourceOid);

			OperationResultType resultType = (ex.getFaultInfo() != null && ex.getFaultInfo()
					.getOperationResult() == null) ? holder.value : ex.getFaultInfo().getOperationResult();
			result = OperationResult.createOperationResult(resultType);
			result.recordFatalError(ex);
		} catch (Exception ex) {
			LoggingUtils.logException(LOGGER, "Couldn't get import status from resource {}", ex, resourceOid);

			result = OperationResult.createOperationResult(holder.value);
			result.recordFatalError(ex);
		}

		return collection;
	}

	@Override
	public ConnectorDto getConnector(String oid) {
		ConnectorType connector = get(oid, new PropertyReferenceListType(), ConnectorType.class);
		return new ConnectorDto(connector);
	}
}

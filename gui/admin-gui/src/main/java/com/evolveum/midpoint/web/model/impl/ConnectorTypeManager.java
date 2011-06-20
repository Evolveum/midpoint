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
package com.evolveum.midpoint.web.model.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.xml.ws.Holder;

import org.springframework.beans.factory.annotation.Autowired;

import com.evolveum.midpoint.api.logging.LoggingUtils;
import com.evolveum.midpoint.api.logging.Trace;
import com.evolveum.midpoint.common.result.OperationResult;
import com.evolveum.midpoint.logging.TraceManager;
import com.evolveum.midpoint.schema.ObjectTypes;
import com.evolveum.midpoint.schema.PagingTypeFactory;
import com.evolveum.midpoint.web.model.ObjectManager;
import com.evolveum.midpoint.web.model.WebModelException;
import com.evolveum.midpoint.web.model.dto.ConnectorDto;
import com.evolveum.midpoint.web.model.dto.PropertyAvailableValues;
import com.evolveum.midpoint.web.model.dto.PropertyChange;
import com.evolveum.midpoint.web.util.FacesUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ConnectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.OperationResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.OrderDirectionType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PagingType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyReferenceListType;
import com.evolveum.midpoint.xml.ns._public.model.model_1.FaultMessage;
import com.evolveum.midpoint.xml.ns._public.model.model_1.ModelPortType;

/**
 * 
 * @author lazyman
 * 
 */
public class ConnectorTypeManager implements ObjectManager<ConnectorDto> {

	private static final Trace LOGGER = TraceManager.getTrace(ConnectorTypeManager.class);
	@Autowired(required = true)
	private transient ModelPortType model;

	@Override
	public Collection<ConnectorDto> list() throws WebModelException {
		PagingType paging = PagingTypeFactory.createListAllPaging(OrderDirectionType.ASCENDING, "name");
		return list(paging);
	}

	@Override
	public String add(ConnectorDto newObject) throws WebModelException {
		throw new UnsupportedOperationException("Not supported for this type.");
	}

	@Override
	public Set<PropertyChange> submit(ConnectorDto changedObject) throws WebModelException {
		throw new UnsupportedOperationException("Not supported for this type.");
	}

	@Override
	public void delete(String oid) throws WebModelException {
		throw new UnsupportedOperationException("Not supported for this type.");
	}

	@Override
	public List<PropertyAvailableValues> getPropertyAvailableValues(String oid, List<String> properties) {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override
	public ConnectorDto get(String oid, PropertyReferenceListType resolve) throws WebModelException {
		LOGGER.debug("Getting connector with oid {}.", new Object[] { oid });

		OperationResult result = new OperationResult("Get Connector");
		Holder<OperationResultType> holder = new Holder<OperationResultType>(
				result.createOperationResultType());

		ConnectorDto connector = null;
		try {
			ObjectType object = model.getObject(oid, resolve, holder);
			connector = new ConnectorDto((ConnectorType) object);

			result = OperationResult.createOperationResult(holder.value);
			result.recordSuccess();
		} catch (FaultMessage ex) {
			LoggingUtils.logException(LOGGER, "Couldn't get connector {} from model", ex, oid);

			result = OperationResult.createOperationResult(holder.value);
			result.recordFatalError(ex);
		} catch (Exception ex) {
			LoggingUtils.logException(LOGGER, "Couldn't get connector {} from model", ex, oid);

			result = OperationResult.createOperationResult(holder.value);
			result.recordFatalError(ex);
		}

		if (!result.isSuccess()) {
			FacesUtils.addMessage(result);
		}

		return connector;
	}

	@Override
	public ConnectorDto create() throws WebModelException {
		return new ConnectorDto();
	}

	@Override
	public Collection<ConnectorDto> list(PagingType paging) throws WebModelException {
		LOGGER.debug("Listing connectors.");

		OperationResult result = new OperationResult("Get Connectors");
		Holder<OperationResultType> holder = new Holder<OperationResultType>(
				result.createOperationResultType());

		Collection<ConnectorDto> collection = new ArrayList<ConnectorDto>();
		try {
			ObjectListType list = model.listObjects(ObjectTypes.CONNECTOR.getValue(), paging, holder);
			if (list != null) {
				for (ObjectType object : list.getObject()) {
					collection.add(new ConnectorDto((ConnectorType) object));
				}
			}
			result = OperationResult.createOperationResult(holder.value);
			result.recordSuccess();
		} catch (FaultMessage ex) {
			LoggingUtils.logException(LOGGER, "Couldn't list connectors from model", ex);

			result = OperationResult.createOperationResult(holder.value);
			result.recordFatalError(ex);
		} catch (Exception ex) {
			LoggingUtils.logException(LOGGER, "Couldn't list connectors from model", ex);

			result = OperationResult.createOperationResult(holder.value);
			result.recordFatalError(ex);
		}

		if (!result.isSuccess()) {
			FacesUtils.addMessage(result);
		}
		
		LOGGER.trace(result.debugDump());

		return collection;
	}
}

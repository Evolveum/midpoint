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

import org.apache.commons.lang.Validate;

import com.evolveum.midpoint.api.logging.LoggingUtils;
import com.evolveum.midpoint.api.logging.Trace;
import com.evolveum.midpoint.common.result.OperationResult;
import com.evolveum.midpoint.logging.TraceManager;
import com.evolveum.midpoint.schema.ObjectTypes;
import com.evolveum.midpoint.schema.PagingTypeFactory;
import com.evolveum.midpoint.web.model.WebModelException;
import com.evolveum.midpoint.web.model.dto.ConnectorDto;
import com.evolveum.midpoint.web.model.dto.PropertyAvailableValues;
import com.evolveum.midpoint.web.model.dto.PropertyChange;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ConnectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.OperationResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PagingType;
import com.evolveum.midpoint.xml.ns._public.model.model_1.FaultMessage;

/**
 * 
 * @author lazyman
 * 
 */
public class ConnectorTypeManager extends ObjectManagerImpl<ConnectorDto> {

	private static final long serialVersionUID = 2332102491422179112L;
	private static final Trace LOGGER = TraceManager.getTrace(ConnectorTypeManager.class);

	@Override
	public Collection<ConnectorDto> list() throws WebModelException {
		return list(PagingTypeFactory.createListAllPaging());
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
	public ConnectorDto create() throws WebModelException {
		return new ConnectorDto();
	}

	@Override
	public Collection<ConnectorDto> list(PagingType paging) throws WebModelException {
		LOGGER.debug("Listing connectors.");
		Validate.notNull(paging);

		OperationResult result = new OperationResult("Get Connectors");
		Holder<OperationResultType> holder = new Holder<OperationResultType>(
				result.createOperationResultType());

		Collection<ConnectorDto> collection = new ArrayList<ConnectorDto>();
		try {
			ObjectListType list = getModel().listObjects(ObjectTypes.CONNECTOR.getValue(), paging, holder);
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

		printResults(LOGGER, result);

		return collection;
	}
}

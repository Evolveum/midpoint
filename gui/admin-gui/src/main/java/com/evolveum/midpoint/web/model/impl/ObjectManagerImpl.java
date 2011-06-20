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

import java.io.Serializable;

import javax.xml.ws.Holder;

import org.apache.commons.lang.Validate;
import org.springframework.beans.factory.annotation.Autowired;

import com.evolveum.midpoint.api.logging.LoggingUtils;
import com.evolveum.midpoint.api.logging.Trace;
import com.evolveum.midpoint.common.result.OperationResult;
import com.evolveum.midpoint.logging.TraceManager;
import com.evolveum.midpoint.web.model.ObjectManager;
import com.evolveum.midpoint.web.model.dto.ObjectDto;
import com.evolveum.midpoint.web.util.FacesUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.OperationResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyReferenceListType;
import com.evolveum.midpoint.xml.ns._public.model.model_1.FaultMessage;
import com.evolveum.midpoint.xml.ns._public.model.model_1.ModelPortType;

/**
 * 
 * @author lazyman
 * 
 * @param <T>
 *            object dto type class
 */
@SuppressWarnings("rawtypes")
public abstract class ObjectManagerImpl<T extends ObjectDto> implements ObjectManager<T>, Serializable {

	private static final long serialVersionUID = -7853884441389039026L;
	private static final Trace LOGGER = TraceManager.getTrace(ObjectManagerImpl.class);
	@Autowired(required = true)
	private transient ModelPortType model;

	protected ModelPortType getModel() {
		return model;
	}

	protected void printResults(Trace logger, OperationResult result) {
		if (!result.isSuccess()) {
			FacesUtils.addMessage(result);
		}

		logger.debug(result.debugDump());
	}

	@SuppressWarnings("unchecked")
	@Override
	public T get(String oid, PropertyReferenceListType resolve) {
		LOGGER.debug("Getting connector with oid {}.", new Object[] { oid });
		Validate.notNull(oid);

		OperationResult result = new OperationResult("Get Object");
		Holder<OperationResultType> holder = new Holder<OperationResultType>(
				result.createOperationResultType());

		T object = null;
		try {
			ObjectType objectType = getModel().getObject(oid, resolve, holder);
			object = create();
			object.setXmlObject(objectType);

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

		printResults(LOGGER, result);

		return object;
	}
}

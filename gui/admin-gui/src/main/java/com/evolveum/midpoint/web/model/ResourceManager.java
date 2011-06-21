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

package com.evolveum.midpoint.web.model;

import java.util.List;

import javax.xml.ws.Holder;

import org.apache.commons.lang.Validate;

import com.evolveum.midpoint.api.logging.LoggingUtils;
import com.evolveum.midpoint.api.logging.Trace;
import com.evolveum.midpoint.common.result.OperationResult;
import com.evolveum.midpoint.logging.TraceManager;
import com.evolveum.midpoint.web.model.dto.ResourceDto;
import com.evolveum.midpoint.web.model.dto.ResourceObjectShadowDto;
import com.evolveum.midpoint.web.model.impl.ObjectManagerImpl;
import com.evolveum.midpoint.xml.ns._public.common.common_1.OperationResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceObjectShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceTestResultType;
import com.evolveum.midpoint.xml.ns._public.model.model_1.FaultMessage;

/**
 * 
 * @author semancik
 * 
 */
public abstract class ResourceManager extends ObjectManagerImpl<ResourceDto> {

	private static final long serialVersionUID = -4183063295869675058L;
	private static final Trace LOGGER = TraceManager.getTrace(ResourceManager.class);

	public abstract <T extends ResourceObjectShadowType> List<ResourceObjectShadowDto<T>> listObjectShadows(
			String oid, Class<T> resourceObjectShadowType);

	public ResourceTestResultType testConnection(String resourceOid) {
		Validate.notNull(resourceOid, "Resource oid must not be null or empty.");
		LOGGER.debug("Couldn't test resource with oid {}.", new Object[] { resourceOid });

		OperationResult result = new OperationResult("Test Resource");
		Holder<OperationResultType> holder = new Holder<OperationResultType>(
				result.createOperationResultType());

		ResourceTestResultType testResult = null;
		try {
			testResult = getModel().testResource(resourceOid, holder);

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

		return testResult;
	}
	
	public void launchImportFromResource(String resourceOid, String objectClass) {
		
	}
}

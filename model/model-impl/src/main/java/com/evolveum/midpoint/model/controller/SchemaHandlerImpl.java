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
package com.evolveum.midpoint.model.controller;

import org.apache.commons.lang.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.api.logging.Trace;
import com.evolveum.midpoint.common.result.OperationResult;
import com.evolveum.midpoint.logging.TraceManager;
import com.evolveum.midpoint.model.expr.ExpressionHandler;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceObjectShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserType;

/**
 * 
 * @author lazyman
 * 
 */
@Component
public class SchemaHandlerImpl implements SchemaHandler {

	private static final Trace LOGGER = TraceManager.getTrace(SchemaHandlerImpl.class);
	@Autowired(required = true)
	private transient ExpressionHandler expressionHandler;

	@Override
	public ExpressionHandler getExpressionHandler() {
		return expressionHandler;
	}

	@Override
	public ObjectModificationType processInboundHandling(UserType user,
			ResourceObjectShadowType resourceObjectShadow, OperationResult result) {
		Validate.notNull(user, "User must not be null.");
		Validate.notNull(resourceObjectShadow, "Resource object shadow must not be null.");
		Validate.notNull(result, "Operation result must not be null.");
		LOGGER.debug("Processing inbound handling for user {} with oid {} and resource object shadow {}.",
				new Object[] { user.getName(), user.getOid(), resourceObjectShadow.getName() });

		ObjectModificationType changes = new ObjectModificationType();
		changes.setOid(user.getOid());

		// TODO: implement

		return changes;
	}

	@Override
	public ObjectModificationType processOutboundHandling(UserType user,
			ResourceObjectShadowType resourceObjectShadow, OperationResult result) {
		Validate.notNull(user, "User must not be null.");
		Validate.notNull(resourceObjectShadow, "Resource object shadow must not be null.");
		Validate.notNull(result, "Operation result must not be null.");
		LOGGER.debug("Processing outbound handling for user {} with oid {} and resource object shadow {}.",
				new Object[] { user.getName(), user.getOid(), resourceObjectShadow.getName() });

		ObjectModificationType changes = new ObjectModificationType();
		changes.setOid(resourceObjectShadow.getOid());

		// TODO: implement

		return changes;
	}
}

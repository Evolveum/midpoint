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

import com.evolveum.midpoint.common.result.OperationResult;
import com.evolveum.midpoint.model.expr.ExpressionHandler;
import com.evolveum.midpoint.schema.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceObjectShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserTemplateType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserType;

/**
 * 
 * @author lazyman
 * 
 */
public interface SchemaHandler {

	String CLASS_NAME_WITH_DOT = SchemaHandler.class.getName() + ".";
	String PROCESS_INBOUND_HANDLING = CLASS_NAME_WITH_DOT + "processInboundHandling";
	String PROCESS_OUTBOUND_HANDLING = CLASS_NAME_WITH_DOT + "processOutboundHandling";
	String INSERT_USER_DEFINED_VARIABLES = CLASS_NAME_WITH_DOT + "insertUserDefinedVariables";
	String PROCESS_ATTRIBUTE_INBOUND = CLASS_NAME_WITH_DOT + "processAttributeInbound";
	String PROCESS_PROPERTY_CONSTRUCTIONS = CLASS_NAME_WITH_DOT + "processPropertyConstructions";
	String PROCESS_PROPERTY_CONSTRUCTION = CLASS_NAME_WITH_DOT + "processPropertyConstruction";

	UserType processInboundHandling(UserType user, ResourceObjectShadowType resourceObjectShadow,
			OperationResult result) throws SchemaException;

	UserType processPropertyConstructions(UserType user, UserTemplateType template, OperationResult result)
			throws SchemaException;

	ObjectModificationType processOutboundHandling(UserType user,
			ResourceObjectShadowType resourceObjectShadow, OperationResult result) throws SchemaException;

	ExpressionHandler getExpressionHandler();
}

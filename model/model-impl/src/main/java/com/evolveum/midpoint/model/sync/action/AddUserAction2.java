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
package com.evolveum.midpoint.model.sync.action;

import com.evolveum.midpoint.common.result.OperationResult;
import com.evolveum.midpoint.model.controller.ModelController;
import com.evolveum.midpoint.model.sync.SynchronizationException;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyReferenceListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserTemplateType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserType;

/**
 * 
 * @author lazyman
 *
 */
public class AddUserAction2 {

	private ModelController model;
	
	public void action() throws Exception {
		OperationResult subResult = new OperationResult("Add User Action");
		UserType user = new UserType();
		
		ObjectType object = model.getObject("USER_TEMPLATE_OID", new PropertyReferenceListType(), subResult);
		if (!(object instanceof UserTemplateType)) {
			//TODO: exception message and logging
			throw new SynchronizationException("object is not user template.");
		}
		
		UserTemplateType userTemplate = (UserTemplateType) object;
		
		String userOid = model.addUser(user, userTemplate, subResult);
	}
}

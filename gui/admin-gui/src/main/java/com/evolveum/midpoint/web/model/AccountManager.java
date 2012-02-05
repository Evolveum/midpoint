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
package com.evolveum.midpoint.web.model;

import java.util.List;
import java.util.Set;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.bean.AccountFormBean;
import com.evolveum.midpoint.web.bean.ResourceCapability;
import com.evolveum.midpoint.web.model.dto.AccountShadowDto;
import com.evolveum.midpoint.web.model.dto.PropertyChange;
import com.evolveum.midpoint.xml.ns._public.common.common_1.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserType;

/**
 * 
 * @author lazyman
 * 
 */
public interface AccountManager extends ObjectManager<AccountShadowDto> {

	String CLASS_NAME = AccountManager.class.getName() + ".";
	String LIST_OWNER = CLASS_NAME + "listOwner";
	String SUBMIT = CLASS_NAME + "submit";

	UserType listOwner(String oid);

	ResourceCapability getResourceCapability(AccountShadowDto account);
	
	Set<PropertyChange> submit(AccountShadowDto changedObject, List<AccountShadowType> oldAccounts, Task task, OperationResult parentResult);
}

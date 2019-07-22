/**
 * Copyright (c) 2018 Evolveum
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

package com.evolveum.midpoint.web.page.admin;

import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractRoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ServiceType;

public abstract class PageAdminAbstractRole<T extends AbstractRoleType> extends PageAdminFocus<T> {
	private static final long serialVersionUID = 1L;
	
	public PageAdminAbstractRole() {
		super();
	}

	public PageAdminAbstractRole(PageParameters parameters) {
		super(parameters);
	}

	public PageAdminAbstractRole(final PrismObject<T> abstractRole) {
		super(abstractRole);
	}
	
	public PageAdminAbstractRole(final PrismObject<T> userToEdit, boolean isNewObject) {
		super(userToEdit, isNewObject);
	}
	
	
	public PageAdminAbstractRole(final PrismObject<T> abstractRole, boolean isNewObject, boolean isReadonly) {
		initialize(abstractRole, isNewObject, isReadonly);
	}

	@Override
	protected void prepareObjectDeltaForModify(ObjectDelta<T> focusDelta) throws SchemaException {
		super.prepareObjectDeltaForModify(focusDelta);

		PrismObject<T> abstractRole = getObjectWrapper().getObject();
		PrismContainerDefinition<AssignmentType> def = abstractRole.getDefinition()
				.findContainerDefinition(AbstractRoleType.F_INDUCEMENT);
	}

	@Override
	protected void prepareObjectForAdd(PrismObject<T> focus) throws SchemaException {
		super.prepareObjectForAdd(focus);
	}

	@Override
	protected void initializeModel(final PrismObject<T> objectToEdit, boolean isNewObject, boolean isReadonly) {
		super.initializeModel(objectToEdit, isNewObject, isReadonly);
	}
}

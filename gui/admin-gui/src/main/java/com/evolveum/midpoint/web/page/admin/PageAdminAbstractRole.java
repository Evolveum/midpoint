/**
 * Copyright (c) 2015 Evolveum
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

import com.evolveum.midpoint.gui.api.model.CountableLoadableModel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.component.assignment.AssignmentDto;
import com.evolveum.midpoint.web.component.assignment.AssignmentEditorDto;
import com.evolveum.midpoint.web.component.prism.ObjectWrapper;
import com.evolveum.midpoint.web.page.admin.users.dto.UserDtoStatus;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractRoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class PageAdminAbstractRole<T extends AbstractRoleType> extends PageAdminFocus<T> {
	private static final long serialVersionUID = 1L;

	private LoadableModel<List<AssignmentEditorDto>> inducementsModel;

	public LoadableModel<List<AssignmentEditorDto>> getInducementsModel() {
		return inducementsModel;
	}

	@Override
	protected void prepareObjectDeltaForModify(ObjectDelta<T> focusDelta) throws SchemaException {
		super.prepareObjectDeltaForModify(focusDelta);

		PrismObject<T> abstractRole = getObjectWrapper().getObject();
		PrismContainerDefinition<AssignmentType> def = abstractRole.getDefinition()
				.findContainerDefinition(AbstractRoleType.F_INDUCEMENT);
		handleAssignmentDeltas(focusDelta, inducementsModel.getObject(), def);
	}


	@Override
	protected void prepareObjectForAdd(PrismObject<T> focus) throws SchemaException {
		super.prepareObjectForAdd(focus);
//		handleAssignmentForAdd(focus, AbstractRoleType.F_INDUCEMENT, inducementsModel.getObject());
	}

	@Override
	protected void initializeModel(final PrismObject<T> objectToEdit, boolean isReadonly) {
		super.initializeModel(objectToEdit, isReadonly);
		inducementsModel = new LoadableModel<List<AssignmentEditorDto>>(false) {
			@Override
			protected List<AssignmentEditorDto> load() {
				return loadInducements();
			}
		};
	}

	// TODO unify with loadAssignments
	private List<AssignmentEditorDto> loadInducements() {

		List<AssignmentEditorDto> list = new ArrayList<AssignmentEditorDto>();
		List<AssignmentType> inducements = getInducementsList();
		for (AssignmentType inducement : inducements) {
			list.add(new AssignmentEditorDto(UserDtoStatus.MODIFY, inducement, this));
		}

		Collections.sort(list);

		return list;
	}

	private List<AssignmentType> getInducementsList(){
		ObjectWrapper focusWrapper = getObjectWrapper();
		PrismObject<T> focus = focusWrapper.getObject();
		List<AssignmentType> inducements = focus.asObjectable().getInducement();
		return inducements;
	}
}

/*
 * Copyright (c) 2016 Evolveum
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
package com.evolveum.midpoint.web.component.assignment;

import java.util.Iterator;
import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.util.BasePanel;
import com.evolveum.midpoint.web.page.admin.users.dto.UserDtoStatus;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractRoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;

/**
 * @author semancik
 */
public class SimpleRoleSelector<F extends FocusType, R extends AbstractRoleType> extends BasePanel<List<AssignmentEditorDto>> {

	private static final Trace LOGGER = TraceManager.getTrace(SimpleRoleSelector.class);
	
	private static final String ID_LIST = "list";
	private static final String ID_ITEM = "item";
	
	List<PrismObject<R>> availableRoles;

	public SimpleRoleSelector(String id, IModel<List<AssignmentEditorDto>> assignmentModel, List<PrismObject<R>> availableRoles) {
		super(id, assignmentModel);
		this.availableRoles = availableRoles;
		initLayout();
	}

	public List<AssignmentType> getAssignmentTypeList() {
		return null;
	}

	public String getExcludeOid() {
		return null;
	}

	private IModel<List<AssignmentEditorDto>> getAssignmentModel() {
		return getModel();
	}

	private void initLayout() {
		ListView<PrismObject<R>> list = new ListView<PrismObject<R>>(ID_LIST, availableRoles) {
			@Override
			protected void populateItem(ListItem<PrismObject<R>> item) {
				item.add(createLink(ID_ITEM, item.getModel()));
			}
		};
		list.setOutputMarkupId(true);
		add(list);

//		AjaxCheckBox checkAll = new AjaxCheckBox(ID_CHECK_ALL, new Model()) {
//
//			@Override
//			protected void onUpdate(AjaxRequestTarget target) {
//				List<AssignmentEditorDto> assignmentEditors = getAssignmentModel().getObject();
//
//				for (AssignmentEditorDto dto : assignmentEditors) {
//					dto.setSelected(this.getModelObject());
//				}
//
//				target.add(assignments);
//			}
//		};
//		assignments.add(checkAll);

	}


	private Component createLink(String id, IModel<PrismObject<R>> model) {
		AjaxLink<PrismObject<R>> button = new AjaxLink<PrismObject<R>>(id, model) {
			
			@Override
			public IModel<?> getBody() {
				return new Model<String>(getModel().getObject().asObjectable().getName().getOrig());
			}

			@Override
			public void onClick(AjaxRequestTarget target) {
				LOGGER.trace("{} CLICK: {}", this, getModel().getObject());
				toggle(getModel().getObject());
				target.add(this);
			}
			
			@Override
		    protected void onComponentTag(ComponentTag tag) {
		        super.onComponentTag(tag);
		        PrismObject<R> role = getModel().getObject();
		        if (isSelected(role)) {
		        	tag.put("class", "list-group-item active");
		        } else {
		        	tag.put("class", "list-group-item");
		        }
		    }
		};
		button.setOutputMarkupId(true);
		return button;
	}

		
	boolean isSelected(PrismObject<R> role) {
		for (AssignmentEditorDto dto: getAssignmentModel().getObject()) {
			if (dto.getTargetRef() != null && role.getOid().equals(dto.getTargetRef().getOid())) {
				if (dto.getStatus() != UserDtoStatus.DELETE) {
					return true;
				}
			}
		}
		return false;
	}
	
	void toggle(PrismObject<R> role) {
		Iterator<AssignmentEditorDto> iterator = getAssignmentModel().getObject().iterator();
		while (iterator.hasNext()) {
			AssignmentEditorDto dto = iterator.next();
			if (dto.getTargetRef() != null && role.getOid().equals(dto.getTargetRef().getOid())) {
				if (dto.getStatus() == UserDtoStatus.ADD) {
					iterator.remove();
				} else {
					dto.setStatus(UserDtoStatus.DELETE);
				}
				return;
			}
		}
		
		AssignmentEditorDto dto = AssignmentEditorDto.createDtoAddFromSelectedObject(role.asObjectable(), getPageBase());
		dto.setMinimized(true);
		getAssignmentModel().getObject().add(dto);
	}
	
}

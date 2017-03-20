/**
 * Copyright (c) 2015-2016 Evolveum
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
package com.evolveum.midpoint.web.page.admin.users.component;

import java.util.List;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.web.component.FocusSummaryPanel;
import com.evolveum.midpoint.web.component.prism.ObjectWrapper;
import com.evolveum.midpoint.web.component.util.SummaryTag;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemObjectsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * @author semancik
 *
 */
public class UserSummaryPanel extends FocusSummaryPanel<UserType> {
	private static final long serialVersionUID = -5077637168906420769L;
	
	private static final String ID_TAG_SECURITY = "summaryTagSecurity";
	private static final String ID_TAG_ORG = "summaryTagOrg";

	public UserSummaryPanel(String id, IModel<ObjectWrapper<UserType>> model) {
		super(id, model);
		
		SummaryTag<UserType> tagSecurity = new SummaryTag<UserType>(ID_TAG_SECURITY, model) {
			private static final long serialVersionUID = 1L;

			@Override
			protected void initialize(ObjectWrapper<UserType> wrapper) {
				List<AssignmentType> assignments = wrapper.getObject().asObjectable().getAssignment();
				if (assignments.isEmpty()) {
					setIconCssClass(GuiStyleConstants.CLASS_ICON_NO_OBJECTS);
					setLabel(getString("user.noAssignments"));
					setCssClass(GuiStyleConstants.CLASS_ICON_STYLE_DISABLED);
					return;
				}
				boolean isSuperuser = false;
				boolean isEndUser = false;
				for (AssignmentType assignment: assignments) {
					if (assignment.getTargetRef() != null) {
						if (SystemObjectsType.ROLE_SUPERUSER.value().equals(assignment.getTargetRef().getOid())) {
							isSuperuser = true;
						} else if (SystemObjectsType.ROLE_END_USER.value().equals(assignment.getTargetRef().getOid())) {
							isEndUser = true;
						}
					}
				}
				if (isSuperuser) {
					setIconCssClass(GuiStyleConstants.CLASS_ICON_SUPERUSER);
					setLabel(getString("user.superuser"));
					setCssClass(GuiStyleConstants.CLASS_ICON_STYLE_PRIVILEGED);
				} else if (isEndUser) {
					setIconCssClass(GuiStyleConstants.CLASS_OBJECT_USER_ICON);
					setLabel(getString("user.enduser"));
					setCssClass(GuiStyleConstants.CLASS_ICON_STYLE_END_USER);
				} else {
					setHideTag(true);
				}
			}
		};
		addTag(tagSecurity);
		
		SummaryTag<UserType> tagOrg = new SummaryTag<UserType>(ID_TAG_ORG, model) {
			private static final long serialVersionUID = 1L;

			@Override
			protected void initialize(ObjectWrapper<UserType> wrapper) {
				List<ObjectReferenceType> parentOrgRefs = wrapper.getObject().asObjectable().getParentOrgRef();
				if (parentOrgRefs.isEmpty()) {
					setIconCssClass(GuiStyleConstants.CLASS_ICON_NO_OBJECTS);
					setLabel(getString("user.noOrgs"));
					setCssClass(GuiStyleConstants.CLASS_ICON_STYLE_DISABLED);
					return;
				}
				boolean isManager = false;
				boolean isMember = false;
				for (ObjectReferenceType parentOrgRef: wrapper.getObject().asObjectable().getParentOrgRef()) {
					if (ObjectTypeUtil.isManagerRelation(parentOrgRef.getRelation())) {
						isManager = true;
					} else {
						isMember = true;
					}
				}
				if (isManager) {
					setIconCssClass(GuiStyleConstants.CLASS_OBJECT_ORG_ICON);
					setLabel(getString("user.orgManager"));
					setCssClass(GuiStyleConstants.CLASS_ICON_STYLE_MANAGER);
				} else if (isMember) {
					setIconCssClass(GuiStyleConstants.CLASS_OBJECT_ORG_ICON);
					setLabel(getString("user.orgMember"));
				} else {
					setHideTag(true);
				}
			}
		};
		addTag(tagOrg);
	}

	@Override
	protected QName getDisplayNamePropertyName() {
		return UserType.F_FULL_NAME;
	}

	@Override
	protected QName getTitlePropertyName() {
		return UserType.F_TITLE;
	}

	@Override
	protected String getIconCssClass() {
		return GuiStyleConstants.CLASS_OBJECT_USER_ICON;
	}

	@Override
	protected String getIconBoxAdditionalCssClass() {
		return "summary-panel-user";
	}

	@Override
	protected String getBoxAdditionalCssClass() {
		return "summary-panel-user";
	}

}

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
package com.evolveum.midpoint.web.page.admin.users.component;

import java.util.List;

import javax.xml.namespace.QName;

import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.web.component.FocusSummaryPanel;
import com.evolveum.midpoint.web.component.prism.ObjectWrapper;
import com.evolveum.midpoint.web.component.util.ReadOnlyWrapperModel;
import com.evolveum.midpoint.web.component.util.SummaryTag;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemObjectsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * @author semancik
 *
 */
public class UserSummaryPanel extends FocusSummaryPanel<UserType> {
	private static final long serialVersionUID = -5077637168906420769L;
	
	private static final String ID_TAG_SECURITY = "summaryTagSecurity";

	public UserSummaryPanel(String id, IModel model) {
		super(id, model);
		
		SummaryTag<UserType> tagSecurity = new SummaryTag<UserType>(ID_TAG_SECURITY, model) {
			@Override
			protected void initialize(ObjectWrapper<UserType> wrapper) {
				List<AssignmentType> assignments = wrapper.getObject().asObjectable().getAssignment();
				if (assignments.isEmpty()) {
					setIconCssClass("fa fa-times");
					setLabel("No assignments");
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
					setIconCssClass("fa fa-shield");
					setLabel("Superuser");
					setColor("red");
				} else if (isEndUser) {
					setIconCssClass("fa fa-user");
					setLabel("End User");
					setColor("green");
				} else {
					setHideTag(true);
				}
			}
		};
		addTag(tagSecurity);
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
		return "fa fa-user";
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

/*
 * Copyright (c) 2010-2017 Evolveum
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

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.DisplayNamePanel;
import com.evolveum.midpoint.gui.api.model.DisplayNameModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.self.PageSelfConsents;
import com.evolveum.midpoint.web.page.self.PageSelfDashboard;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractRoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TimeIntervalStatusType;

public class SelfConsentPanel extends BasePanel<AssignmentType> {

	private static final long serialVersionUID = 1L;

	private static final String ID_DISPLAY_NAME = "displayName";
	private static final String ID_DESCRIPTION = "description";
	private static final String ID_CONSENT_ICON = "consentIcon";
	private static final String ID_VALIDITY = "validity";

	private static final String ID_REVOKE = "revoke";
	private static final String ID_REFUSE = "refuse";
	private static final String ID_AGREE = "agree";

	private static final String DOT_CLASS = SelfConsentPanel.class.getSimpleName() + ".";
	private static final String OPERATION_LOAD_TARGET = DOT_CLASS + "loadTargetRef";
//	private PageBase parentPage;

	public SelfConsentPanel(String id, IModel<AssignmentType> model, PageBase parentPage) {
		super(id, model);

		Task task = parentPage.createSimpleTask(OPERATION_LOAD_TARGET);
		OperationResult result = task.getResult();
		
		// TODO: is this OK? We should NOT be loading this in constructor, should we?
		// ... also, we should use utility method for loading
		
		PrismObject<AbstractRoleType> abstractRole = WebModelServiceUtils
						.loadObject(getModelObject().getTargetRef(), parentPage, task, result);

		if (abstractRole == null) {
			getSession().error("Failed to load target ref");
			throw new RestartResponseException(PageSelfDashboard.class);
		}

		initLayout(abstractRole.asObjectable());
	}

	private void initLayout(final AbstractRoleType abstractRole) {
		setOutputMarkupId(true);
		
		WebMarkupContainer iconCssClass = new WebMarkupContainer(ID_CONSENT_ICON);
		iconCssClass.add(AttributeAppender.append("class", getIconCssClass(getModelObject())));
		add(iconCssClass);
		
		Label displayName = new Label(ID_DISPLAY_NAME, new DisplayNameModel(abstractRole));
		add(displayName);

		// TODO: not sure about displaying description here. It may be too long. Need to figure this out.
		Label description = new Label(ID_DESCRIPTION, Model.of(abstractRole.getDescription()));
		add(description);

		// TODO: Maybe better to use lifecycle than activation ... or a combination
		Label validityLabel = new Label(ID_VALIDITY, AssignmentsUtil.createConsentActivationTitleModel(getModel(), this));
		add(validityLabel);

		AjaxButton buttonRevoke = new AjaxButton(ID_REVOKE, createStringResource("SelfConsentPanel.button.revoke")) {

			private static final long serialVersionUID = 1L;

			@Override
			public void onClick(AjaxRequestTarget target) {
				SelfConsentPanel.this.getModelObject().setLifecycleState(SchemaConstants.LIFECYCLE_FAILED);
				target.add(SelfConsentPanel.this);
			}
		};
		add(buttonRevoke);
		buttonRevoke.add(createActiveConsentBehaviour());
		
		AjaxButton buttonAgree = new AjaxButton(ID_AGREE, createStringResource("SelfConsentPanel.button.agree")) {

			private static final long serialVersionUID = 1L;

			@Override
			public void onClick(AjaxRequestTarget target) {
				SelfConsentPanel.this.getModelObject().setLifecycleState(SchemaConstants.LIFECYCLE_ACTIVE);
				target.add(SelfConsentPanel.this);
			}
		};
		add(buttonAgree);
		buttonAgree.add(createProposedConsentBehaviour());
		
		AjaxButton buttonRefuse = new AjaxButton(ID_REFUSE, createStringResource("SelfConsentPanel.button.refuse")) {

			private static final long serialVersionUID = 1L;

			@Override
			public void onClick(AjaxRequestTarget target) {
				SelfConsentPanel.this.getModelObject().setLifecycleState(SchemaConstants.LIFECYCLE_FAILED);
				target.add(SelfConsentPanel.this);
			}
		};
		add(buttonRefuse);
		buttonRefuse.add(createProposedConsentBehaviour());

	}
	
	private VisibleEnableBehaviour createActiveConsentBehaviour() {
		return new VisibleEnableBehaviour() {
			private static final long serialVersionUID = 1L;

			@Override
			public boolean isVisible() {
				return isActiveConsent();
			}
		};
	}
	
	private VisibleEnableBehaviour createProposedConsentBehaviour() {
		return new VisibleEnableBehaviour() {
			private static final long serialVersionUID = 1L;

			@Override
			public boolean isVisible() {
				return !isActiveConsent();
			}
		};
	}


		//TODO move to the WebComponentUtil ???
	private String getIconCssClass(AssignmentType assignmentType) {
		String currentLifecycle = assignmentType.getLifecycleState();
		if (StringUtils.isBlank(currentLifecycle)) {
			return GuiStyleConstants.CLASS_APPROVAL_OUTCOME_ICON_FUTURE_COLORED;
		}

		if (SchemaConstants.LIFECYCLE_DRAFT.equals(currentLifecycle) || SchemaConstants.LIFECYCLE_PROPOSED.equals(currentLifecycle)) {
			return GuiStyleConstants.CLASS_APPROVAL_OUTCOME_ICON_IN_PROGRESS_COLORED;
		}

		if (SchemaConstants.LIFECYCLE_ACTIVE.equals(currentLifecycle)) {
			return GuiStyleConstants.CLASS_APPROVAL_OUTCOME_ICON_APPROVED_COLORED;
		}

		if (SchemaConstants.LIFECYCLE_FAILED.equals(currentLifecycle)) {
			return GuiStyleConstants.CLASS_APPROVAL_OUTCOME_ICON_REJECTED_COLORED;
		}

		return GuiStyleConstants.CLASS_APPROVAL_OUTCOME_ICON_FUTURE_COLORED;
	}

	private boolean isActiveConsent(){
		String lifecycle = SelfConsentPanel.this.getModelObject().getLifecycleState();
		if (StringUtils.isBlank(lifecycle)) {
			return false;
		}

		return lifecycle.equals(SchemaConstants.LIFECYCLE_ACTIVE);
	}

}

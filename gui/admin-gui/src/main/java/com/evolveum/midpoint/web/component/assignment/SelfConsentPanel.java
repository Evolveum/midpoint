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
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.self.PageSelfConsents;
import com.evolveum.midpoint.web.page.self.PageSelfDashboard;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractRoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;

public class SelfConsentPanel extends BasePanel<AssignmentDto>{

	private static final long serialVersionUID = 1L;

	private static final String ID_DISPLAY_NAME = "displayName";
	private static final String ID_CONSENT_ICON = "consentIcon";
	private static final String ID_VALIDITY = "validity";

	private static final String ID_REVOKE = "revoke";
	private static final String ID_AGREE = "agree";

	private static final String DOT_CLASS = SelfConsentPanel.class.getSimpleName() + ".";
	private static final String OPERATION_LOAD_TARGET = DOT_CLASS + "loadTargetRef";
//	private PageBase parentPage;

	public SelfConsentPanel(String id, IModel<AssignmentDto> model, PageBase parentPage) {
		super(id, model);

		Task task = parentPage.createSimpleTask(OPERATION_LOAD_TARGET);
		OperationResult result = task.getResult();
		PrismObject<AbstractRoleType> abstractRole = WebModelServiceUtils
						.loadObject(getModelObject().getAssignment().getTargetRef(), parentPage, task, result);

		if (abstractRole == null) {
			getSession().error("Failed to load target ref");
			throw new RestartResponseException(PageSelfDashboard.class);
		}

		initLayout(abstractRole.asObjectable());
	}

	private void initLayout(final AbstractRoleType abstractRole) {

		DisplayNamePanel<? extends AbstractRoleType> displayName = new DisplayNamePanel<>(ID_DISPLAY_NAME, Model.of(abstractRole));
		displayName.setOutputMarkupId(true);
		add(displayName);

		WebMarkupContainer iconCssClass = new WebMarkupContainer(ID_CONSENT_ICON);
		iconCssClass.add(AttributeAppender.append("class", getIconCssClass(getModelObject())));
		iconCssClass.setOutputMarkupId(true);
		add(iconCssClass);

		Label validityLabel = new Label(ID_VALIDITY, AssignmentsUtil.createActivationTitleModelExperimental(getModel(), displayName));
		validityLabel.setOutputMarkupId(true);
		add(validityLabel);

		AjaxButton revoke = new AjaxButton(ID_REVOKE, createStringResource("SelfConsentPanel.button.revoke")) {

			private static final long serialVersionUID = 1L;

			@Override
			public void onClick(AjaxRequestTarget target) {
				SelfConsentPanel.this.getModelObject().getAssignment().setLifecycleState(SchemaConstants.LIFECYCLE_FAILED);
				target.add(SelfConsentPanel.this);
			}
		};
		add(revoke);
		revoke.add(new VisibleEnableBehaviour() {

			private static final long serialVersionUID = 1L;

			@Override
			public boolean isVisible() {
				return isActiveConsent();
			}
		});

AjaxButton activate = new AjaxButton(ID_AGREE, createStringResource("SelfConsentPanel.button.agree")) {

			private static final long serialVersionUID = 1L;

			@Override
			public void onClick(AjaxRequestTarget target) {
				SelfConsentPanel.this.getModelObject().getAssignment().setLifecycleState(SchemaConstants.LIFECYCLE_ACTIVE);
				target.add(SelfConsentPanel.this);
			}
		};
		add(activate);
		activate.add(new VisibleEnableBehaviour() {

			private static final long serialVersionUID = 1L;

			@Override
			public boolean isVisible() {
				return !isActiveConsent();
			}
		});
	}

		//TODO move to the WebComponentUtil ???
	private String getIconCssClass(AssignmentDto assignment) {
		AssignmentType assignmentType = assignment.getAssignment();
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
		String lifecycle = SelfConsentPanel.this.getModelObject().getAssignment().getLifecycleState();
		if (StringUtils.isBlank(lifecycle)) {
			return false;
		}

		return lifecycle.equals(SchemaConstants.LIFECYCLE_ACTIVE);
	}

}

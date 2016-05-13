package com.evolveum.midpoint.web.page.admin.certification;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.web.component.prism.PrismPropertyPanel;
import com.evolveum.midpoint.web.component.prism.ReferenceWrapper;
import com.evolveum.midpoint.web.page.admin.certification.dto.AccessCertificationReviewerDto;
import com.evolveum.midpoint.web.page.admin.certification.dto.ManagerSearchDto;
import com.evolveum.midpoint.web.page.admin.certification.dto.StageDefinitionDto;
import com.evolveum.midpoint.web.page.admin.configuration.component.ChooseTypePanel;
import com.evolveum.midpoint.web.page.admin.dto.ObjectViewDto;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCaseOutcomeStrategyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationResponseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.*;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

import javax.xml.namespace.QName;
import java.util.List;

/**
 * Created by Kate Honchar.
 */
public class DefinitionStagePanel extends BasePanel<StageDefinitionDto> {
    private static final String ID_NAME = "name";
    private static final String ID_DESCRIPTION = "description";
    private static final String ID_DURATION = "duration";
    private static final String ID_STAGE_DURATION_HELP = "stageDurationHelp";
    private static final String ID_NOTIFY_BEFORE_DEADLINE = "notifyBeforeDeadline";
    private static final String ID_NOTIFY_BEFORE_DEADLINE_HELP = "notifyBeforeDeadlineHelp";
    private static final String ID_NOTIFY_ONLY_WHEN_NO_DECISION = "notifyOnlyWhenNoDecision";
    private static final String ID_NOTIFY_WHEN_NO_DECISION_HELP = "notifyWhenNoDecisionHelp";
    private static final String ID_REVIEWER_NAME= "reviewerName";
    private static final String ID_REVIEWER_DESCRIPTION = "reviewerDescription";
    private static final String ID_USE_TARGET_OWNER = "useTargetOwner";
    private static final String ID_USE_TARGET_APPROVER = "useTargetApprover";
    private static final String ID_TARGET_HELP = "reviewerSpecificationTargetHelp";
    private static final String ID_USE_OBJECT_OWNER = "useObjectOwner";
    private static final String ID_USE_OBJECT_APPROVER = "useObjectApprover";
	private static final String ID_OBJECT_HELP = "reviewerSpecificationObjectHelp";
    private static final String ID_USE_OBJECT_MANAGER = "useObjectManager";
	private static final String ID_USE_OBJECT_MANAGER_HELP = "reviewerUseObjectManagerHelp";
    private static final String ID_USE_OBJECT_MANAGER_ORG_TYPE = "objectManagerOrgType";
    private static final String ID_USE_OBJECT_MANAGER_ORG_TYPE_HELP = "reviewerUseObjectManagerOrgTypeHelp";
    private static final String ID_USE_OBJECT_MANAGER_ALLOW_SELF = "useObjectManagerAllowSelf";
    private static final String ID_USE_OBJECT_MANAGER_ALLOW_SELF_HELP = "reviewerUseObjectManagerAllowSelfHelp";
    private static final String ID_DEFAULT_REVIEWER_REF_CONTAINER = "defaultReviewerRefContainer";
    private static final String ID_DEFAULT_REVIEWER_REF = "defaultReviewerRef";
    private static final String ID_DEFAULT_REVIEWER_REF_HELP = "defaultReviewerRefHelp";
    private static final String ID_ADDITIONAL_REVIEWER_REF_CONTAINER = "additionalReviewerRefContainer";
    private static final String ID_ADDITIONAL_REVIEWER_REF = "additionalReviewerRef";
    private static final String ID_ADDITIONAL_REVIEWER_REF_HELP = "additionalReviewerRefHelp";
    private static final String ID_APPROVAL_STRATEGY_CHECKBOX = "approvalStrategyCheckbox";
    private static final String ID_OUTCOME_STRATEGY = "outcomeStrategy";
    private static final String ID_OUTCOME_STRATEGY_HELP = "outcomeStrategyHelp";
    private static final String ID_OUTCOME_IF_NO_REVIEWERS = "outcomeIfNoReviewers";
    private static final String ID_OUTCOME_IF_NO_REVIEWERS_HELP = "outcomeIfNoReviewersHelp";
    private static final String ID_STOP_REVIEW_ON = "stopReviewOn";
    private static final String ID_STOP_REVIEW_ON_HELP = "stopReviewOnHelp";

    // TODO remove pageBase from the constructor -- replace with delayed layout initialization
    public DefinitionStagePanel(String id, IModel<StageDefinitionDto> model, PageBase pageBase) {
        super(id, model);
        initLayout(pageBase);
    }

    protected void initLayout(PageBase pageBase) {
        TextField nameField = new TextField(ID_NAME, new PropertyModel<>(getModel(), StageDefinitionDto.F_NAME));
        add(nameField);

        TextArea descriptionField = new TextArea(ID_DESCRIPTION, new PropertyModel<>(getModel(), StageDefinitionDto.F_DESCRIPTION));
        add(descriptionField);

        TextField durationField = new TextField(ID_DURATION, new PropertyModel<>(getModel(), StageDefinitionDto.F_DURATION));
        add(durationField);
		add(WebComponentUtil.createHelp(ID_STAGE_DURATION_HELP));

        TextField notifyBeforeDeadlineField = new TextField(ID_NOTIFY_BEFORE_DEADLINE,
                new PropertyModel<>(getModel(), StageDefinitionDto.F_NOTIFY_BEFORE_DEADLINE));
        add(notifyBeforeDeadlineField);
		add(WebComponentUtil.createHelp(ID_NOTIFY_BEFORE_DEADLINE_HELP));

        add(new CheckBox(ID_NOTIFY_ONLY_WHEN_NO_DECISION, new PropertyModel<Boolean>(getModel(), StageDefinitionDto.F_NOTIFY_ONLY_WHEN_NO_DECISION)));
		add(WebComponentUtil.createHelp(ID_NOTIFY_WHEN_NO_DECISION_HELP));

		TextField reviewerNameField = new TextField(ID_REVIEWER_NAME, new PropertyModel<>(getModel(), StageDefinitionDto.F_REVIEWER_DTO + "."
                + AccessCertificationReviewerDto.F_NAME));
        add(reviewerNameField);

        TextArea reviewerDescriptionField = new TextArea(ID_REVIEWER_DESCRIPTION, new PropertyModel<>(getModel(), StageDefinitionDto.F_REVIEWER_DTO + "." +
                AccessCertificationReviewerDto.F_DESCRIPTION));
        add(reviewerDescriptionField);

        add(new CheckBox(ID_USE_TARGET_OWNER, new PropertyModel<Boolean>(getModel(), StageDefinitionDto.F_REVIEWER_DTO + "." +
                AccessCertificationReviewerDto.F_USE_TARGET_OWNER)));
        add(new CheckBox(ID_USE_TARGET_APPROVER, new PropertyModel<Boolean>(getModel(), StageDefinitionDto.F_REVIEWER_DTO + "." +
                AccessCertificationReviewerDto.F_USE_TARGET_APPROVER)));
		add(WebComponentUtil.createHelp(ID_TARGET_HELP));

		add(new CheckBox(ID_USE_OBJECT_OWNER, new PropertyModel<Boolean>(getModel(), StageDefinitionDto.F_REVIEWER_DTO + "." +
                AccessCertificationReviewerDto.F_USE_OBJECT_OWNER)));
        add(new CheckBox(ID_USE_OBJECT_APPROVER, new PropertyModel<Boolean>(getModel(), StageDefinitionDto.F_REVIEWER_DTO + "." +
                AccessCertificationReviewerDto.F_USE_OBJECT_APPROVER)));
		add(WebComponentUtil.createHelp(ID_OBJECT_HELP));

        TextField orgTypeField = new TextField(ID_USE_OBJECT_MANAGER_ORG_TYPE, new PropertyModel<>(getModel(), StageDefinitionDto.F_REVIEWER_DTO + "."
                + AccessCertificationReviewerDto.F_USE_OBJECT_MANAGER + "." + ManagerSearchDto.F_ORG_TYPE));
        add(orgTypeField);
		add(WebComponentUtil.createHelp(ID_USE_OBJECT_MANAGER_ORG_TYPE_HELP));
		add(WebComponentUtil.createHelp(ID_USE_OBJECT_MANAGER_HELP));

		add(new CheckBox(ID_USE_OBJECT_MANAGER_ALLOW_SELF, new PropertyModel<Boolean>(getModel(), StageDefinitionDto.F_REVIEWER_DTO + "." +
                AccessCertificationReviewerDto.F_USE_OBJECT_MANAGER + "." + ManagerSearchDto.F_ALLOW_SELF)));
		add(WebComponentUtil.createHelp(ID_USE_OBJECT_MANAGER_ALLOW_SELF_HELP));

		PrismPropertyPanel defaultOwnerRefPanel = new NoOffsetPrismReferencePanel(ID_DEFAULT_REVIEWER_REF,
                new PropertyModel(getModel(), StageDefinitionDto.F_REVIEWER_DTO + "." + AccessCertificationReviewerDto.F_DEFAULT_REVIEWERS),
                null, pageBase);
        defaultOwnerRefPanel.setLabelContainerVisible(false);
        add(defaultOwnerRefPanel);
		add(WebComponentUtil.createHelp(ID_DEFAULT_REVIEWER_REF_HELP));

		PrismPropertyPanel additionalOwnerRefPanel = new NoOffsetPrismReferencePanel(ID_ADDITIONAL_REVIEWER_REF,
                new PropertyModel(getModel(), StageDefinitionDto.F_REVIEWER_DTO + "." + AccessCertificationReviewerDto.F_ADDITIONAL_REVIEWERS),
                null, pageBase);
        additionalOwnerRefPanel.setLabelContainerVisible(false);
        add(additionalOwnerRefPanel);
		add(WebComponentUtil.createHelp(ID_ADDITIONAL_REVIEWER_REF_HELP));

        DropDownChoice outcomeStrategy1 =
                new DropDownChoice<>(ID_OUTCOME_STRATEGY,
                        new PropertyModel<AccessCertificationCaseOutcomeStrategyType>(getModel(), StageDefinitionDto.F_OUTCOME_STRATEGY),
                        WebComponentUtil.createReadonlyModelFromEnum(AccessCertificationCaseOutcomeStrategyType.class),
                new EnumChoiceRenderer<AccessCertificationCaseOutcomeStrategyType>(this));
        add(outcomeStrategy1);
		add(WebComponentUtil.createHelp(ID_OUTCOME_STRATEGY_HELP));

        DropDownChoice<AccessCertificationResponseType> outcomeIfNoReviewers =
                new DropDownChoice<>(ID_OUTCOME_IF_NO_REVIEWERS,
                        new PropertyModel<AccessCertificationResponseType>(getModel(), StageDefinitionDto.F_OUTCOME_IF_NO_REVIEWERS),
                        WebComponentUtil.createReadonlyModelFromEnum(AccessCertificationResponseType.class),
                new EnumChoiceRenderer<AccessCertificationResponseType>(this));
        add(outcomeIfNoReviewers);
		add(WebComponentUtil.createHelp(ID_OUTCOME_IF_NO_REVIEWERS_HELP));

		Label stopReviewOn = new Label(ID_STOP_REVIEW_ON, new AbstractReadOnlyModel<String>() {
            @Override
            public String getObject() {
                List<AccessCertificationResponseType> stopOn = getModelObject().getStopReviewOn();
                return CertMiscUtil.getStopReviewOnText(stopOn, getPageBase());
            }
        });
        add(stopReviewOn);
		add(WebComponentUtil.createHelp(ID_STOP_REVIEW_ON_HELP));
	}


    private WebMarkupContainer createReviewerRefChooser(String id, String expression) {
        ChooseTypePanel panel = new ChooseTypePanel(id,
                new PropertyModel<ObjectViewDto>(getModel(), expression)) {

            @Override
            protected boolean isSearchEnabled() {
                return true;
            }

            @Override
            protected QName getSearchProperty() {
                return UserType.F_NAME;
            }
        };

        return panel;
    }

	private class NoOffsetPrismReferencePanel extends PrismPropertyPanel<ReferenceWrapper> {
		public NoOffsetPrismReferencePanel(String id, IModel<ReferenceWrapper> propertyModel, Form form, PageBase pageBase) {
			super(id, propertyModel, form, pageBase);
		}
		// quite a hack, to get rid of col-md-offset-2 style
		@Override
		protected IModel<String> createStyleClassModel(IModel value) {
			return new AbstractReadOnlyModel<String>() {
				@Override
				public String getObject() {
					return null;
				}
			};
		}
	}
}

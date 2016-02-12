package com.evolveum.midpoint.web.page.admin.certification;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.web.component.util.SimplePanel;
import com.evolveum.midpoint.web.page.admin.certification.dto.*;
import com.evolveum.midpoint.web.page.admin.configuration.component.ChooseTypePanel;
import com.evolveum.midpoint.web.page.admin.dto.ObjectViewDto;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCaseOutcomeStrategyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationResponseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.EnumChoiceRenderer;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

import javax.xml.namespace.QName;
import java.util.List;

/**
 * Created by Kate Honchar.
 */
public class StageDefinitionPanel extends SimplePanel<StageDefinitionDto> {
    private static final String ID_NAME = "name";
    private static final String ID_DESCRIPTION = "description";
    private static final String ID_DURATION = "duration";
    private static final String ID_NOTIFY_BEFORE_DEADLINE = "notifyBeforeDeadline";
    private static final String ID_NOTIFY_ONLY_WHEN_NO_DECISION = "notifyOnlyWhenNoDecision";
    private static final String ID_REVIEWER_NAME= "reviewerName";
    private static final String ID_REVIEWER_DESCRIPTION = "reviewerDescription";
    private static final String ID_USE_TARGET_OWNER = "useTargetOwner";
    private static final String ID_USE_TARGET_APPROVER = "useTargetApprover";
    private static final String ID_USE_OBJECT_OWNER = "useObjectOwner";
    private static final String ID_USE_OBJECT_APPROVER = "useObjectApprover";
    private static final String ID_USE_OBJECT_MANAGER = "useObjectManager";
    private static final String ID_USE_OBJECT_MANAGER_ORG_TYPE = "objectManagerOrgType";
    private static final String ID_USE_OBJECT_MANAGER_ALLOW_SELF = "useObjectManagerAllowSelf";
    private static final String ID_DEFAULT_REVIEWER_REF_CONTAINER = "defaultReviewerRefContainer";
    private static final String ID_DEFAULT_REVIEWER_REF = "defaultReviewerRef";
    private static final String ID_ADDITIONAL_REVIEWER_REF_CONTAINER = "additionalReviewerRefContainer";
    private static final String ID_ADDITIONAL_REVIEWER_REF = "additionalReviewerRef";
    private static final String ID_APPROVAL_STRATEGY_CHECKBOX = "approvalStrategyCheckbox";
    private static final String ID_OUTCOME_STRATEGY = "outcomeStrategy";
    private static final String ID_OUTCOME_IF_NO_REVIEWERS = "outcomeIfNoReviewers";
    private static final String ID_STOP_REVIEW_ON = "stopReviewOn";

    public StageDefinitionPanel(String id, IModel<StageDefinitionDto> model) {
        super(id, model);
    }

    @Override
    protected void initLayout() {
        TextField nameField = new TextField(ID_NAME, new PropertyModel<>(getModel(), StageDefinitionDto.F_NAME));
        add(nameField);

        TextArea descriptionField = new TextArea(ID_DESCRIPTION, new PropertyModel<>(getModel(), StageDefinitionDto.F_DESCRIPTION));
        add(descriptionField);

        TextField durationField = new TextField(ID_DURATION, new PropertyModel<>(getModel(), StageDefinitionDto.F_DURATION));
        add(durationField);

        TextField notifyBeforeDeadlineField = new TextField(ID_NOTIFY_BEFORE_DEADLINE,
                new PropertyModel<>(getModel(), StageDefinitionDto.F_NOTIFY_BEFORE_DEADLINE));
        add(notifyBeforeDeadlineField);

        add(new AjaxCheckBox(ID_NOTIFY_ONLY_WHEN_NO_DECISION, new PropertyModel<Boolean>(getModel(), StageDefinitionDto.F_NOTIFY_ONLY_WHEN_NO_DECISION)) {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
            }
        });

        TextField reviewerNameField = new TextField(ID_REVIEWER_NAME, new PropertyModel<>(getModel(), StageDefinitionDto.F_REVIEWER_DTO + "."
                + AccessCertificationReviewerDto.F_NAME));
        add(reviewerNameField);

        TextArea reviewerDescriptionField = new TextArea(ID_REVIEWER_DESCRIPTION, new PropertyModel<>(getModel(), StageDefinitionDto.F_REVIEWER_DTO + "." +
                AccessCertificationReviewerDto.F_DESCRIPTION));
        add(reviewerDescriptionField);

        add(new AjaxCheckBox(ID_USE_TARGET_OWNER, new PropertyModel<Boolean>(getModel(), StageDefinitionDto.F_REVIEWER_DTO + "." +
                AccessCertificationReviewerDto.F_USE_TARGET_OWNER)) {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
            }
        });

        add(new AjaxCheckBox(ID_USE_TARGET_APPROVER, new PropertyModel<Boolean>(getModel(), StageDefinitionDto.F_REVIEWER_DTO + "." +
                AccessCertificationReviewerDto.F_USE_TARGET_APPROVER)) {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
            }
        });

        add(new AjaxCheckBox(ID_USE_OBJECT_OWNER, new PropertyModel<Boolean>(getModel(), StageDefinitionDto.F_REVIEWER_DTO + "." +
                AccessCertificationReviewerDto.F_USE_OBJECT_OWNER)) {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
            }
        });

        add(new AjaxCheckBox(ID_USE_OBJECT_APPROVER, new PropertyModel<Boolean>(getModel(), StageDefinitionDto.F_REVIEWER_DTO + "." +
                AccessCertificationReviewerDto.F_USE_OBJECT_APPROVER)) {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
            }
        });

        TextField orgTypeField = new TextField(ID_USE_OBJECT_MANAGER_ORG_TYPE, new PropertyModel<>(getModel(), StageDefinitionDto.F_REVIEWER_DTO + "."
                + AccessCertificationReviewerDto.F_USE_OBJECT_MANAGER + "." + ManagerSearchDto.F_ORG_TYPE));
        add(orgTypeField);


        add(new AjaxCheckBox(ID_USE_OBJECT_MANAGER_ALLOW_SELF, new PropertyModel<Boolean>(getModel(), StageDefinitionDto.F_REVIEWER_DTO + "." +
                AccessCertificationReviewerDto.F_USE_OBJECT_MANAGER + "." + ManagerSearchDto.F_ALLOW_SELF)) {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
            }
        });

        WebMarkupContainer defaultOwnerRefChooser = createReviewerRefChooser(ID_DEFAULT_REVIEWER_REF, StageDefinitionDto.F_REVIEWER_DTO + "." + AccessCertificationReviewerDto.F_FIRST_DEF_REVIEWER_REF);
        defaultOwnerRefChooser.setOutputMarkupId(true);
        add(defaultOwnerRefChooser);

        WebMarkupContainer additionalOwnerRefChooser = createReviewerRefChooser(ID_ADDITIONAL_REVIEWER_REF, StageDefinitionDto.F_REVIEWER_DTO + "." + AccessCertificationReviewerDto.F_FIRST_ADDITIONAL_REVIEWER_REF);
        additionalOwnerRefChooser.setOutputMarkupId(true);
        add(additionalOwnerRefChooser);

        DropDownChoice outcomeStrategy1 =
                new DropDownChoice<>(ID_OUTCOME_STRATEGY,
                        new PropertyModel<AccessCertificationCaseOutcomeStrategyType>(getModel(), StageDefinitionDto.F_OUTCOME_STRATEGY),
                        WebComponentUtil.createReadonlyModelFromEnum(AccessCertificationCaseOutcomeStrategyType.class),
                new EnumChoiceRenderer<AccessCertificationCaseOutcomeStrategyType>(this));
        add(outcomeStrategy1);

        DropDownChoice<AccessCertificationResponseType> outcomeIfNoReviewers =
                new DropDownChoice<>(ID_OUTCOME_IF_NO_REVIEWERS,
                        new PropertyModel<AccessCertificationResponseType>(getModel(), StageDefinitionDto.F_OUTCOME_IF_NO_REVIEWERS),
                        WebComponentUtil.createReadonlyModelFromEnum(AccessCertificationResponseType.class),
                new EnumChoiceRenderer<AccessCertificationResponseType>(this));
        add(outcomeIfNoReviewers);

        Label stopReviewOn = new Label(ID_STOP_REVIEW_ON, new AbstractReadOnlyModel<String>() {
            @Override
            public String getObject() {
                List<AccessCertificationResponseType> stopOn = getModelObject().getStopReviewOn();
                return CertMiscUtil.getStopReviewOnText(stopOn, getPageBase());
            }
        });
        add(stopReviewOn);
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
}

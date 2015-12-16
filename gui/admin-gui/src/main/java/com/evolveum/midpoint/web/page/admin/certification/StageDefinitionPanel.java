package com.evolveum.midpoint.web.page.admin.certification;

import com.evolveum.midpoint.web.component.input.DropDownChoicePanel;
import com.evolveum.midpoint.web.component.util.SimplePanel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.certification.dto.*;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationApprovalStrategyType;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

import java.util.ArrayList;
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
    private static final String ID_APPROVAL_STRATEGY = "approvalStrategy";

    public StageDefinitionPanel(String id, IModel<StageDefinitionDto> model) {
        super(id, model);
    }

    @Override
    protected void initLayout() {
        TextField nameField = new TextField(ID_NAME, new PropertyModel<>(getModel(), StageDefinitionDto.F_NAME));
        add(nameField);

        TextArea descriptionField = new TextArea(ID_DESCRIPTION, new PropertyModel<>(getModel(), StageDefinitionDto.F_DESCRIPTION));
        add(descriptionField);

        TextField durationField = new TextField(ID_DURATION, new PropertyModel<>(getModel(), StageDefinitionDto.F_DAYS));
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

        DropDownChoicePanel approvalStrategy = new DropDownChoicePanel(ID_APPROVAL_STRATEGY,
                new PropertyModel(getModel(), StageDefinitionDto.F_REVIEWER_DTO + "." + AccessCertificationReviewerDto.F_APPROVAL_STRATEGY),
                WebMiscUtil.createReadonlyModelFromEnum(AccessCertificationApprovalStrategyType.class),
                new IChoiceRenderer<AccessCertificationApprovalStrategyType>() {

                    @Override
                    public Object getDisplayValue(AccessCertificationApprovalStrategyType item) {
                        return item.name();
                    }

                    @Override
                    public String getIdValue(AccessCertificationApprovalStrategyType item, int index) {
                        return Integer.toString(index);
                    }
                });
        add(approvalStrategy);
    }

}

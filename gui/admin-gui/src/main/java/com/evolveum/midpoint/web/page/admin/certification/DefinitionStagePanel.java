/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.certification;

import java.util.List;

import com.evolveum.midpoint.gui.impl.prism.panel.ItemHeaderPanel;
import com.evolveum.midpoint.gui.impl.prism.panel.ItemPanelSettingsBuilder;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.component.prism.ItemVisibility;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.EnumChoiceRenderer;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.certification.dto.AccessCertificationReviewerDto;
import com.evolveum.midpoint.web.page.admin.certification.dto.ManagerSearchDto;
import com.evolveum.midpoint.web.page.admin.certification.dto.StageDefinitionDto;
import com.evolveum.midpoint.web.page.admin.configuration.component.EmptyOnBlurAjaxFormUpdatingBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCaseOutcomeStrategyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationResponseType;

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
    private static final String ID_USE_OBJECT_MANAGER_DETAILS= "useObjectManagerDetails";
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
    public DefinitionStagePanel(String id, IModel<StageDefinitionDto> model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout(getPageBase());
    }

    protected void initLayout(PageBase pageBase) {
        TextField nameField = new TextField<>(ID_NAME, new PropertyModel<>(getModel(), StageDefinitionDto.F_NAME));
        add(nameField);

        TextArea descriptionField = new TextArea<>(ID_DESCRIPTION, new PropertyModel<>(getModel(), StageDefinitionDto.F_DESCRIPTION));
        add(descriptionField);

        TextField durationField = new TextField<>(ID_DURATION, new PropertyModel<>(getModel(), StageDefinitionDto.F_DURATION));
        add(durationField);
        add(WebComponentUtil.createHelp(ID_STAGE_DURATION_HELP));

        TextField notifyBeforeDeadlineField = new TextField<>(ID_NOTIFY_BEFORE_DEADLINE,
                new PropertyModel<>(getModel(), StageDefinitionDto.F_NOTIFY_BEFORE_DEADLINE));
        add(notifyBeforeDeadlineField);
        add(WebComponentUtil.createHelp(ID_NOTIFY_BEFORE_DEADLINE_HELP));

        add(new CheckBox(ID_NOTIFY_ONLY_WHEN_NO_DECISION,
                new PropertyModel<>(getModel(), StageDefinitionDto.F_NOTIFY_ONLY_WHEN_NO_DECISION)));
        add(WebComponentUtil.createHelp(ID_NOTIFY_WHEN_NO_DECISION_HELP));

        TextField reviewerNameField = new TextField<>(ID_REVIEWER_NAME, new PropertyModel<>(getModel(), StageDefinitionDto.F_REVIEWER_DTO + "."
                + AccessCertificationReviewerDto.F_NAME));
        add(reviewerNameField);

        TextArea reviewerDescriptionField = new TextArea<>(ID_REVIEWER_DESCRIPTION, new PropertyModel<>(getModel(), StageDefinitionDto.F_REVIEWER_DTO + "." +
                AccessCertificationReviewerDto.F_DESCRIPTION));
        add(reviewerDescriptionField);

        add(new CheckBox(ID_USE_TARGET_OWNER, new PropertyModel<>(getModel(), StageDefinitionDto.F_REVIEWER_DTO + "." +
                AccessCertificationReviewerDto.F_USE_TARGET_OWNER)));
        add(new CheckBox(ID_USE_TARGET_APPROVER, new PropertyModel<>(getModel(), StageDefinitionDto.F_REVIEWER_DTO + "." +
                AccessCertificationReviewerDto.F_USE_TARGET_APPROVER)));
        add(WebComponentUtil.createHelp(ID_TARGET_HELP));

        add(new CheckBox(ID_USE_OBJECT_OWNER, new PropertyModel<>(getModel(), StageDefinitionDto.F_REVIEWER_DTO + "." +
                AccessCertificationReviewerDto.F_USE_OBJECT_OWNER)));
        add(new CheckBox(ID_USE_OBJECT_APPROVER, new PropertyModel<>(getModel(), StageDefinitionDto.F_REVIEWER_DTO + "." +
                AccessCertificationReviewerDto.F_USE_OBJECT_APPROVER)));
        add(WebComponentUtil.createHelp(ID_OBJECT_HELP));

        AjaxCheckBox useObjectManagerCheckbox = new AjaxCheckBox(ID_USE_OBJECT_MANAGER,
                new PropertyModel<>(getModel(), StageDefinitionDto.F_REVIEWER_DTO + "." +
                        AccessCertificationReviewerDto.F_USE_OBJECT_MANAGER_PRESENT)) {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                target.add(DefinitionStagePanel.this.get(ID_USE_OBJECT_MANAGER_DETAILS));
            }
        };
        add(useObjectManagerCheckbox);
        WebMarkupContainer useObjectManagerDetails = new WebMarkupContainer(ID_USE_OBJECT_MANAGER_DETAILS);
        useObjectManagerDetails.add(new VisibleEnableBehaviour() {
            @Override
            public boolean isEnabled() {
                return useObjectManagerCheckbox.getModelObject();
            }
        });
        useObjectManagerDetails.setOutputMarkupId(true);
        add(useObjectManagerDetails);

        add(WebComponentUtil.createHelp(ID_USE_OBJECT_MANAGER_HELP));
        TextField orgTypeField = new TextField<>(ID_USE_OBJECT_MANAGER_ORG_TYPE, new PropertyModel<>(getModel(), StageDefinitionDto.F_REVIEWER_DTO + "."
                + AccessCertificationReviewerDto.F_USE_OBJECT_MANAGER + "." + ManagerSearchDto.F_ORG_TYPE));
        orgTypeField.add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
        useObjectManagerDetails.add(orgTypeField);
        useObjectManagerDetails.add(WebComponentUtil.createHelp(ID_USE_OBJECT_MANAGER_ORG_TYPE_HELP));

        CheckBox allowSelf = new CheckBox(ID_USE_OBJECT_MANAGER_ALLOW_SELF,
                new PropertyModel<>(getModel(), StageDefinitionDto.F_REVIEWER_DTO + "." +
                        AccessCertificationReviewerDto.F_USE_OBJECT_MANAGER + "." + ManagerSearchDto.F_ALLOW_SELF));
        allowSelf.add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
        useObjectManagerDetails.add(allowSelf);
        useObjectManagerDetails.add(WebComponentUtil.createHelp(ID_USE_OBJECT_MANAGER_ALLOW_SELF_HELP));


        try {
            ItemPanelSettingsBuilder builder = new ItemPanelSettingsBuilder().visibilityHandler(iw -> ItemVisibility.AUTO);
            Panel defaultOwnerRefPanel = pageBase.initItemPanel(ID_DEFAULT_REVIEWER_REF, ObjectReferenceType.COMPLEX_TYPE,
                    new PropertyModel<>(getModel(), StageDefinitionDto.F_REVIEWER_DTO + "." + AccessCertificationReviewerDto.F_DEFAULT_REVIEWERS), builder.build());
            add(defaultOwnerRefPanel);
            add(WebComponentUtil.createHelp(ID_DEFAULT_REVIEWER_REF_HELP));

            Panel additionalOwnerRefPanel = pageBase.initItemPanel(ID_ADDITIONAL_REVIEWER_REF, ObjectReferenceType.COMPLEX_TYPE,
                    new PropertyModel<>(getModel(), StageDefinitionDto.F_REVIEWER_DTO + "." + AccessCertificationReviewerDto.F_ADDITIONAL_REVIEWERS), builder.build());
            add(additionalOwnerRefPanel);
            add(WebComponentUtil.createHelp(ID_ADDITIONAL_REVIEWER_REF_HELP));
        } catch (SchemaException e) {

        }


        DropDownChoice outcomeStrategy1 =
                new DropDownChoice<>(ID_OUTCOME_STRATEGY,
                        new PropertyModel<>(getModel(), StageDefinitionDto.F_OUTCOME_STRATEGY),
                        WebComponentUtil.createReadonlyModelFromEnum(AccessCertificationCaseOutcomeStrategyType.class),
                        new EnumChoiceRenderer<>(this));
        add(outcomeStrategy1);
        add(WebComponentUtil.createHelp(ID_OUTCOME_STRATEGY_HELP));

        DropDownChoice<AccessCertificationResponseType> outcomeIfNoReviewers =
                new DropDownChoice<>(ID_OUTCOME_IF_NO_REVIEWERS,
                        new PropertyModel<>(getModel(), StageDefinitionDto.F_OUTCOME_IF_NO_REVIEWERS),
                        WebComponentUtil.createReadonlyModelFromEnum(AccessCertificationResponseType.class),
                        new EnumChoiceRenderer<>(this));
        add(outcomeIfNoReviewers);
        add(WebComponentUtil.createHelp(ID_OUTCOME_IF_NO_REVIEWERS_HELP));

        Label stopReviewOn = new Label(ID_STOP_REVIEW_ON, new IModel<String>() {
            @Override
            public String getObject() {
                List<AccessCertificationResponseType> stopOn = getModelObject().getStopReviewOn();
                return CertMiscUtil.getStopReviewOnText(stopOn, getPageBase());
            }
        });
        add(stopReviewOn);
        add(WebComponentUtil.createHelp(ID_STOP_REVIEW_ON_HELP));
    }


//    private static class NoOffsetPrismReferencePanel extends PrismPropertyPanel<ReferenceWrapper> {
//        public NoOffsetPrismReferencePanel(String id, IModel<ReferenceWrapper> propertyModel, Form form) {
//            super(id, propertyModel, form, null);
//        }
//        // quite a hack, to get rid of col-md-offset-2 style
//        @Override
//        protected IModel<String> createStyleClassModel(IModel value) {
//            return new IModel<String>() {
//                @Override
//                public String getObject() {
//                    return null;
//                }
//            };
//        }
//    }
}

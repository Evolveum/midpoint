/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.panels;

import static com.evolveum.midpoint.gui.api.component.mining.DataStorage.*;

import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.gui.api.component.mining.structure.ProbabilityStructure;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.mining.RoleMiningFilter;
import com.evolveum.midpoint.gui.api.component.mining.analyse.tools.RoleMiningDataGenerator;
import com.evolveum.midpoint.gui.api.component.mining.analyse.tools.prune.PruneDataGenerator;
import com.evolveum.midpoint.gui.api.component.result.MessagePanel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.page.admin.role.PageRoleMiningRBAM;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.dialog.Popupable;
import com.evolveum.midpoint.web.component.form.MidpointForm;

public class GenerateDataPanelRBAM extends BasePanel<String> implements Popupable {

    private static final String ID_CANCEL_OK = "cancel";
    private static final String ID_FORM_GENERATE_USER = "form_generate_user";
    private static final String ID_FORM_GENERATE_ROLES = "form_generate_roles";
    private static final String ID_USER_COUNT = "userCount";
    private static final String ID_ROLE_COUNT = "roleCount";
    private static final String ID_ASSIGN = "assign";
    private static final String ID_ASSIGN_AUTH = "assign_auth";
    private static final String ID_ASSIGN_AUTH_MULTIPLE = "assign_auth_multiple";
    private static final String ID_UNASSIGN = "unassign";
    private static final String ID_UNASSIGN_AUTH = "unassign_auth";
    private static final String ID_WARNING = "warning";
    private static final String ID_FORM = "form";
    private static final String ID_AJAX_LINK_SUBMIT_USER_FORM = "ajax_submit_link_user";
    private static final String ID_AJAX_LINK_SUBMIT_ROLE_FORM = "ajax_submit_link_role";

    private static final String ID_PROBABILITIES_FORM = "probabilityForm";
    private static final String ID_REPEATING_OVERLAP_INPUT = "repeating_overlap";
    private static final String ID_REPEATING_PROBABILITIES_INPUT = "repeating_probabilities";
    private static final String ID_AJAX_RESET_PROB = "ajax_reset_prob";
    private static final String ID_AJAX_SUBMIT_PROB = "submit_prob";

    List<ProbabilityStructure> probabilityList = new ArrayList<>();

    int generatedUserCount = 0;
    int generatedRoleCount = 0;
    int maxPermCount = 15;

    public GenerateDataPanelRBAM(String id, IModel<String> messageModel) {
        super(id, messageModel);

    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        initLayout();
        probabilityForm();
    }

    private void initLayout() {

        Form<?> form = new MidpointForm<>(ID_FORM);
        form.setOutputMarkupId(true);
        add(form);

        MessagePanel<?> warningMessage = new MessagePanel<>(ID_WARNING,
                MessagePanel.MessagePanelType.WARN, getWarningMessageModel()) {
        };
        warningMessage.setOutputMarkupId(true);
        warningMessage.setOutputMarkupPlaceholderTag(true);
        warningMessage.setVisible(false);
        form.add(warningMessage);

        AjaxButton ajaxLinkAssign = new AjaxButton(ID_ASSIGN, Model.of("Random assign roles")) {
            @Override
            public void onClick(AjaxRequestTarget target) {

                new PruneDataGenerator().assignRoles(new RoleMiningFilter().filterUsers(getPageBase()),
                        new RoleMiningFilter().filterRoles(getPageBase()), getPageBase());
                getPage().setResponsePage(PageRoleMiningRBAM.class);

            }
        };
        ajaxLinkAssign.setOutputMarkupId(true);
        form.add(ajaxLinkAssign);

        AjaxButton ajaxLinkAssignAuth = new AjaxButton(ID_ASSIGN_AUTH, Model.of("Random add authorization (Single)")) {
            @Override
            public void onClick(AjaxRequestTarget target) {

                new PruneDataGenerator().assignAuthorization(new RoleMiningFilter().filterRoles(getPageBase()),
                        getPageBase());
                getPage().setResponsePage(PageRoleMiningRBAM.class);

            }
        };
        ajaxLinkAssignAuth.setOutputMarkupId(true);
        form.add(ajaxLinkAssignAuth);

        TextField<Integer> textFieldCount = new TextField<>("input_count_perm", Model.of(maxPermCount));
        textFieldCount.setOutputMarkupId(true);
        form.add(textFieldCount);

        AjaxSubmitLink ajaxLinkAssignAuthMultiple = new AjaxSubmitLink(ID_ASSIGN_AUTH_MULTIPLE, form) {
            @Override
            protected void onSubmit(AjaxRequestTarget target) {
                maxPermCount = textFieldCount.getModelObject();
                target.add(textFieldCount.setDefaultModelObject(maxPermCount));
                new PruneDataGenerator().assignAuthorizationMultiple(
                        new RoleMiningFilter().filterRoles(getPageBase()), textFieldCount.getModelObject(), getPageBase());
                getPage().setResponsePage(PageRoleMiningRBAM.class);

            }
        };
        ajaxLinkAssignAuthMultiple.setOutputMarkupId(true);
        form.add(ajaxLinkAssignAuthMultiple);

        AjaxButton ajaxLinkUnassign = new AjaxButton(ID_UNASSIGN, Model.of("Unassign roles")) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                new PruneDataGenerator().unassignRoles(getPageBase(), new RoleMiningFilter().filterUsers(getPageBase()));
                getPage().setResponsePage(PageRoleMiningRBAM.class);

            }
        };
        ajaxLinkUnassign.setOutputMarkupId(true);
        form.add(ajaxLinkUnassign);

        AjaxButton ajaxLinkUnassignAuth = new AjaxButton(ID_UNASSIGN_AUTH, Model.of("Unassign authorizations")) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                new PruneDataGenerator().unassignAuthorizations(getPageBase(), new RoleMiningFilter().filterRoles(getPageBase()));
                getPage().setResponsePage(PageRoleMiningRBAM.class);

            }
        };
        ajaxLinkUnassignAuth.setOutputMarkupId(true);
        form.add(ajaxLinkUnassignAuth);

        Form<?> formGenerateUser = new Form<Void>(ID_FORM_GENERATE_USER);

        final TextField<Integer> inputUserCount = new TextField<>(ID_USER_COUNT, Model.of(generatedUserCount));
        inputUserCount.setOutputMarkupId(true);

        AjaxSubmitLink ajaxSubmitUser = new AjaxSubmitLink(ID_AJAX_LINK_SUBMIT_USER_FORM, formGenerateUser) {
            @Override
            protected void onSubmit(AjaxRequestTarget target) {

                generatedUserCount = inputUserCount.getModelObject();
                new RoleMiningDataGenerator().generateUser(getPageBase(), generatedUserCount,
                        new RoleMiningFilter().filterUsers(getPageBase()));
                getPage().setResponsePage(PageRoleMiningRBAM.class);
            }
        };

        formGenerateUser.setOutputMarkupId(true);
        formGenerateUser.add(inputUserCount);
        formGenerateUser.add(ajaxSubmitUser);
        add(formGenerateUser);

        Form<?> formGenerateRoles = new Form<Void>(ID_FORM_GENERATE_ROLES);

        final TextField<Integer> inputRoleCount = new TextField<>(ID_ROLE_COUNT, Model.of(generatedRoleCount));
        inputRoleCount.setOutputMarkupId(true);

        AjaxSubmitLink ajaxSubmitRoles = new AjaxSubmitLink(ID_AJAX_LINK_SUBMIT_ROLE_FORM, formGenerateRoles) {
            @Override
            protected void onSubmit(AjaxRequestTarget target) {
                generatedUserCount = inputRoleCount.getModelObject();
                new RoleMiningDataGenerator().generateRole(getPageBase(), generatedUserCount,
                        new RoleMiningFilter().filterRoles(getPageBase()));
                getPage().setResponsePage(PageRoleMiningRBAM.class);
            }
        };

        formGenerateRoles.setOutputMarkupId(true);
        formGenerateRoles.add(inputRoleCount);
        formGenerateRoles.add(ajaxSubmitRoles);
        add(formGenerateRoles);

        AjaxButton cancelButton = new AjaxButton(ID_CANCEL_OK,
                createStringResource("Button.cancel")) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                getPageBase().hideMainPopup(target);
            }
        };
        add(cancelButton);

    }

    public void probabilityForm() {
        Form<?> probabilityForm = new Form<>(ID_PROBABILITIES_FORM);
        probabilityForm.setOutputMarkupId(true);
        add(probabilityForm);

        if (!getProbabilitiesList().isEmpty()) {
            probabilityList = getProbabilitiesList();
        } else {
            probabilityList = resetProbList();
        }

        int numRows = 5;

        RepeatingView repeatingViewOverlapInput = new RepeatingView(ID_REPEATING_OVERLAP_INPUT);

        for (int i = 0; i < numRows; i++) {
            TextField<Integer> textFieldOverlap = new TextField<>("overlap_" + i,
                    Model.of(probabilityList.get(i).getOverlap()));
            textFieldOverlap.setOutputMarkupId(true);
            repeatingViewOverlapInput.add(textFieldOverlap);

        }

        repeatingViewOverlapInput.setOutputMarkupId(true);
        probabilityForm.add(repeatingViewOverlapInput);

        RepeatingView repeatingViewProbInput = new RepeatingView(ID_REPEATING_PROBABILITIES_INPUT);

        for (int i = 0; i < numRows; i++) {
            TextField<Double> textFieldProb = new TextField<>("probability_" + i,
                    Model.of(probabilityList.get(i).getProbability()));
            textFieldProb.setOutputMarkupId(true);
            repeatingViewProbInput.add(textFieldProb);
        }
        repeatingViewProbInput.setOutputMarkupId(true);
        probabilityForm.add(repeatingViewProbInput);

        AjaxButton ajaxButtonResetProb = new AjaxButton(ID_AJAX_RESET_PROB, Model.of("Reset")) {
            @Override
            public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                resetProbabilities();
                probabilityList = resetProbList();
                ajaxRequestTarget.add(probabilityForm);
                getPage().setResponsePage(PageRoleMiningRBAM.class);
            }
        };
        ajaxButtonResetProb.setOutputMarkupId(true);
        probabilityForm.add(ajaxButtonResetProb);

        List<ProbabilityStructure> finalProbabilitiesList = probabilityList;
        AjaxSubmitLink ajaxSubmitProb = new AjaxSubmitLink(ID_AJAX_SUBMIT_PROB, probabilityForm) {
            @Override
            protected void onSubmit(AjaxRequestTarget target) {
                for (int i = 0; i < numRows; i++) {

                    int overlap = (int) ((TextField<?>) getRepeatingOverlap().get("overlap_" + i)).getModelObject();
                    double probability = (double) ((TextField<?>) getRepeatingProb().get("probability_" + i)).getModelObject();
                    finalProbabilitiesList.get(i).setOverlap(overlap);
                    finalProbabilitiesList.get(i).setProbability(probability);
                }

                setProbabilitiesList(finalProbabilitiesList);
            }
        };

        probabilityForm.add(ajaxSubmitProb);

    }

    public List<ProbabilityStructure> resetProbList() {
        List<ProbabilityStructure> probabilityList = new ArrayList<>();
        probabilityList.add(new ProbabilityStructure(5, 0.2d));
        probabilityList.add(new ProbabilityStructure(20, 0.3d));
        probabilityList.add(new ProbabilityStructure(10, 0.3d));
        probabilityList.add(new ProbabilityStructure(3, 0.1d));
        probabilityList.add(new ProbabilityStructure(1, 0.1d));

        return probabilityList;
    }

    protected RepeatingView getRepeatingOverlap() {
        return (RepeatingView) get(((PageBase) getPage())
                .createComponentPath(ID_PROBABILITIES_FORM, ID_REPEATING_OVERLAP_INPUT));
    }

    protected RepeatingView getRepeatingProb() {
        return (RepeatingView) get(((PageBase) getPage())
                .createComponentPath(ID_PROBABILITIES_FORM, ID_REPEATING_PROBABILITIES_INPUT));
    }

    @Override
    public int getWidth() {
        return 400;
    }

    @Override
    public int getHeight() {
        return 300;
    }

    @Override
    public String getWidthUnit() {
        return "px";
    }

    @Override
    public String getHeightUnit() {
        return "px";
    }

    @Override
    public Component getContent() {
        return this;
    }

    @Override
    public StringResourceModel getTitle() {
        return new StringResourceModel("RoleMining.generateDataPanel.title");
    }

    protected IModel<String> getWarningMessageModel() {
        return new StringResourceModel("RoleMining.generateDataPanel.warning");
    }

}

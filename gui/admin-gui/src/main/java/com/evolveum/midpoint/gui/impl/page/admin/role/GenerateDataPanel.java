/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role;

import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.mining.RoleMiningDataGenerator;
import com.evolveum.midpoint.gui.api.component.result.MessagePanel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.data.column.AjaxLinkPanel;
import com.evolveum.midpoint.web.component.dialog.Popupable;
import com.evolveum.midpoint.web.component.form.MidpointForm;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

public class GenerateDataPanel extends BasePanel<String> implements Popupable {

    private static final String ID_CANCEL_OK = "cancel";
    private static final String ID_FORM_GENERATE_USER = "form_generate_user";
    private static final String ID_FORM_GENERATE_ROLES = "form_generate_roles";
    private static final String ID_USER_COUNT = "userCount";
    private static final String ID_ROLE_COUNT = "roleCount";
    private static final String ID_ASSIGN = "assign";
    private static final String ID_UNASSIGN = "unassign";
    private static final String ID_WARNING = "warning";
    private static final String ID_FORM = "form";
    private static final String ID_AJAX_LINK_SUBMIT_USER_FORM = "ajax_submit_link_user";
    private static final String ID_AJAX_LINK_SUBMIT_ROLE_FORM = "ajax_submit_link_role";


    List<PrismObject<UserType>> users;
    List<PrismObject<RoleType>> roles;
    int generatedUserCount = 0;
    int generatedRoleCount = 0;

    StringResourceModel stringResourceModel;

    public GenerateDataPanel(String id, IModel<String> messageModel, List<PrismObject<UserType>> users, List<PrismObject<RoleType>> roles) {
        super(id, messageModel);
        this.users = users;
        this.roles = roles;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        initLayout();
    }

    protected MessagePanel<?> getMessagePanel() {
        return (MessagePanel<?>) get(((PageBase) getPage()).createComponentPath(ID_FORM, ID_WARNING));
    }

    private void initLayout() {

        Form<?> form = new MidpointForm<>(ID_FORM);
        form.setOutputMarkupId(true);
        add(form);

        MessagePanel<?> warningMessage = new MessagePanel<>(ID_WARNING, MessagePanel.MessagePanelType.WARN, getWarningMessageModel()) {
        };
        warningMessage.setOutputMarkupId(true);
        warningMessage.setOutputMarkupPlaceholderTag(true);
        warningMessage.setVisible(false);
        form.add(warningMessage);

        AjaxLinkPanel ajaxLinkAssign = new AjaxLinkPanel(ID_ASSIGN, Model.of("Random assign roles")) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                if (users.size() < 5 && roles.size() < 10) {
                    stringResourceModel = new StringResourceModel("RoleMining.generateDataPanel.warning");
                    target.add(getMessagePanel().replaceWith(getMessagePanel().setVisible(true)));
                } else {
                    new RoleMiningDataGenerator().assignRoles(users, roles, getPageBase());
                    getPage().setResponsePage(PageRoleMiningSimple.class);
                }
            }
        };
        ajaxLinkAssign.setOutputMarkupId(true);
        form.add(ajaxLinkAssign);

        AjaxLinkPanel ajaxLinkUnassign = new AjaxLinkPanel(ID_UNASSIGN, Model.of("Unassign roles")) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                new RoleMiningDataGenerator().unassignRoles(getPageBase(), users);
                getPage().setResponsePage(PageRoleMiningSimple.class);

            }
        };
        ajaxLinkUnassign.setOutputMarkupId(true);
        form.add(ajaxLinkUnassign);


        Form<?> formGenerateUser = new Form<Void>(ID_FORM_GENERATE_USER);

        final TextField<Integer> inputUserCount = new TextField<>(ID_USER_COUNT, Model.of(generatedUserCount));
        inputUserCount.setOutputMarkupId(true);

        AjaxSubmitLink ajaxSubmitUser = new AjaxSubmitLink(ID_AJAX_LINK_SUBMIT_USER_FORM, formGenerateUser) {
            @Override
            protected void onSubmit(AjaxRequestTarget target) {
                generatedUserCount = inputUserCount.getModelObject();
                new RoleMiningDataGenerator().generateUser(getPageBase(), generatedUserCount, users);
                getPage().setResponsePage(PageRoleMiningSimple.class);
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
                new RoleMiningDataGenerator().generateRole(getPageBase(), generatedUserCount, roles);
                getPage().setResponsePage(PageRoleMiningSimple.class);
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

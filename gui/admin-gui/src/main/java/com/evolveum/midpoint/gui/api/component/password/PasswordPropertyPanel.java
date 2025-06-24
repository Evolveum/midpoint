/*
 * Copyright (C) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.api.component.password;

import com.evolveum.midpoint.authentication.api.util.AuthUtil;
import com.evolveum.midpoint.model.api.authentication.GuiProfiledPrincipal;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.ResourceModel;

public class PasswordPropertyPanel extends PasswordPanel {

    private static final long serialVersionUID = 1L;
    private static final String ID_INFO_LABEL_CONTAINER = "infoLabelContainer";
    private static final String ID_PASSWORD_SET = "passwordSet";
    private static final String ID_PASSWORD_REMOVE = "passwordRemove";
    private static final String ID_CHANGE_PASSWORD_LINK = "changePasswordLink";
    private static final String ID_REMOVE_PASSWORD_LINK = "removePasswordLink";
    private static final String ID_BUTTON_BAR = "buttonBar";


    public PasswordPropertyPanel(String id, IModel<ProtectedStringType> passwordModel) {
        this(id, passwordModel, false, passwordModel == null || passwordModel.getObject() == null, false);
    }

    public PasswordPropertyPanel(String id, IModel<ProtectedStringType> passwordModel, boolean showOneLinePasswordPanel) {
        this(id, passwordModel, false, passwordModel == null || passwordModel.getObject() == null, showOneLinePasswordPanel, null);
    }

    public PasswordPropertyPanel(String id, IModel<ProtectedStringType> passwordModel, boolean isReadOnly, boolean isInputVisible, boolean showOneLinePasswordPanel) {
        this(id, passwordModel, isReadOnly, isInputVisible, showOneLinePasswordPanel,null);
    }

    public <F extends FocusType> PasswordPropertyPanel(String id, IModel<ProtectedStringType> passwordModel, boolean isReadOnly, boolean isInputVisible, PrismObject<F> object, boolean isTabIndexForLimitationPanelEnabled) {
        super(id, passwordModel, isReadOnly, isInputVisible, false, object, isTabIndexForLimitationPanelEnabled);
    }

    public <F extends FocusType> PasswordPropertyPanel(
            String id,
            IModel<ProtectedStringType> passwordModel,
            boolean isReadOnly,
            boolean isInputVisible,
            boolean showOneLinePasswordPanel,
            PrismObject<F> object) {
        super(id, passwordModel, isReadOnly, isInputVisible, showOneLinePasswordPanel, object);
    }

    @Override
    protected  <F extends FocusType> void initLayout() {
        super.initLayout();

        final WebMarkupContainer infoLabelContainer = new WebMarkupContainer(ID_INFO_LABEL_CONTAINER);
        infoLabelContainer.add(new VisibleBehaviour(() -> !isPasswordInputVisible()));
        infoLabelContainer.setOutputMarkupId(true);
        add(infoLabelContainer);

        final Label passwordSetLabel = new Label(ID_PASSWORD_SET, new ResourceModel("passwordPanel.passwordSet"));
        passwordSetLabel.setRenderBodyOnly(true);
        infoLabelContainer.add(passwordSetLabel);

        final Label passwordRemoveLabel = new Label(ID_PASSWORD_REMOVE, new ResourceModel("passwordPanel.passwordRemoveLabel"));
        passwordRemoveLabel.setVisible(false);
        infoLabelContainer.add(passwordRemoveLabel);

        WebMarkupContainer buttonBar = new WebMarkupContainer(ID_BUTTON_BAR);
        buttonBar.add(new VisibleBehaviour(() -> isChangePasswordLinkVisible() || isRemovePasswordVisible()));
        add(buttonBar);

        AjaxLink<Void> changePasswordLink = new AjaxLink<>(ID_CHANGE_PASSWORD_LINK) {
            private static final long serialVersionUID = 1L;
            @Override
            public void onClick(AjaxRequestTarget target) {
                changePasswordLinkClickPerformed(target);
            }
        };
        changePasswordLink.add(new VisibleBehaviour(this::isChangePasswordLinkVisible));
        changePasswordLink.setBody(new ResourceModel("passwordPanel.passwordChange"));
        changePasswordLink.setOutputMarkupId(true);
        buttonBar.add(changePasswordLink);

        AjaxLink<Void> removePassword = new AjaxLink<>(ID_REMOVE_PASSWORD_LINK) {
            private static final long serialVersionUID = 1L;
            @Override
            public void onClick(AjaxRequestTarget target) {
                onRemovePassword(getPasswordModel(), target);
            }
        };
        removePassword.add(new VisibleBehaviour(this::isRemovePasswordVisible));
        removePassword.setBody(new ResourceModel("passwordPanel.passwordRemove"));
        removePassword.setOutputMarkupId(true);
        buttonBar.add(removePassword);

    }

    private boolean isChangePasswordLinkVisible() {
        return !isReadOnly && !isPasswordInputVisible() && getPasswordModel() != null && getPasswordModel().getObject() != null;
    }

    private void changePasswordLinkClickPerformed(AjaxRequestTarget target) {
        passwordInputVisible = true;
        target.add(this);
    }

    private void onRemovePassword(IModel<ProtectedStringType> model, AjaxRequestTarget target) {
        get(ID_INFO_LABEL_CONTAINER).get(ID_PASSWORD_SET).setVisible(false);
        get(ID_INFO_LABEL_CONTAINER).get(ID_PASSWORD_REMOVE).setVisible(true);
        passwordInputVisible = false;
        model.setObject(null);
        target.add(this);
    }

    private boolean isRemovePasswordVisible() {
        return !isReadOnly && !isPasswordInputVisible() && getPasswordModel().getObject() != null && notLoggedInUserPassword();
    }

    private boolean notLoggedInUserPassword() {
        GuiProfiledPrincipal principal = AuthUtil.getPrincipalUser();
        String objectOid = getPrismObject() != null ? getPrismObject().getOid() : null;
        return principal != null && (StringUtils.isEmpty(objectOid) || !principal.getOid().equals(getPrismObject().getOid()));
    }
}

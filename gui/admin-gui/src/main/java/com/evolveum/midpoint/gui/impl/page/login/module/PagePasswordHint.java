/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.login.module;

import java.io.Serial;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;
import com.evolveum.midpoint.authentication.api.config.ModuleAuthentication;
import com.evolveum.midpoint.authentication.api.util.AuthenticationModuleNameConstants;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.impl.page.login.PageAbstractAuthenticationModule;
import com.evolveum.midpoint.web.component.form.MidpointForm;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * @author lskublik
 */
@PageDescriptor(urls = {
        @Url(mountUrl = "/hint", matchUrlForSecurity = "/hint")
}, permitAll = true, loginPage = true, authModule = AuthenticationModuleNameConstants.HINT)
public class PagePasswordHint extends PageAbstractAuthenticationModule<ModuleAuthentication> {
    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_HINT_PANEL = "hintPanel";
    private static final String ID_HINT_LABEL = "hintLabel";

    private String hint = null;

    public PagePasswordHint() {
    }

    protected void initModuleLayout(MidpointForm form) {
        initHintPanel(form);
    }

    private void initHintPanel(MidpointForm<?> form) {
        initHintValue();

        WebMarkupContainer hintPanel = new WebMarkupContainer(ID_HINT_PANEL);
        hintPanel.setOutputMarkupId(true);
        hintPanel.add(new VisibleBehaviour(this::isHintPresent));
        form.add(hintPanel);

        Label hintLabel = new Label(ID_HINT_LABEL, new LoadableModel<String>() {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected String load() {
                return createStringResource("PageEmailNonce.passwordHintLabel", hint).getString();
            }
        });
        hintLabel.setOutputMarkupId(true);
        hintPanel.add(hintLabel);
    }

    private void initHintValue() {
        UserType user = searchUser();
        hint = getUserPasswordHint(user);
    }

    private boolean isHintPresent() {
        return StringUtils.isNotEmpty(getHintValue());
    }

    private String getHintValue() {
        return hint != null ? hint : null;
    }

    private String getUserPasswordHint(@NotNull UserType user) {
        return user.getCredentials() != null && user.getCredentials().getPassword() != null ?
                user.getCredentials().getPassword().getHint() : null;
    }

    @Override
    protected IModel<String> getLoginPanelTitleModel() {
        return createStringResource("PagePasswordHint.panelTitle");
    }

    @Override
    protected IModel<String> getLoginPanelDescriptionModel() {
        return createStringResource("PagePasswordHint.description");
    }

}


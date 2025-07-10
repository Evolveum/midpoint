/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.self;

import com.evolveum.midpoint.authentication.api.util.AuthUtil;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.gui.impl.page.admin.component.UserOperationalButtonsPanel;
import com.evolveum.midpoint.gui.impl.page.admin.user.PageUser;

import com.evolveum.midpoint.gui.impl.page.admin.user.UserDetailsModel;
import com.evolveum.midpoint.gui.impl.page.self.dashboard.PageSelfDashboard;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.page.self.PagePostAuthentication;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.authentication.api.authorization.AuthorizationAction;
import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;
import com.evolveum.midpoint.web.component.breadcrumbs.Breadcrumb;
import com.evolveum.midpoint.web.page.self.PageSelf;

import org.jetbrains.annotations.NotNull;

import java.io.Serial;

@PageDescriptor(
        urls = {
                @Url(mountUrl = "/self/profile/user")
        },
        action = {
        @AuthorizationAction(actionUri = PageSelf.AUTH_SELF_ALL_URI,
                label = PageSelf.AUTH_SELF_ALL_LABEL,
                description = PageSelf.AUTH_SELF_ALL_DESCRIPTION),
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_SELF_PROFILE_URL,
                label = "PageUserSelfProfile.auth.profile.label",
                description = "PageUserSelfProfile.auth.profile.description")})
public class PageUserSelfProfile extends PageUser {

    private static final long serialVersionUID = 1L;

    public PageUserSelfProfile() {
        super();

    }

    public PageUserSelfProfile(PageParameters parameters) {
        super(parameters);
    }

    @Override
    protected UserOperationalButtonsPanel createButtonsPanel(String id, LoadableModel<PrismObjectWrapper<UserType>> wrapperModel) {
        return new UserOperationalButtonsPanel(id, wrapperModel, getObjectDetailsModels().getExecuteOptionsModel(), getObjectDetailsModels().isSelfProfile()) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void createBackButton(@NotNull RepeatingView repeatingView) {
                AjaxIconButton back = new AjaxIconButton(repeatingView.newChildId(), Model.of(GuiStyleConstants.ARROW_LEFT),
                        createStringResource("PageUserSelfProfile.button.backToHome")) {
                    @Serial private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                        backPerformed(ajaxRequestTarget);
                    }
                };

                back.showTitleAsLabel(true);
                back.add(AttributeAppender.append("class", getBackCssClass()));
                repeatingView.add(back);
            }

            @Override
            protected void backPerformedConfirmed() {
                if (AuthUtil.isPostAuthenticationEnabled(getTaskManager(), getModelInteractionService())) {
                    setResponsePage(PagePostAuthentication.class);
                }
                setResponsePage(PageSelfDashboard.class);
            }

            @Override
            protected void refresh(AjaxRequestTarget target) {
                PageUserSelfProfile.this.refresh(target);
            }
            @Override
            protected void submitPerformed(AjaxRequestTarget target) {
                PageUserSelfProfile.this.savePerformed(target);
            }

            @Override
            protected void previewPerformed(AjaxRequestTarget target) {
                PageUserSelfProfile.this.previewPerformed(target);
            }

            @Override
            protected boolean hasUnsavedChanges(AjaxRequestTarget target) {
                return PageUserSelfProfile.this.hasUnsavedChanges(target);
            }
        };
    }

    @Override
    protected String getObjectOidParameter() {
        return WebModelServiceUtils.getLoggedInFocusOid();
    }

    @Override
    protected IModel<String> createPageTitleModel() {
        return createStringResource("PageAdmin.menu.profile");
    }

    @Override
    protected UserDetailsModel createObjectDetailsModels(PrismObject<UserType> object) {
        UserDetailsModel userDetailsModel = super.createObjectDetailsModels(object);
        userDetailsModel.setSelfProfile(true);
        return userDetailsModel;
    }

    @Override
    protected void createBreadcrumb() {
        super.createBreadcrumb();

        Breadcrumb bc = getLastBreadcrumb();
        bc.setIcon(new Model(GuiStyleConstants.CLASS_OBJECT_USER_ICON));
    }
}

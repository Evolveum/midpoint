/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.systemconfiguration;

import com.evolveum.midpoint.gui.api.component.wizard.TileEnum;

import com.evolveum.midpoint.gui.impl.page.admin.EnumChoicePanel;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.authentication.api.authorization.AuthorizationAction;
import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;
import com.evolveum.midpoint.authentication.api.util.AuthConstants;
import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.page.admin.systemconfiguration.page.*;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.web.component.breadcrumbs.Breadcrumb;

/**
 * @author lazyman
 * @author skublik
 */
@PageDescriptor(
        urls = {
                @Url(mountUrl = "/admin/config/system"),
        },
        action = {
                @AuthorizationAction(actionUri = AuthConstants.AUTH_CONFIGURATION_ALL,
                        label = AuthConstants.AUTH_CONFIGURATION_ALL_LABEL,
                        description = AuthConstants.AUTH_CONFIGURATION_ALL_DESCRIPTION),
                @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_CONFIGURATION_SYSTEM_CONFIG_URL,
                        label = "PageSystemConfiguration.auth.configSystemConfiguration.label",
                        description = "PageSystemConfiguration.auth.configSystemConfiguration.description")
        })
public class PageSystemConfiguration extends PageBase {

    public enum SubPage implements TileEnum {

        BASIC("fa fa-wrench", PageSystemBasic.class),

        POLICIES("fa fa-camera", PageSystemPolicies.class),

        NOTIFICATION("fa fa-envelope", PageSystemNotification.class),

        LOGGING("fa fa-file-alt", PageSystemLogging.class),

        PROFILING("fa fa-chart-bar", PageProfiling.class),

        ADMIN_GUI("fa fa-desktop", PageSystemAdminGui.class),

        WORKFLOW("fa fa-code-branch", PageSystemWorkflow.class),

        ROLE_MANAGEMENT("fe fe-role", PageRoleManagement.class),

        INTERNALS("fa fa-cogs", PageSystemInternals.class),

        ACCESS_CERTIFICATION("fa fa-certificate", PageAccessCertification.class),

        SECRETS_PROVIDERS("fa fa-key", PageSystemSecretsProviders.class);

        String icon;

        Class<? extends PageBaseSystemConfiguration> page;

        SubPage(String icon, Class<? extends PageBaseSystemConfiguration> page) {
            this.icon = icon;
            this.page = page;
        }

        @Override
        public String getIcon() {
            return icon;
        }

        public Class<? extends PageBaseSystemConfiguration> getPage() {
            return page;
        }

        public static String getIcon(Class<? extends PageBaseSystemConfiguration> page) {
            if (page == null) {
                return null;
            }

            for (SubPage sp : values()) {
                if (page.equals(sp.getPage())) {
                    return sp.getIcon();
                }
            }

            return null;
        }
    }

    private static final long serialVersionUID = 1L;

    private static final String ID_CHOICE_PANEL = "choicePanel";

    public PageSystemConfiguration() {
        initLayout();
    }

    @Override
    protected void createBreadcrumb() {
        Breadcrumb bc = getLastBreadcrumb();
        if (bc != null && PageSystemConfiguration.class.equals(bc.getPageClass())) {
            // there's already breadcrumb for this page
            return;
        }

        super.createBreadcrumb();

        bc = getLastBreadcrumb();
        bc.setIcon(new Model(GuiStyleConstants.CLASS_SYSTEM_CONFIGURATION_ICON));
    }

    private void initLayout() {
        EnumChoicePanel<SubPage> choicePanel = new EnumChoicePanel<>(ID_CHOICE_PANEL, SubPage.class) {

            @Override
            protected IModel<String> getTextModel() {
                return createStringResource("PageSystemConfiguration.choicePanel.text");
            }

            @Override
            protected IModel<String> getSubTextModel() {
                return createStringResource("PageSystemConfiguration.choicePanel.subText");
            }

            @Override
            protected void onTemplateChosePerformed(SubPage page, AjaxRequestTarget target) {
                navigateToNext(page.getPage());
            }

            @Override
            protected String getTitleOfEnum(SubPage type) {
                return getString(type.getPage().getSimpleName() + ".title");
            }
        };
        add(choicePanel);
    }

    @Override
    protected boolean isContentVisible() {
        return true;
    }
}

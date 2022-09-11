/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.systemconfiguration;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.authentication.api.authorization.AuthorizationAction;
import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;
import com.evolveum.midpoint.authentication.api.util.AuthConstants;
import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIconBuilder;
import com.evolveum.midpoint.gui.impl.component.icon.IconCssStyle;
import com.evolveum.midpoint.gui.impl.page.admin.systemconfiguration.page.*;
import com.evolveum.midpoint.model.api.AssignmentObjectRelation;
import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.CompositedIconButtonDto;
import com.evolveum.midpoint.web.component.MultiCompositedButtonPanel;
import com.evolveum.midpoint.web.component.breadcrumbs.Breadcrumb;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DisplayType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

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

    public enum SubPage {

        BASIC("fa fa-wrench", PageSystemBasic.class),

        POLICIES("fa fa-camera", PageSystemPolicies.class),

        NOTIFICATION("fa fa-envelope", PageSystemNotification.class),

        LOGGING("fa fa-file-alt", PageSystemLogging.class),

        PROFILING("fa fa-chart-bar", PageProfiling.class),

        ADMIN_GUI("fa fa-desktop", PageSystemAdminGui.class),

        WORKFLOW("fa fa-code-branch", PageSystemWorkflow.class),

        ROLE_MANAGEMENT("fe fe-role", PageRoleManagement.class),

        INTERNALS("fa fa-cogs", PageSystemInternals.class),

        ACCESS_CERTIFICATION("fa fa-certificate", PageAccessCertification.class);

        String icon;

        Class<? extends PageBaseSystemConfiguration> page;

        SubPage(String icon, Class<? extends PageBaseSystemConfiguration> page) {
            this.icon = icon;
            this.page = page;
        }

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

    private static final String DOT_CLASS = PageSystemConfiguration.class.getName() + ".";

    private static final Trace LOGGER = TraceManager.getTrace(PageSystemConfiguration.class);

    private static final String ID_CONTAINER = "container";

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
        List<CompositedIconButtonDto> buttons = Arrays.stream(SubPage.values())
                .map(s -> createCompositedButton(s.getIcon(), s.getPage())).collect(Collectors.toUnmodifiableList());

        IModel<List<CompositedIconButtonDto>> model = Model.ofList(buttons);

        MultiCompositedButtonPanel panel = new MultiCompositedButtonPanel(ID_CONTAINER, model) {

            @Override
            protected void buttonClickPerformed(AjaxRequestTarget target, AssignmentObjectRelation relationSpec, CompiledObjectCollectionView collectionViews, Class<? extends WebPage> page) {
                navigateToNext(page);
            }
        };
        panel.add(AttributeModifier.append("class", " row"));
        add(panel);
    }

    private CompositedIconButtonDto createCompositedButton(String icon, Class<? extends WebPage> page) {
        String title = page.getSimpleName() + ".title";

        CompositedIconButtonDto button = new CompositedIconButtonDto();
        CompositedIconBuilder builder = new CompositedIconBuilder();
        builder.setTitle(title);
        builder.setBasicIcon(icon, IconCssStyle.IN_ROW_STYLE);
        button.setCompositedIcon(builder.build());
        DisplayType displayType = new DisplayType();
        displayType.setLabel(new PolyStringType(title));
        button.setAdditionalButtonDisplayType(displayType);
        button.setPage(page);

        return button;
    }

}

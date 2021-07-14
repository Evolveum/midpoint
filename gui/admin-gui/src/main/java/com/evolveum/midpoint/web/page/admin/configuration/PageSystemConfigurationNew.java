/*
 * Copyright (c) 2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.configuration;

import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIconBuilder;
import com.evolveum.midpoint.gui.impl.component.icon.IconCssStyle;
import com.evolveum.midpoint.model.api.AssignmentObjectRelation;
import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.web.component.CompositedIconButtonDto;
import com.evolveum.midpoint.web.component.MultiCompositedButtonPanel;

import com.evolveum.midpoint.web.page.admin.configuration.system.*;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.application.Url;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * @author lazyman
 * @author skublik
 */
@PageDescriptor(
        urls = {
                @Url(mountUrl = "/admin/config/system2", matchUrlForSecurity = "/admin/config/system2"),
        },
        action = {
                @AuthorizationAction(actionUri = PageAdminConfiguration.AUTH_CONFIGURATION_ALL,
                        label = PageAdminConfiguration.AUTH_CONFIGURATION_ALL_LABEL,
                        description = PageAdminConfiguration.AUTH_CONFIGURATION_ALL_DESCRIPTION),
                @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_CONFIGURATION_SYSTEM_CONFIG_URL,
                        label = "PageSystemConfiguration.auth.configSystemConfiguration.label",
                        description = "PageSystemConfiguration.auth.configSystemConfiguration.description")
        })
public class PageSystemConfigurationNew extends PageBase {

    private static final long serialVersionUID = 1L;

    private static final String DOT_CLASS = PageSystemConfigurationNew.class.getName() + ".";

    private static final Trace LOGGER = TraceManager.getTrace(PageSystemConfigurationNew.class);

    private static final String ID_CONTAINER = "container";


    public PageSystemConfigurationNew() {
        initLayout();
    }

    private void initLayout() {
        IModel<List<CompositedIconButtonDto>> model = new LoadableModel<>() {
            @Override
            protected List<CompositedIconButtonDto> load() {
                List<CompositedIconButtonDto> buttons = new ArrayList<>();
                buttons.add(createCompositedButton("Basic", "fa fa-wrench", PageSystemConfigurationBasic.class));
                buttons.add(createCompositedButton("Object policies", "fa  fa-umbrella", PageObjectPoliciesConfiguration.class));
                buttons.add(createCompositedButton("Global policy rule", "fa fa-eye", PageGlobalPolicyRule.class));
                buttons.add(createCompositedButton("Global projection policy", "fa fa-globe", PageGlobalProjectionPolicy.class));
                buttons.add(createCompositedButton("Cleanup policy", "fa  fa-eraser", PageCleanupPolicy.class));
                buttons.add(createCompositedButton("Notifications", "fa fa-envelope", PageNotificationConfiguration.class));
                buttons.add(createCompositedButton("Logging", "fa fa-file-text", PageLogging.class));
                buttons.add(createCompositedButton("Profiling", "fa fa-camera", PageProfiling.class));
                buttons.add(createCompositedButton("Admin GUI configuration", "fa fa-camera", PageAdminGuiConfiguration.class));
                buttons.add(createCompositedButton("Workflow configuration", "fa fa-camera", PageWorkflowConfiguration.class));
                buttons.add(createCompositedButton("Role management", "fa fa-camera", PageRoleManagement.class));
                buttons.add(createCompositedButton("Internals", "fa fa-camera", PageInternals.class));
                buttons.add(createCompositedButton("Deployment information", "fa fa-camera", PageDeploymentInformation.class));
                buttons.add(createCompositedButton("Access certification", "fa fa-camera", PageAccessCertification.class));
                buttons.add(createCompositedButton("Infrastructure", "fa fa-camera", PageInfrastructure.class));
                buttons.add(createCompositedButton("Full text configuration", "fa fa-camera", PageFullTextSearch.class));


                return buttons;
            }
        };
        MultiCompositedButtonPanel panel = new MultiCompositedButtonPanel(ID_CONTAINER, model) {
            @Override
            protected void buttonClickPerformed(AjaxRequestTarget target, AssignmentObjectRelation relationSepc, CompiledObjectCollectionView collectionViews, Class<? extends WebPage> page) {
                navigateToNext(page);
            }
        };
        panel.add(AttributeModifier.append("class", " row"));
        add(panel);
    }


    private CompositedIconButtonDto createCompositedButton(String type, String icon, Class<? extends WebPage> page) {
        CompositedIconButtonDto button = new CompositedIconButtonDto();
        CompositedIconBuilder builder = new CompositedIconBuilder();
        builder.setTitle(type);
        builder.setBasicIcon(icon, IconCssStyle.IN_ROW_STYLE);
        button.setCompositedIcon(builder.build());
        DisplayType displayType = new DisplayType();
        displayType.setLabel(new PolyStringType(type));
        button.setAdditionalButtonDisplayType(displayType);
        button.setPage(page);
        return button;
    }

}

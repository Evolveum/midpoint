/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.systemconfiguration;

import com.evolveum.midpoint.authentication.api.authorization.AuthorizationAction;
import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;
import com.evolveum.midpoint.authentication.api.util.AuthConstants;
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
import com.evolveum.midpoint.xml.ns._public.common.common_3.DisplayType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import java.util.Arrays;
import java.util.List;

/**
 * @author lazyman
 * @author skublik
 */
@PageDescriptor(
        urls = {
                @Url(mountUrl = "/admin/config/system2"),
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

    private static final long serialVersionUID = 1L;

    private static final String DOT_CLASS = PageSystemConfiguration.class.getName() + ".";

    private static final Trace LOGGER = TraceManager.getTrace(PageSystemConfiguration.class);

    private static final String ID_CONTAINER = "container";

    public PageSystemConfiguration() {
        initLayout();
    }

    private void initLayout() {
        IModel<List<CompositedIconButtonDto>> model = Model.ofList(Arrays.asList(
                createCompositedButton("fa fa-wrench", PageSystemBasic.class),
                createCompositedButton("fa fa-camera", PageSystemPolicies.class),
                createCompositedButton("fa fa-envelope", PageSystemNotification.class),
                createCompositedButton("fa fa-file-text", PageSystemLogging.class),
                createCompositedButton("fa fa-camera", PageProfiling.class),
                createCompositedButton("fa fa-camera", PageSystemAdminGui.class),
                createCompositedButton("fa fa-camera", PageSystemWorkflow.class),
                createCompositedButton("fa fa-camera", PageRoleManagement.class),
                createCompositedButton("fa fa-camera", PageSystemInternals.class),
                createCompositedButton("fa fa-camera", PageAccessCertification.class)
        ));

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

/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.objecttemplate;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.AssignmentHolderDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.FocusDetailsModels;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.PageAssignmentHolderDetails;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.application.Url;
import com.evolveum.midpoint.web.page.admin.configuration.PageAdminConfiguration;
import com.evolveum.midpoint.web.page.admin.objectTemplate.ObjectTemplateSummaryPanel;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectTemplateType;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

@PageDescriptor(
        urls = {
                @Url(mountUrl = "/admin/newObjectTemplate")
        },
        encoder = OnePageParameterEncoder.class,
        action = {
                @AuthorizationAction(actionUri = PageAdminConfiguration.AUTH_CONFIGURATION_ALL,
                        label = PageAdminConfiguration.AUTH_CONFIGURATION_ALL_LABEL,
                        description = PageAdminConfiguration.AUTH_CONFIGURATION_ALL_DESCRIPTION),
                @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_OBJECT_TEMPLATES_ALL_URL,
                        label = "PageObjectCollection.auth.objectTemplatesAll.label",
                        description = "PageObjectCollection.auth.objectTemplatesAll.description"),
                @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_OBJECT_TEMPLATE_URL,
                        label = "PageObjectCollection.auth.objectTemplate.label",
                        description = "PageObjectCollection.auth.objectTemplate.description")
        })
public class PageObjectTemplate extends PageAssignmentHolderDetails<ObjectTemplateType, AssignmentHolderDetailsModel<ObjectTemplateType>> {

    private static final long serialVersionUID = 1L;

    public PageObjectTemplate() {
        super();
    }

    public PageObjectTemplate(PageParameters parameters) {
        super(parameters);
    }

    public PageObjectTemplate(final PrismObject<ObjectTemplateType> obj) {
        super(obj);
    }

    @Override
    protected Class<ObjectTemplateType> getType() {
        return ObjectTemplateType.class;
    }

    @Override
    protected Panel createSummaryPanel(String id, LoadableModel<ObjectTemplateType> summaryModel) {
        return new ObjectTemplateSummaryPanel(id, summaryModel, getSummaryPanelSpecification());
    }
}

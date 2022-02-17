/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.messagetemplate;

import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.evolveum.midpoint.authentication.api.authorization.AuthorizationAction;
import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;
import com.evolveum.midpoint.authentication.api.util.AuthConstants;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.PageAssignmentHolderDetails;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MessageTemplateType;

@PageDescriptor(
        urls = {
                @Url(mountUrl = "/admin/messageTemplate")
        },
        encoder = OnePageParameterEncoder.class,
        action = {
                @AuthorizationAction(actionUri = AuthConstants.AUTH_CONFIGURATION_ALL,
                        label = AuthConstants.AUTH_CONFIGURATION_ALL_LABEL,
                        description = AuthConstants.AUTH_CONFIGURATION_ALL_DESCRIPTION),
                @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_MESSAGE_TEMPLATES_ALL_URL,
                        label = "PageMessageTemplates.auth.messageTemplatesAll.label",
                        description = "PageMessageTemplates.auth.messageTemplatesAll.description"),
                @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_MESSAGE_TEMPLATE_URL,
                        label = "PageMessageTemplate.auth.messageTemplate.label",
                        description = "PageMessageTemplate.auth.messageTemplate.description")
        })
public class PageMessageTemplate extends PageAssignmentHolderDetails<MessageTemplateType, MessageTemplateModel> {

    private static final long serialVersionUID = 1L;

    public PageMessageTemplate() {
        super();
    }

    public PageMessageTemplate(PageParameters parameters) {
        super(parameters);
    }

    public PageMessageTemplate(final PrismObject<MessageTemplateType> object) {
        super(object);
    }

    @Override
    public Class<MessageTemplateType> getType() {
        return MessageTemplateType.class;
    }

    @Override
    protected Panel createSummaryPanel(String id, IModel<MessageTemplateType> model) {
        return new MessageTemplateSummaryPanel(id, model, getSummaryPanelSpecification());
    }
}

/*
 * Copyright (c) 2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.certification;

import com.evolveum.midpoint.gui.impl.page.admin.certification.helpers.CampaignProcessingHelper;
import com.evolveum.midpoint.prism.query.ObjectQuery;

import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignType;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.evolveum.midpoint.authentication.api.authorization.AuthorizationAction;
import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.page.admin.certification.component.CampaignsPanel;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.form.MidpointForm;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationDefinitionType;

import java.io.Serial;

@PageDescriptor(
        urls = {
                @Url(mountUrl = "/admin/certification/campaigns", matchUrlForSecurity = "/admin/certification/campaigns")
        },
        encoder = OnePageParameterEncoder.class, action = {
        @AuthorizationAction(actionUri = PageAdminCertification.AUTH_CERTIFICATION_ALL,
                label = PageAdminCertification.AUTH_CERTIFICATION_ALL_LABEL,
                description = PageAdminCertification.AUTH_CERTIFICATION_ALL_DESCRIPTION),
        @AuthorizationAction(actionUri = PageAdminCertification.AUTH_CERTIFICATION_CAMPAIGNS,
                label = PageAdminCertification.AUTH_CERTIFICATION_CAMPAIGNS_LABEL,
                description = PageAdminCertification.AUTH_CERTIFICATION_CAMPAIGNS_DESCRIPTION) })
public class PageCertCampaigns extends PageAdminCertification {

    private static final Trace LOGGER = TraceManager.getTrace(PageCertCampaigns.class);

    private static final String ID_MAIN_FORM = "mainForm";
    private static final String ID_CAMPAIGNS_TABLE = "campaignsTable";

    public PageCertCampaigns(PageParameters parameters) {
        super(parameters);
        initLayout();
    }

    private String getCampaignDefinitionOid() {
        return getPageParameters().get(OnePageParameterEncoder.PARAMETER).toString();
    }

    @Override
    protected IModel<String> createPageTitleModel() {
        String definitionOid = getCampaignDefinitionOid();
        if (definitionOid == null) {
            return super.createPageTitleModel();
        } else {
            return new LoadableDetachableModel<String>() {
                @Override
                protected String load() {
                    Task task = createSimpleTask("dummy");
                    PrismObject<AccessCertificationDefinitionType> definitionPrismObject = WebModelServiceUtils
                            .loadObject(AccessCertificationDefinitionType.class, definitionOid,
                                    PageCertCampaigns.this, task, task.getResult());

                    String name = definitionPrismObject == null ? ""
                            : WebComponentUtil.getName(definitionPrismObject);

                    return createStringResource("PageCertCampaigns.title", name).getString();
                }
            };
        }
    }

    private void initLayout() {
        Form<?> mainForm = new MidpointForm<>(ID_MAIN_FORM);
        add(mainForm);

        CampaignsPanel campaignsPanel = createCampaignsPanel();
        campaignsPanel.setOutputMarkupId(true);
        mainForm.add(campaignsPanel);

    }

    protected CampaignsPanel createCampaignsPanel() {
        return new CampaignsPanel(ID_CAMPAIGNS_TABLE) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected ObjectQuery getCustomCampaignsQuery() {
                String certDefinitionOid = getPageParameters().get(OnePageParameterEncoder.PARAMETER) != null ?
                        getPageParameters().get(OnePageParameterEncoder.PARAMETER).toString() : null;
                if (StringUtils.isEmpty(certDefinitionOid)) {
                    return super.getCustomCampaignsQuery();
                }
                return getPrismContext()
                        .queryFor(AccessCertificationCampaignType.class)
                        .item(AccessCertificationCampaignType.F_DEFINITION_REF)
                        .ref(certDefinitionOid)
                        .build();
            }

            @Override
            protected void nameColumnLinkClickPerformed(AjaxRequestTarget target,
                    IModel<SelectableBean<AccessCertificationCampaignType>> rowModel) {
                CampaignProcessingHelper.campaignDetailsPerformed(rowModel.getObject().getValue().getOid(), getPageBase());
            }

            @Override
            protected boolean isNameColumnLinkEnabled(IModel<SelectableBean<AccessCertificationCampaignType>> rowModel) {
                return WebComponentUtil.isAuthorizedForPage(PageCertCampaign.class);
            }
        };
    }
}

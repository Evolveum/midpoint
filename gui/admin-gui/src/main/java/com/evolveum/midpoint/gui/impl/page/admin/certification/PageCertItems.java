/*
 * Copyright (c) 2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.certification;

import java.io.Serial;
import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.evolveum.midpoint.authentication.api.authorization.AuthorizationAction;
import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.page.admin.certification.component.CertificationItemsPanel;
import com.evolveum.midpoint.web.component.DateLabelComponent;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignType;

@PageDescriptor(
        urls = {
                @Url(mountUrl = "/admin/certification/decisionsAll", matchUrlForSecurity = "/admin/certification/decisionsAll")
        },
        encoder = OnePageParameterEncoder.class,
        action = {
                @AuthorizationAction(actionUri = PageAdminCertification.AUTH_CERTIFICATION_ALL,
                        label = PageAdminCertification.AUTH_CERTIFICATION_ALL_LABEL,
                        description = PageAdminCertification.AUTH_CERTIFICATION_ALL_DESCRIPTION),
                @AuthorizationAction(actionUri = PageAdminCertification.AUTH_CERTIFICATION_DECISIONS,
                        label = PageAdminCertification.AUTH_CERTIFICATION_DECISIONS_LABEL,
                        description = PageAdminCertification.AUTH_CERTIFICATION_DECISIONS_DESCRIPTION)})
public class PageCertItems extends PageAdminCertification {

    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_CERT_ITEMS_PANEL = "certItemsPanel";

    String campaignOid = null;

    public PageCertItems() {
    }

    public PageCertItems(PageParameters params) {
        super(params);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        campaignOid = getCampaignOid();

        initLayout();
    }

    private String getCampaignOid() {
        return OnePageParameterEncoder.getParameter(this);
    }

    private void initLayout() {
        addCertItemsPanel();
    }

    private void addCertItemsPanel() {
        CertificationItemsPanel table = new CertificationItemsPanel(ID_CERT_ITEMS_PANEL) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected boolean isMyCertItems() {
                return PageCertItems.this.isMyCertItems();
            }

            @Override
            protected boolean showOnlyNotDecidedItems() {
                return PageCertItems.this.showOnlyNotDecidedItems();
            }

            @Override
            protected void onBackPerformed(AjaxRequestTarget target) {
                redirectBack(); //TODO
            }

            @Override
            protected String getCampaignOid() {
                return campaignOid;
            }

        };
        table.setOutputMarkupId(true);
        add(table);
    }


    public IModel<String> getCampaignStartDateModel(AccessCertificationCampaignType campaign) {
        return () -> {
            if (campaign == null) {
                return "";
            }
            XMLGregorianCalendar startDate = campaign.getStartTimestamp();
            return WebComponentUtil.getLocalizedDate(startDate, DateLabelComponent.SHORT_NOTIME_STYLE);
        };
    }


    boolean showOnlyNotDecidedItems() {
        return false;
    }

    protected boolean isMyCertItems() {
        return false;
    }
}

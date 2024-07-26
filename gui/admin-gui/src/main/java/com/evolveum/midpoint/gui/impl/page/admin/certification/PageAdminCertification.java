/*
 * Copyright (c) 2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.certification;


import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.web.page.admin.PageAdmin;

import org.apache.wicket.request.mapper.parameter.PageParameters;

/**
 * @author lazyman
 */
public class PageAdminCertification extends PageAdmin {

    public static final String AUTH_CERTIFICATION_ALL = AuthorizationConstants.AUTZ_UI_CERTIFICATION_ALL_URL;
    public static final String AUTH_CERTIFICATION_ALL_LABEL = "PageAdminCertification.auth.certificationAll.label";
    public static final String AUTH_CERTIFICATION_ALL_DESCRIPTION = "PageAdminCertification.auth.certificationAll.description";

    public static final String AUTH_CERTIFICATION_DEFINITIONS = AuthorizationConstants.AUTZ_UI_CERTIFICATION_DEFINITIONS_URL;
    public static final String AUTH_CERTIFICATION_DEFINITIONS_LABEL = "PageAdminCertification.auth.certificationDefinitions.label";
    public static final String AUTH_CERTIFICATION_DEFINITIONS_DESCRIPTION = "PageAdminCertification.auth.certificationDefinitions.description";

    public static final String AUTH_CERTIFICATION_DEFINITION = AuthorizationConstants.AUTZ_UI_CERTIFICATION_DEFINITION_URL;
    public static final String AUTH_CERTIFICATION_DEFINITION_LABEL = "PageAdminCertification.auth.certificationDefinition.label";
    public static final String AUTH_CERTIFICATION_DEFINITION_DESCRIPTION = "PageAdminCertification.auth.certificationDefinition.description";

    // TODO how to use this (distinguish 'new definition' from 'edit definition')
    public static final String AUTH_CERTIFICATION_NEW_DEFINITION = AuthorizationConstants.AUTZ_UI_CERTIFICATION_NEW_DEFINITION_URL;
    public static final String AUTH_CERTIFICATION_NEW_DEFINITION_LABEL = "PageAdminCertification.auth.certificationNewDefinition.label";
    public static final String AUTH_CERTIFICATION_NEW_DEFINITION_DESCRIPTION = "PageAdminCertification.auth.certificationNewDefinition.description";

    public static final String AUTH_CERTIFICATION_CAMPAIGNS = AuthorizationConstants.AUTZ_UI_CERTIFICATION_CAMPAIGNS_URL;
    public static final String AUTH_CERTIFICATION_CAMPAIGNS_LABEL = "PageAdminCertification.auth.certificationCampaigns.label";
    public static final String AUTH_CERTIFICATION_CAMPAIGNS_DESCRIPTION = "PageAdminCertification.auth.certificationCampaigns.description";

    public static final String AUTH_MY_CERTIFICATION_CAMPAIGNS = AuthorizationConstants.AUTZ_UI_MY_CERTIFICATION_DECISIONS_URL;
    public static final String AUTH_MY_CERTIFICATION_CAMPAIGNS_LABEL = "PageAdminCertification.auth.myCertificationCampaigns.label";
    public static final String AUTH_MY_CERTIFICATION_CAMPAIGNS_DESCRIPTION = "PageAdminCertification.auth.myCertificationCampaigns.description";

    public static final String AUTH_ACTIVE_CERTIFICATION_CAMPAIGNS = AuthorizationConstants.AUTZ_UI_CERTIFICATION_DECISIONS_URL;
    public static final String AUTH_ACTIVE_CERTIFICATION_CAMPAIGNS_LABEL = "PageAdminCertification.auth.activeCertificationCampaigns.label";
    public static final String AUTH_ACTIVE_CERTIFICATION_CAMPAIGNS_DESCRIPTION = "PageAdminCertification.auth.activeCertificationCampaigns.description";

    public static final String AUTH_CERTIFICATION_CAMPAIGN = AuthorizationConstants.AUTZ_UI_CERTIFICATION_CAMPAIGN_URL;
    public static final String AUTH_CERTIFICATION_CAMPAIGN_LABEL = "PageAdminCertification.auth.certificationCampaign.label";
    public static final String AUTH_CERTIFICATION_CAMPAIGN_DESCRIPTION = "PageAdminCertification.auth.certificationCampaign.description";

    public static final String AUTH_CERTIFICATION_DECISIONS = AuthorizationConstants.AUTZ_UI_CERTIFICATION_DECISIONS_URL;
    public static final String AUTH_CERTIFICATION_DECISIONS_LABEL = "PageAdminCertification.auth.certificationDecisions.label";
    public static final String AUTH_CERTIFICATION_DECISIONS_DESCRIPTION = "PageAdminCertification.auth.certificationDecisions.description";

    public static final String AUTH_MY_CERTIFICATION_DECISIONS = AuthorizationConstants.AUTZ_UI_MY_CERTIFICATION_DECISIONS_URL;
    public static final String AUTH_MY_CERTIFICATION_DECISIONS_LABEL = "PageAdminCertification.auth.myCertificationDecisions.label";
    public static final String AUTH_MY_CERTIFICATION_DECISIONS_DESCRIPTION = "PageAdminCertification.auth.myCertificationDecisions.description";

    public static final String AUTH_CERTIFICATION_CAMPAIGN_DECISIONS_URL = AuthorizationConstants.AUTZ_UI_CERTIFICATION_CAMPAIGN_DECISIONS_URL;
    public static final String AUTH_CERTIFICATION_CAMPAIGN_DECISIONS_LABEL = "PageAdminCertification.auth.certificationCampaignDecisions.label";
    public static final String AUTH_CERTIFICATION_CAMPAIGN_DECISIONS_DESCRIPTION = "PageAdminCertification.auth.certificationCampaignDecisions.description";

    public static final String AUTH_MY_CERTIFICATION_CAMPAIGN_DECISIONS_URL = AuthorizationConstants.AUTZ_UI_MY_CERTIFICATION_CAMPAIGN_DECISIONS_URL;
    public static final String AUTH_MY_CERTIFICATION_CAMPAIGN_DECISIONS_LABEL = "PageAdminCertification.auth.myCertificationCampaignDecisions.label";
    public static final String AUTH_MY_CERTIFICATION_CAMPAIGN_DECISIONS_DESCRIPTION = "PageAdminCertification.auth.myCertificationCampaignDecisions.description";

    public PageAdminCertification() {
        super();
    }

    public PageAdminCertification(PageParameters parameters) {
        super(parameters);
    }
}

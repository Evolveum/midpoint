/*
 * Copyright (c) 2010-2015 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.web.page.admin.certification;


import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.web.page.admin.PageAdmin;

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

    public static final String AUTH_CERTIFICATION_CAMPAIGN = AuthorizationConstants.AUTZ_UI_CERTIFICATION_CAMPAIGN_URL;
    public static final String AUTH_CERTIFICATION_CAMPAIGN_LABEL = "PageAdminCertification.auth.certificationCampaign.label";
    public static final String AUTH_CERTIFICATION_CAMPAIGN_DESCRIPTION = "PageAdminCertification.auth.certificationCampaign.description";

    public static final String AUTH_CERTIFICATION_DECISIONS = AuthorizationConstants.AUTZ_UI_CERTIFICATION_DECISIONS_URL;
    public static final String AUTH_CERTIFICATION_DECISIONS_LABEL = "PageAdminCertification.auth.certificationDecisions.label";
    public static final String AUTH_CERTIFICATION_DECISIONS_DESCRIPTION = "PageAdminCertification.auth.certificationDecisions.description";
}

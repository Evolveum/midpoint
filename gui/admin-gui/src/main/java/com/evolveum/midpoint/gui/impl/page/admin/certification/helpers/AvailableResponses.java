/*
 * Copyright (c) 2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.certification.helpers;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationResponseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.GuiActionType;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationResponseType.ACCEPT;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationResponseType.REVOKE;

/**
 // available responses can be configured in 3 places:
 // 1. systemConfiguration -> accessCertification -> availableResponses
 // 2. certificationCampaign -> accessCertification -> defaultView -> action
 // 3. accessCertificationDefinition -> view -> action
 // todo for now this class takes care only for 2 first options. the third should be counted as well
 */
public class AvailableResponses implements Serializable {

    private List<String> responseKeys;
    private List<AccessCertificationResponseType> responseValues;
    private PageBase pageBase;

    public AvailableResponses(PageBase pageBase) {
        this.pageBase = pageBase;

        AccessCertificationConfigurationType config = WebModelServiceUtils.getCertificationConfiguration(pageBase);

        responseKeys = new ArrayList<>(6);
        responseValues = new ArrayList<>(6);

        addResponse(config, "PageCertDecisions.menu.accept", ACCEPT);
        addResponse(config, "PageCertDecisions.menu.revoke", REVOKE);

        List<GuiActionType> configuredActions = config != null && config.getDefaultView() != null ? config.getDefaultView().getAction() : new ArrayList<>();
        addResponses(configuredActions);

        //starting from 4.9 the default responses are Accept a Revoke. All others should be configured if needed
//        addResponse(config, "PageCertDecisions.menu.reduce", REDUCE);
//        addResponse(config, "PageCertDecisions.menu.notDecided", NOT_DECIDED);
//        addResponse(config, "PageCertDecisions.menu.noResponse", NO_RESPONSE);
    }

    public List<String> getResponseKeys() {
        return responseKeys;
    }

    public List<AccessCertificationResponseType> getResponseValues() {
        return responseValues;
    }

    private void addResponse(AccessCertificationConfigurationType config, String captionKey, AccessCertificationResponseType response) {
        if (config != null && !config.getAvailableResponse().isEmpty() && !config.getAvailableResponse().contains(response)) {
            return;
        }
        responseKeys.add(captionKey);
        responseValues.add(response);
    }

    private void addResponses(List<GuiActionType> configuredActions) {
        for (GuiActionType action : configuredActions) {
            var response = CertificationItemResponseHelper.getResponseForGuiAction(action);
            if (response != null && !responseValues.contains(response)) {
                var responseHelper = new CertificationItemResponseHelper(response);
                responseKeys.add(responseHelper.getLabelKey());
                responseValues.add(response);
            }
        }
    }

    public boolean isAvailable(AccessCertificationResponseType response) {
        return response == null || responseValues.contains(response);
    }

    public String getTitle(int id) {
        if (id < responseKeys.size()) {
            return PageBase.createStringResourceStatic(responseKeys.get(id)).getString();
        } else {
            return PageBase.createStringResourceStatic("PageCertDecisions.menu.illegalResponse").getString();
        }
    }

    public int getCount() {
        return responseKeys.size();
    }
}

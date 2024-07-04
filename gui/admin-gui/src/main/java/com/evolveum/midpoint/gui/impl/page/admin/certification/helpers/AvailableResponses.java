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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationResponseType.ACCEPT;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationResponseType.NOT_DECIDED;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationResponseType.NO_RESPONSE;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationResponseType.REDUCE;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationResponseType.REVOKE;

/**
 * TODO implement more cleanly
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

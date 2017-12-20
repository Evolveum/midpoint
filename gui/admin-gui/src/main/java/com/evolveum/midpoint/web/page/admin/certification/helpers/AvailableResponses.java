/*
 * Copyright (c) 2010-2017 Evolveum
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

package com.evolveum.midpoint.web.page.admin.certification.helpers;

import com.evolveum.midpoint.common.SystemConfigurationHolder;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationResponseType;
import org.apache.wicket.Page;

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
 *
 * @author mederly
 */
public class AvailableResponses implements Serializable {

    private List<String> responseKeys;
    private List<AccessCertificationResponseType> responseValues;
    private Page page;

    public AvailableResponses(Page page) {
        this.page = page;

        // TODO: use ModelInteractionService.getCertificationConfiguration() instead
        AccessCertificationConfigurationType config = SystemConfigurationHolder.getCertificationConfiguration();

        responseKeys = new ArrayList<>(6);
        responseValues = new ArrayList<>(6);

        addResponse(config, "PageCertDecisions.menu.accept", ACCEPT);
        addResponse(config, "PageCertDecisions.menu.revoke", REVOKE);
        addResponse(config, "PageCertDecisions.menu.reduce", REDUCE);
        addResponse(config, "PageCertDecisions.menu.notDecided", NOT_DECIDED);
        addResponse(config, "PageCertDecisions.menu.noResponse", NO_RESPONSE);
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
            return PageBase.createStringResourceStatic(page, responseKeys.get(id)).getString();
        } else {
            return PageBase.createStringResourceStatic(page, "PageCertDecisions.menu.illegalResponse").getString();
        }
    }

    public int getCount() {
        return responseKeys.size();
    }
}

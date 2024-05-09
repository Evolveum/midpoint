/*
 * Copyright (c) 2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.certification.helpers;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.util.LocalizationUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationResponseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DisplayType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.IconType;

import java.util.HashMap;
import java.util.Map;

public class CertificationItemResponseHelper {

    enum CertificationItemResponse {
        ACCEPT(GuiStyleConstants.CLASS_APPROVAL_OUTCOME_ICON_APPROVED_COLORED, "text-success"),
        REVOKE(GuiStyleConstants.CLASS_APPROVAL_OUTCOME_ICON_REJECTED_COLORED, "text-danger"),
        REDUCE(GuiStyleConstants.CLASS_APPROVAL_OUTCOME_ICON_REJECTED_COLORED, "text-warning"),
        NOT_DECIDED(GuiStyleConstants.CLASS_APPROVAL_OUTCOME_ICON_REJECTED_COLORED, "text-warning"),
        DELEGATE(GuiStyleConstants.CLASS_APPROVAL_OUTCOME_ICON_FORWARDED_COLORED, "text-info"),
        NO_RESPONSE(GuiStyleConstants.CLASS_APPROVAL_OUTCOME_ICON_IN_PROGRESS_COLORED, "text-info");

        String iconCssClass;
        String colorCssClass;

        CertificationItemResponse(String iconCssClass, String colorCssClass) {
            this.iconCssClass = iconCssClass;
            this.colorCssClass = colorCssClass;
        }
    }

    private static final Map<AccessCertificationResponseType, CertificationItemResponse> RESPONSES_MAP = new HashMap<>();

    static {
        RESPONSES_MAP.put(AccessCertificationResponseType.ACCEPT, CertificationItemResponse.ACCEPT);
        RESPONSES_MAP.put(AccessCertificationResponseType.REVOKE, CertificationItemResponse.REVOKE);
        RESPONSES_MAP.put(AccessCertificationResponseType.REDUCE, CertificationItemResponse.REDUCE);
        RESPONSES_MAP.put(AccessCertificationResponseType.NOT_DECIDED, CertificationItemResponse.NOT_DECIDED);
        RESPONSES_MAP.put(AccessCertificationResponseType.DELEGATE, CertificationItemResponse.DELEGATE);
        RESPONSES_MAP.put(AccessCertificationResponseType.NO_RESPONSE, CertificationItemResponse.NO_RESPONSE);
    }

    AccessCertificationResponseType response;

    public CertificationItemResponseHelper(AccessCertificationResponseType response) {
        this.response = response;
    }

    public DisplayType getResponseDisplayType() {
        CertificationItemResponse itemResponse = RESPONSES_MAP.get(response);
        return new DisplayType()
                .label(LocalizationUtil.translateEnum(response))
                .cssClass(itemResponse.colorCssClass)
                .icon(new IconType().cssClass(itemResponse.iconCssClass));
    }

}

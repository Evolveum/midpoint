/*
 * Copyright (c) 2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.certification.helpers;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.progressbar.ProgressBar;
import com.evolveum.midpoint.gui.api.util.LocalizationUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationResponseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DisplayType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.IconType;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class CertificationItemResponseHelper {

    enum CertificationItemResponse {
        ACCEPT(GuiStyleConstants.CLASS_APPROVAL_OUTCOME_ICON_APPROVED_COLORED, "-success"),
        REVOKE(GuiStyleConstants.CLASS_APPROVAL_OUTCOME_ICON_REJECTED_COLORED, "-danger"),
        REDUCE(GuiStyleConstants.CLASS_APPROVAL_OUTCOME_ICON_REJECTED_COLORED, "-warning"),
        NOT_DECIDED(GuiStyleConstants.CLASS_APPROVAL_OUTCOME_ICON_REJECTED_COLORED, "-warning"),
        DELEGATE(GuiStyleConstants.CLASS_APPROVAL_OUTCOME_ICON_FORWARDED_COLORED, "-info"),
        NO_RESPONSE(GuiStyleConstants.CLASS_APPROVAL_OUTCOME_ICON_IN_PROGRESS_COLORED, "-info");

        String iconCssClass;
        String colorCssClassSuffix;

        CertificationItemResponse(String iconCssClass, String colorCssClassSuffix) {
            this.iconCssClass = iconCssClass;
            this.colorCssClassSuffix = colorCssClassSuffix;
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
                .cssClass(getTextCssClass())
                .icon(new IconType().cssClass(itemResponse.iconCssClass));
    }

    private String getTextCssClass() {
        CertificationItemResponse itemResponse = RESPONSES_MAP.get(response);
        return "text" + itemResponse.colorCssClassSuffix;
    }

    private String getBackgroundCssClass() {
        CertificationItemResponse itemResponse = RESPONSES_MAP.get(response);
        return "bg" + itemResponse.colorCssClassSuffix;
    }

    public ProgressBar.State getProgressBarState() {
        CertificationItemResponse itemResponse = RESPONSES_MAP.get(response);
        return Arrays.stream(ProgressBar.State.values())
                .filter(state -> state.getCssClass().equals(getBackgroundCssClass()))
                .findFirst()
                .orElse(ProgressBar.State.INFO);
    }

}

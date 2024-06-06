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
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractWorkItemOutputType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationResponseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DisplayType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.IconType;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class CertificationItemResponseHelper implements Serializable {

    enum CertificationItemResponse {
        ACCEPT(GuiStyleConstants.CLASS_CERT_OUTCOME_ICON_APPROVED, "-success", "AccessCertificationResponseType.ACCEPT.description"),
        REVOKE(GuiStyleConstants.CLASS_CERT_OUTCOME_ICON_REJECTED, "-danger", "AccessCertificationResponseType.REVOKE.description"),
        REDUCE(GuiStyleConstants.CLASS_CERT_OUTCOME_ICON_REDUCED, "-warning", "AccessCertificationResponseType.REDUCE.description"),
        NOT_DECIDED(GuiStyleConstants.CLASS_CERT_OUTCOME_ICON_NOT_DECIDED, "-secondary", "AccessCertificationResponseType.NOT_DECIDED.description"),
//        DELEGATE(GuiStyleConstants.CLASS_APPROVAL_OUTCOME_ICON_FORWARDED_COLORED, "-info"),
        NO_RESPONSE(GuiStyleConstants.CLASS_CERT_OUTCOME_ICON_NO_RESPONSE, "-light", "AccessCertificationResponseType.NO_RESPONSE.description");

        String iconCssClass;
        String colorCssClassSuffix;
        String help;

        CertificationItemResponse(String iconCssClass, String colorCssClassSuffix, String help) {
            this.iconCssClass = iconCssClass;
            this.help = help;
            this.colorCssClassSuffix = colorCssClassSuffix;
        }
    }

    private static final Map<AccessCertificationResponseType, CertificationItemResponse> RESPONSES_MAP = new HashMap<>();

    static {
        RESPONSES_MAP.put(AccessCertificationResponseType.ACCEPT, CertificationItemResponse.ACCEPT);
        RESPONSES_MAP.put(AccessCertificationResponseType.REVOKE, CertificationItemResponse.REVOKE);
        RESPONSES_MAP.put(AccessCertificationResponseType.REDUCE, CertificationItemResponse.REDUCE);
        RESPONSES_MAP.put(AccessCertificationResponseType.NOT_DECIDED, CertificationItemResponse.NOT_DECIDED);
//        RESPONSES_MAP.put(AccessCertificationResponseType.DELEGATE, CertificationItemResponse.DELEGATE);
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
                .help(getDocumentation())
                .cssClass(getTextCssClass())
                .icon(new IconType().cssClass(itemResponse.iconCssClass));
    }

    public String getTextCssClass() {
        CertificationItemResponse itemResponse = RESPONSES_MAP.get(response);
        return "text" + itemResponse.colorCssClassSuffix;
    }

    public String getBackgroundCssClass() {
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

    public String getLabelKey() {
        CertificationItemResponse itemResponse = RESPONSES_MAP.get(response);
        return "CertificationItemResponse." + itemResponse.name();
    }

    private String getDocumentation() {
        //todo get documentation from schema
//        var def = PrismContext.get().getSchemaRegistry().findItemDefinitionByElementName(AbstractWorkItemOutputType.F_OUTCOME);
//        return def != null ? def.getDocumentation() : null;
        CertificationItemResponse itemResponse = RESPONSES_MAP.get(response);
        return itemResponse.help;
    }
}

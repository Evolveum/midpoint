/*
 * Copyright (c) 2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.certification.helpers;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.progressbar.ProgressBar;
import com.evolveum.midpoint.gui.api.util.LocalizationUtil;
import com.evolveum.midpoint.gui.impl.component.action.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class CertificationItemResponseHelper implements Serializable {

    enum CertificationItemResponse {
        ACCEPT(GuiStyleConstants.CLASS_CERT_OUTCOME_ICON_APPROVED, "text-success",
                "bg-success", "AccessCertificationResponseType.ACCEPT.description"),
        REVOKE(GuiStyleConstants.CLASS_CERT_OUTCOME_ICON_REJECTED, "text-danger", "bg-danger", "AccessCertificationResponseType.REVOKE.description"),
        REDUCE(GuiStyleConstants.CLASS_CERT_OUTCOME_ICON_REDUCED, "text-warning", "bg-warning", "AccessCertificationResponseType.REDUCE.description"),
        NOT_DECIDED(GuiStyleConstants.CLASS_CERT_OUTCOME_ICON_NOT_DECIDED, "text-info", "bg-info", "AccessCertificationResponseType.NOT_DECIDED.description"),
        NO_RESPONSE(GuiStyleConstants.CLASS_CERT_OUTCOME_ICON_NO_RESPONSE, "", "bg-light", "AccessCertificationResponseType.NO_RESPONSE.description");

        final String iconCssClass;
        final String textColorCssClass;
        final String bgColorCssClass;
        final String help;

        CertificationItemResponse(String iconCssClass, String textColorCssClass, String backgroundColorCssClass, String help) {
            this.iconCssClass = iconCssClass;
            this.help = help;
            this.textColorCssClass = textColorCssClass;
            this.bgColorCssClass = backgroundColorCssClass;
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

    private static final Map<AccessCertificationResponseType, Class<? extends AbstractGuiAction<AccessCertificationWorkItemType>>>
            RESPONSES_GUI_ACTION_MAP = new HashMap<>();

    static {
        RESPONSES_GUI_ACTION_MAP.put(AccessCertificationResponseType.ACCEPT, CertItemAcceptAction.class);
        RESPONSES_GUI_ACTION_MAP.put(AccessCertificationResponseType.REVOKE, CertItemRevokeAction.class);
        RESPONSES_GUI_ACTION_MAP.put(AccessCertificationResponseType.REDUCE, CertItemReduceAction.class);
        RESPONSES_GUI_ACTION_MAP.put(AccessCertificationResponseType.NOT_DECIDED, CertItemNotDecidedAction.class);
        RESPONSES_GUI_ACTION_MAP.put(AccessCertificationResponseType.NO_RESPONSE, CertItemNoResponseAction.class);
    }

    AccessCertificationResponseType response;

    public CertificationItemResponseHelper(AccessCertificationResponseType response) {
        this.response = response;
    }

    public DisplayType getResponseDisplayType() {
        return new DisplayType()
                .label(LocalizationUtil.translateEnum(response))
                .help(getDocumentation())
                .cssClass(getTextCssClass())
                .icon(new IconType().cssClass(getIconCssClass()));
    }

    private String getIconCssClass() {
        // TODO what to do if no response was made?
        if (response == null) {
            return GuiStyleConstants.CLASS_CERT_OUTCOME_ICON_NO_RESPONSE;
        }
        CertificationItemResponse itemResponse = RESPONSES_MAP.get(response);
        return itemResponse.iconCssClass + " " + itemResponse.textColorCssClass;
    }

    public String getTextCssClass() {
        // TODO what to do if no response was made?
        if (response == null) {
            return "";
        }
        CertificationItemResponse itemResponse = RESPONSES_MAP.get(response);
        return itemResponse.textColorCssClass;
    }

    public String getBackgroundCssClass() {
        CertificationItemResponse itemResponse = RESPONSES_MAP.get(response);
        return itemResponse.bgColorCssClass;
    }

    public ProgressBar.State getProgressBarState() {
        CertificationItemResponse itemResponse = RESPONSES_MAP.get(response);
        return Arrays.stream(ProgressBar.State.values())
                .filter(state -> state.getCssClass().equals(getBackgroundCssClass()))
                .findFirst()
                .orElse(ProgressBar.State.SECONDARY);
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
        if (itemResponse == null) {
            return null;
        }
        return itemResponse.help;
    }

    public Class<? extends AbstractGuiAction<AccessCertificationWorkItemType>> getGuiActionForResponse() {
        return RESPONSES_GUI_ACTION_MAP.get(response);
    }

    public static AccessCertificationResponseType getResponseForGuiAction(Class<? extends AbstractGuiAction<?>> actionClass) {
        return RESPONSES_GUI_ACTION_MAP.entrySet().stream()
                .filter(entry -> entry.getValue().equals(actionClass))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);
    }
}

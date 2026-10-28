/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.component;

import java.io.Serializable;

import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.smart.api.info.AiInfo;
import com.evolveum.midpoint.smart.api.info.HealthStatus;
import com.evolveum.midpoint.web.component.dialog.ConfirmationWithOptionsDto;
import com.evolveum.midpoint.web.component.dialog.SuggestionOption;
import com.evolveum.midpoint.web.component.dialog.privacy.DataAccessPermission;
import com.evolveum.midpoint.web.component.util.Describable;

/** Smart Suggest configuration composed with the generic confirmation configuration. */
public record SmartSuggestConfirmationDto<T extends Describable>(
        ConfirmationWithOptionsDto<T> confirmation,
        IModel<AiInfo> aiInfo,
        boolean requireAiService) implements Serializable {

    public boolean isAiAvailable() {
        return isAiAvailable(aiInfo);
    }

    private static boolean isAiAvailable(IModel<AiInfo> aiInfo) {
        AiInfo info = aiInfo != null ? aiInfo.getObject() : null;
        return info != null && HealthStatus.OK.equals(info.status());
    }

    public void detach() {
        if (aiInfo != null) {
            aiInfo.detach();
        }
    }

    public static SmartSuggestConfirmationDto<DataAccessPermission> create(
            PageBase pageBase, SuggestionOption options, IModel<AiInfo> aiInfo) {
        return new SmartSuggestConfirmationDto<>(
                ConfirmationWithOptionsDto.<DataAccessPermission>builder()
                        .confirmationTitle(pageBase.createStringResource("SmartSuggestConfirmationPanel.title"))
                        .confirmationSubtitle(pageBase.createStringResource("SmartSuggestConfirmationPanel.subtitle"))
                        .confirmationOptionsTitle(pageBase.createStringResource(
                                "SmartSuggestConfirmationPanel.request.component.title"))
                        .externalLinkButtonLabel(pageBase.createStringResource("SmartSuggestConfirmationPanel.learnMore"))
                        .externalLinkUrl("https://docs.evolveum.com/")
                        .confirmationButtonLabel(pageBase.createStringResource("SmartSuggestConfirmationPanel.allowAndContinue"))
                        .cancelButtonLabel(pageBase.createStringResource("SmartSuggestConfirmationPanel.cancel"))
                        .confirmationOptions(options.confirmationOptions())
                        .errorMessage(() -> options.requiresAiService() && !isAiAvailable(aiInfo)
                                ? pageBase.createStringResource("SmartSuggestConfirmationPanel.serviceUnreachable.error").getString()
                                : null)
                        .warningMessage(() -> !options.requiresAiService() && !isAiAvailable(aiInfo)
                                ? pageBase.createStringResource("SmartSuggestConfirmationPanel.serviceUnreachable.warning").getString()
                                : null)
                        .build(), aiInfo, options.requiresAiService());
    }
}

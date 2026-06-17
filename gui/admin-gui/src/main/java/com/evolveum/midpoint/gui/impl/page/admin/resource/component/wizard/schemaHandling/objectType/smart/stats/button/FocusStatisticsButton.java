/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.stats.button;

import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.stats.action.FocusStatisticsActions;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;

import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;

import javax.xml.namespace.QName;
import java.io.Serial;

/**
 * UI button panel for displaying or regenerating focus object statistics (e.g. UserType).
 *
 * <p>If statistics exist, they are displayed in a popup. Otherwise (or if regeneration is forced),
 * a background task is started and a progress popup is shown.</p>
 */
public class FocusStatisticsButton extends AbstractStatisticsButton<QName> {

    @Serial private static final long serialVersionUID = 1L;

    private final IModel<String> resourceOidModel;
    private final IModel<ShadowKindType> kindModel;
    private final IModel<String> intentModel;

    public FocusStatisticsButton(
            String id,
            IModel<QName> focusObjectTypeNameModel,
            IModel<String> resourceOidModel,
            IModel<ShadowKindType> kindModel,
            IModel<String> intentModel) {
        super(id, focusObjectTypeNameModel);
        this.resourceOidModel = resourceOidModel;
        this.kindModel = kindModel;
        this.intentModel = intentModel;
    }

    @Override
    protected IModel<String> getMainButtonLabel() {
        return isRegenerateMode()
                ? createStringResource("SmartStatisticsPanel.regenerateStatistics")
                : createStringResource("FocusStatisticsButton.processStatistics");
    }

    @Override
    protected void onButtonClick(AjaxRequestTarget target) {
        QName focusType = getModelObject();
        String resourceOid = resourceOidModel != null ? resourceOidModel.getObject() : null;
        ShadowKindType kind = kindModel != null ? kindModel.getObject() : null;
        String intent = intentModel != null ? intentModel.getObject() : null;

        if (focusType == null) {
            getPageBase().warn("Focus object type is not specified.");
            target.add(getPageBase().getFeedbackPanel());
            return;
        }

        if (resourceOid == null || kind == null || intent == null) {
            getPageBase().warn("Resource, kind, and intent must be specified for focus statistics.");
            target.add(getPageBase().getFeedbackPanel());
            return;
        }

        FocusStatisticsActions.handleClick(
                target,
                getPageBase(),
                getPageBase().getSmartIntegrationService(),
                focusType,
                resourceOid,
                kind,
                intent,
                getPreselectedAttribute(),
                forceRegeneration());
    }

    protected ItemPathType getPreselectedAttribute() {
        return null;
    }
}

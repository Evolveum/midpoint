/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.stats.button;

import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.stats.action.ObjectTypeStatisticsActions;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeIdentification;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;

import java.io.Serial;

/**
 * UI button panel for displaying or regenerating object type statistics.
 *
 * <p>When clicked, the button tries to load the latest statistics for the given
 * resource object class. If statistics exist, they are displayed in a popup.
 * If they do not exist (or regeneration is forced), a background task is started
 * to compute new statistics and a progress popup is shown.</p>
 *
 * <p>After the computation finishes, the newly generated statistics are
 * automatically displayed.</p>
 */
public class ObjectTypeStatisticsButton extends AbstractStatisticsButton<ResourceObjectTypeIdentification> {

    @Serial private static final long serialVersionUID = 1L;

    private final String resourceOid;

    public ObjectTypeStatisticsButton(
            String id,
            IModel<ResourceObjectTypeIdentification> resourceObjectTypeIdentifier,
            String resourceOid) {
        super(id, resourceObjectTypeIdentifier);
        this.resourceOid = resourceOid;
    }

    @Override
    protected IModel<String> getMainButtonLabel() {
        return isRegenerateMode()
                ? createStringResource("SmartStatisticsPanel.regenerateStatistics")
                : createStringResource("ObjectClassStatisticsButton.processStatistics");
    }

    @Override
    protected void onButtonClick(AjaxRequestTarget target) {
        ObjectTypeStatisticsActions.handleClick(
                target,
                getPageBase(),
                getPageBase().getSmartIntegrationService(),
                resourceOid,
                getModelObject(),
                null,
                forceRegeneration());
    }
}

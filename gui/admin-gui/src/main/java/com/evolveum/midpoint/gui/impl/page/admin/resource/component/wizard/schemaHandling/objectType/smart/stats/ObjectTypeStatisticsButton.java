/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.stats;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeIdentification;
import com.evolveum.midpoint.web.component.AjaxIconButton;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;

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
public class ObjectTypeStatisticsButton extends BasePanel<ResourceObjectTypeIdentification> {

    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_PROCESS_STATISTICS_BUTTON = "processStatisticsButton";

    private final String resourceOid;

    public ObjectTypeStatisticsButton(String id, IModel<ResourceObjectTypeIdentification> resourceObjectTypeIdentifier, String resourceOid) {
        super(id, resourceObjectTypeIdentifier);
        this.resourceOid = resourceOid;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        AjaxIconButton processStatisticsButton = new AjaxIconButton(
                ID_PROCESS_STATISTICS_BUTTON,
                Model.of("fa-solid fa-chart-bar"),
                getMainButtonLabel()) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                handleClick(target);
            }
        };
        processStatisticsButton.showTitleAsLabel(true);
        add(processStatisticsButton);
    }

    protected IModel<String> getMainButtonLabel() {
        return createStringResource("ObjectClassStatisticsButton.processStatistics");
    }

    private void handleClick(@NotNull AjaxRequestTarget target) {
        PageBase page = getPageBase();
        ObjectTypeStatisticsActions.handleClick(
                target,
                page,
                page.getSmartIntegrationService(),
                resourceOid,
                getModelObject(),
                null,
                forceRegeneration());
    }

    protected boolean forceRegeneration() {
        return false;
    }

}

/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.stats;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.smart.api.SmartIntegrationService;
import com.evolveum.midpoint.web.component.AjaxIconButton;

import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.io.Serial;

/**
 * UI button panel for displaying or regenerating focus object statistics (e.g. UserType).
 *
 * <p>If statistics exist, they are displayed in a popup. Otherwise (or if regeneration is forced),
 * a background task is started and a progress popup is shown.</p>
 */
public class FocusStatisticsButton extends BasePanel<QName> {

    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_PROCESS_STATISTICS_BUTTON = "processStatisticsButton";

    public FocusStatisticsButton(String id, IModel<QName> focusObjectTypeNameModel) {
        super(id, focusObjectTypeNameModel);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        AjaxIconButton button = new AjaxIconButton(
                ID_PROCESS_STATISTICS_BUTTON,
                Model.of("fa-solid fa-chart-bar"),
                getMainButtonLabel()) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                handleClick(target);
            }
        };
        button.showTitleAsLabel(true);
        add(button);
    }

    protected IModel<String> getMainButtonLabel() {
        return createStringResource("FocusStatisticsButton.processStatistics");
    }

    protected boolean forceRegeneration() {
        return false;
    }

    protected ItemPathType getPreselectedAttribute() {
        return null;
    }

    private void handleClick(@NotNull AjaxRequestTarget target) {
        PageBase page = getPageBase();
        SmartIntegrationService sis = page.getSmartIntegrationService();

        QName focusType = getModelObject();
        if (focusType == null) {
            page.warn("Focus object type is not specified.");
            target.add(page.getFeedbackPanel());
            return;
        }

        FocusStatisticsActions.handleClick(
                target,
                page,
                sis,
                focusType,
                getPreselectedAttribute(),
                forceRegeneration());
    }
}

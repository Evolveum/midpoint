/*
 * Copyright (c) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.resource.component;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismPropertyWrapper;

import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.util.exception.SchemaException;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;

public class ToggleSimulationModePanel extends BasePanel<PrismPropertyWrapper<String>> {

    private static final Trace LOGGER = TraceManager.getTrace(ToggleSimulationModePanel.class);

    private static final String ID_MODE = "mode";
    private static final String ID_SWITCH_TO_DEV = "switchToDev";
    private static final String ID_SWITCH_TO_PROD = "switchToProd";

    public ToggleSimulationModePanel(String id, IModel<PrismPropertyWrapper<String>> model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        setOutputMarkupId(true);

        Label label = new Label(ID_MODE, createLabelModel());
        label.setOutputMarkupId(true);
        add(label);

        AjaxIconButton toggleToProduction = new AjaxIconButton(ID_SWITCH_TO_PROD, Model.of(GuiStyleConstants.ClASS_ICON_TOOGLE),
                createStringResource("OperationalButtonsPanel.button.toggleToProduction")) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                updateState(SchemaConstants.LIFECYCLE_ACTIVE);
                target.add(ToggleSimulationModePanel.this);
                target.add(label);
            }
        };
        toggleToProduction.setOutputMarkupId(true);
        toggleToProduction.showTitleAsLabel(true);
        toggleToProduction.add(new VisibleBehaviour(() -> isToggleModeButtonVisible(SchemaConstants.LIFECYCLE_ACTIVE)));
        add(toggleToProduction);

        AjaxIconButton toggleToDevelopment = new AjaxIconButton(ID_SWITCH_TO_DEV, Model.of(GuiStyleConstants.ClASS_ICON_TOOGLE),
                createStringResource("OperationalButtonsPanel.button.toggleToDevelopment")) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                updateState(SchemaConstants.LIFECYCLE_PROPOSED);
                target.add(ToggleSimulationModePanel.this);
                target.add(label);
            }
        };
        toggleToDevelopment.setOutputMarkupId(true);
        toggleToDevelopment.showTitleAsLabel(true);
        toggleToDevelopment.add(new VisibleBehaviour(() -> isToggleModeButtonVisible(SchemaConstants.LIFECYCLE_PROPOSED)));
        add(toggleToDevelopment);

    }

    private IModel<String> createLabelModel() {
        return () -> {
            String key = "SimulationMode.undefined";
            try {
                String lifecycle = getModelObject().getValue().getRealValue();
                if (StringUtils.isEmpty(lifecycle)) {
                    lifecycle = SchemaConstants.LIFECYCLE_ACTIVE;
                }

                if (SchemaConstants.LIFECYCLE_ACTIVE.equals(lifecycle)
                        || SchemaConstants.LIFECYCLE_PROPOSED.equals(lifecycle)) {
                    key = "SimulationMode." + lifecycle;
                }
            } catch (SchemaException e) {
                LOGGER.error("Couldn't get value from " + getModelObject(), e);
            }
            return getString(key);
        };
    }

    private void updateState(String lifecycleActive) {
        try {
            getModelObject().getValue().setRealValue(lifecycleActive);
        } catch (SchemaException e) {
            LOGGER.error("Couldn't get value from " + getModelObject(), e);
        }
    }

    private boolean isToggleModeButtonVisible(@NotNull String expectedLifecycleState) {
        try {
            String lifecycleState = getModelObject().getValue().getRealValue();
            if (StringUtils.isEmpty(lifecycleState)) {
                lifecycleState = SchemaConstants.LIFECYCLE_ACTIVE;
            }

            return !expectedLifecycleState.equals(lifecycleState);
        } catch (SchemaException e) {
            LOGGER.error("Couldn't get value from " + getModelObject(), e);
        }
        return false;
    }
}

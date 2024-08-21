/*
 * Copyright (c) 2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.component.action;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.page.admin.certification.component.ActionConfigurationPanel;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ItemDeltaCollectionsUtil;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.ActionType;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;

import java.io.Serial;
import java.io.Serializable;
import java.util.*;

public abstract class AbstractGuiAction<C extends Containerable> implements Serializable {

    private static final Trace LOGGER = TraceManager.getTrace(AbstractGuiAction.class);

    private GuiActionType guiActionType;

    public AbstractGuiAction() {
    }

    public AbstractGuiAction(@NotNull GuiActionType guiActionType) {
        this.guiActionType = guiActionType;
    }

    /**
     * Executes the action. In case panel is defined, it is shown in the main popup.
     * @param objectsToProcess
     * @param pageBase
     * @param target
     */
    public void onActionPerformed(List<C> objectsToProcess, PageBase pageBase, AjaxRequestTarget target) {
        if (guiActionType == null) {
            executeAction(objectsToProcess, pageBase, target);
            return;
        }

        ContainerPanelConfigurationType panel = guiActionType.getPanel();
        if (panel != null) {
            showActionConfigurationPanel(panel, objectsToProcess, pageBase, target);
            return;
        }
        executeAction(objectsToProcess, pageBase, target);

    }

    protected void showActionConfigurationPanel(ContainerPanelConfigurationType panelConfig, List<C> objectsToProcess,
            PageBase pageBase, AjaxRequestTarget target) {
        ActionConfigurationPanel panel = new ActionConfigurationPanel(pageBase.getMainPopupBodyId(), Model.of(panelConfig)) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected void confirmPerformedWithDeltas(AjaxRequestTarget target, Collection<ItemDelta<?, ?>> deltas) {
                confirmActionPerformed(target, objectsToProcess, deltas, pageBase);
            }
        };
        pageBase.showMainPopup(panel, target);
    }

    protected void confirmActionPerformed(AjaxRequestTarget target, List<C> objectsToProcess,
            Collection<ItemDelta<?, ?>> deltas, PageBase pageBase) {
        for (C objectToProcess : objectsToProcess) {
            try {
                ItemDeltaCollectionsUtil.applyTo(deltas, objectToProcess.asPrismContainerValue());
            } catch (SchemaException e) {
                LOGGER.error("Cannot apply deltas to object {}", objectToProcess, e);
            }
        }

        executeAction(objectsToProcess, pageBase, target);
        pageBase.hideMainPopup(target);
    }

    protected abstract void executeAction(List<C> objectsToProcess, PageBase pageBase, AjaxRequestTarget target);

    public boolean isButton() {
        ActionType actionType = AbstractGuiAction.this.getClass().getAnnotation(ActionType.class);
        return actionType != null && actionType.button();
    }

    public DisplayType getActionDisplayType() {
        DisplayType configuredDisplay = guiActionType == null ? null : guiActionType.getDisplay();
        ActionType actionType = AbstractGuiAction.this.getClass().getAnnotation(ActionType.class);
        PanelDisplay display = actionType != null ? actionType.display() : null;
        DisplayType pageDisplay = null;
        if (display != null) {
            pageDisplay = new DisplayType()
                    .label(display.label())
                    .icon(new IconType()
                            .cssClass(display.icon()));
        }
        if (configuredDisplay == null) {
            return pageDisplay;
        }
        if (pageDisplay == null) {
            return configuredDisplay;
        }
        MiscSchemaUtil.mergeDisplay(configuredDisplay, pageDisplay);
        return configuredDisplay;
    }

    public boolean isVisible(C rowObject) {
        return isConfiguredVisibility() && isVisibleForRow(rowObject);
    }

    private boolean isConfiguredVisibility() {
        return guiActionType == null || WebComponentUtil.getElementVisibility(guiActionType.getVisibility());
    }

    protected boolean isVisibleForRow(C rowObject) {
        return true;
    }

    public List<String> getParameterNameList() {
        ActionType actionType = AbstractGuiAction.this.getClass().getAnnotation(ActionType.class);
        if (actionType!= null && actionType.parameterName() != null) {
            return List.of(actionType.parameterName());
        }
        return new ArrayList<>();
    }

    public boolean isBulkAction() {
        ActionType actionType = AbstractGuiAction.this.getClass().getAnnotation(ActionType.class);
        return actionType != null && actionType.bulkAction();
    }

    public int getOrder() {
        ActionType actionType = AbstractGuiAction.this.getClass().getAnnotation(ActionType.class);
        PanelDisplay display = actionType != null ? actionType.display() : null;
        return display != null ? display.order() : 0;
    }
}

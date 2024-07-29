/*
 * Copyright (c) 2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.component.action;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.LocalizationUtil;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.page.admin.certification.component.ConfirmationPanelWithComment;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ItemDeltaCollectionsUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.application.ActionType;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;

import java.io.Serial;
import java.io.Serializable;
import java.util.*;

public abstract class AbstractGuiAction<C extends Containerable> implements Serializable {

    private GuiActionType guiActionType;

    public AbstractGuiAction() {
    }

    public AbstractGuiAction(@NotNull GuiActionType guiActionType) {
        this.guiActionType = guiActionType;
    }

    public void onActionPerformed(List<C> objectsToProcess, PageBase pageBase, AjaxRequestTarget target) {
        onActionPerformed(objectsToProcess, true, pageBase, target);
    }

    /**
     * Executes the action. If preAction is set, it will be executed first.
     * This method should be called for main action with preActionPerform set to false (after pre-action was performed)
     * @param objectsToProcess
     * @param preActionPerform
     * @param pageBase
     * @param target
     */
    public void onActionPerformed(List<C> objectsToProcess, boolean preActionPerform, PageBase pageBase, AjaxRequestTarget target) {
        if (guiActionType == null) {
            executeAction(objectsToProcess, pageBase, target);
            return;
        }

        GuiConfirmationActionType confirmation = guiActionType.getConfirmation();
        if (confirmation != null) {
            runConfirmation(confirmation, objectsToProcess, pageBase, target);
            return;
        }
        executeAction(objectsToProcess, pageBase, target);

    }

    private void runConfirmation(GuiConfirmationActionType confirmation, List<C> objectsToProcess, PageBase pageBase, AjaxRequestTarget target) {

        ConfirmationPanelWithComment confirmationPanel = new ConfirmationPanelWithComment(pageBase.getMainPopupBodyId(),
                getConfirmationModel(confirmation),
                () -> confirmation,
                (List<Containerable>) objectsToProcess) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected void yesPerformedWithComment(AjaxRequestTarget target, Collection<ItemDelta<?, ?>> deltas) {
                for (C objectToProcess : objectsToProcess) {
                    try {
                        ItemDeltaCollectionsUtil.applyTo(deltas, objectToProcess.asPrismContainerValue());
                    } catch (SchemaException e) {
                        throw new RuntimeException(e);
                        //TODO error handling
                    }
                }

                executeAction(objectsToProcess, pageBase, target);
                pageBase.hideMainPopup(target);

            }

            @Override
            public void noPerformed(AjaxRequestTarget target) {
                pageBase.hideMainPopup(target);
            }
        };
        pageBase.showMainPopup(confirmationPanel, target);
    }

    private IModel<String> getConfirmationModel(GuiConfirmationActionType confirmation) {
        return () -> LocalizationUtil.translatePolyString(confirmation.getMessage());
    }

    protected abstract void executeAction(List<C> objectsToProcess, PageBase pageBase, AjaxRequestTarget target);

    public boolean isButton() {
        ActionType actionType = AbstractGuiAction.this.getClass().getAnnotation(ActionType.class);
        return actionType != null && actionType.button();
    }

    public DisplayType getActionDisplayType() {
        //TODO probably there should be merging of those two
        DisplayType displayType = guiActionType == null ? null : guiActionType.getDisplay();
        if (displayType != null) {
            return displayType;
        }
        ActionType actionType = AbstractGuiAction.this.getClass().getAnnotation(ActionType.class);
        PanelDisplay display = actionType != null ? actionType.display() : null;
        if (display == null) {
            return null;
        }
        return new DisplayType()
                .label(display.label())
                .icon(new IconType()
                        .cssClass(display.icon()));
    }

    public boolean isVisible(C rowObject) {
        return isConfiguredVisibility() && isVisibleForRow(rowObject);
    }

    private boolean isConfiguredVisibility() {
        return guiActionType != null || WebComponentUtil.getElementVisibility(guiActionType.getVisibility());
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

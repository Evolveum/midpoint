/*
 * Copyright (c) 2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.action;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.GuiDisplayTypeUtil;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.web.application.ActionType;

import com.evolveum.midpoint.web.component.dialog.ConfirmationPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DisplayType;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import java.io.Serial;
import java.util.List;

@ActionType(
        identifier = "confirm")
public class ConfirmAction<C extends Containerable, AGA extends AbstractGuiAction<C>> extends AbstractGuiAction<C>
        implements PreAction<C, AGA> {

    public ConfirmAction() {
        super();
    }

    @Override
    public void executeAction(List<C> objectsToProcess, PageBase pageBase, AjaxRequestTarget target) {
        showConfirmationPanel(null, objectsToProcess, pageBase, target);
    }

    @Override
    public void onActionPerformed(AGA mainAction, List<C> objectsToProcess, PageBase pageBase, AjaxRequestTarget target) {
        showConfirmationPanel(mainAction, objectsToProcess, pageBase, target);
    }

    private void showConfirmationPanel(AGA mainAction, List<C> objectsToProcess, PageBase pageBase, AjaxRequestTarget target) {
        ConfirmationPanel confirmationPanel = new ConfirmationPanel(pageBase.getMainPopupBodyId(), getConfirmationModel()) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void yesPerformed(AjaxRequestTarget target) {
                if (mainAction != null) {
                    mainAction.onActionPerformed(objectsToProcess, false, pageBase, target);
                }
                pageBase.hideMainPopup(target);
            }

            @Override
            public void noPerformed(AjaxRequestTarget target) {
                pageBase.hideMainPopup(target);
            }
        };
        pageBase.showMainPopup(confirmationPanel, target);
    }

    private IModel<String> getConfirmationModel() {
        DisplayType display = getActionDisplayType();
        return Model.of(GuiDisplayTypeUtil.getTranslatedLabel(display));
    }
}

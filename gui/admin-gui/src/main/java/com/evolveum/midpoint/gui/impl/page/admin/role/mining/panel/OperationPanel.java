/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.panel;

import java.io.Serial;

import com.evolveum.midpoint.web.component.dialog.DeleteConfirmationPanel;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.dialog.ConfirmationPanel;

public class OperationPanel extends BasePanel<String> {

    private static final String ID_BUTTONS = "buttons";
    private static final String ID_PANEL_BUTTONS = "panelButtons";


    public OperationPanel(String id) {
        super(id);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        RepeatingView repeatingView = new RepeatingView(ID_BUTTONS);
        add(repeatingView);
        createBackButton(repeatingView);
        createDeleteButton(repeatingView);

        RepeatingView panelButton = new RepeatingView(ID_PANEL_BUTTONS);
        add(panelButton);

        addPanelButton(panelButton);

    }

    protected void addPanelButton(RepeatingView repeatingView) {

    }


    private void createDeleteButton(RepeatingView repeatingView) {
        AjaxIconButton remove = new AjaxIconButton(repeatingView.newChildId(), Model.of(GuiStyleConstants.CLASS_ICON_REMOVE),
                getPageBase().createStringResource("OperationalButtonsPanel.delete")) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                deletePerformed(ajaxRequestTarget);
            }
        };
        remove.showTitleAsLabel(true);
        remove.add(AttributeAppender.append("class", "btn btn-danger btn-sm"));
        repeatingView.add(remove);
    }

    private void deletePerformed(AjaxRequestTarget target) {
        ConfirmationPanel confirmationPanel = new DeleteConfirmationPanel(getPageBase().getMainPopupBodyId(),
                createStringResource("OperationalButtonsPanel.deletePerformed")) {
            private static final long serialVersionUID = 1L;

            @Override
            public void yesPerformed(AjaxRequestTarget target) {
                executeDelete(target);
            }
        };
        getPageBase().showMainPopup(confirmationPanel, target);
    }

    protected void executeDelete(AjaxRequestTarget target){

    }
    private void createBackButton(RepeatingView repeatingView) {
        AjaxIconButton back = new AjaxIconButton(repeatingView.newChildId(), Model.of(GuiStyleConstants.ARROW_LEFT),
                getPageBase().createStringResource("pageAdminFocus.button.back")) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                backPerformed(ajaxRequestTarget);
            }
        };

        back.showTitleAsLabel(true);
        back.add(AttributeAppender.append("class", "btn btn-default btn-sm"));
        repeatingView.add(back);
    }

    protected void backPerformed(AjaxRequestTarget target) {
        PageBase page = getPageBase();

        ConfirmationPanel confirmationPanel = new ConfirmationPanel(page.getMainPopupBodyId(),
                page.createStringResource("OperationalButtonsPanel.confirmBack")) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void yesPerformed(AjaxRequestTarget target) {
                backPerformedConfirmed();
            }

        };

        page.showMainPopup(confirmationPanel, target);
    }

    protected void backPerformedConfirmed() {
        getPageBase().redirectBack();
    }
}

/*
 * Copyright (c) 2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.component.action;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.GuiDisplayTypeUtil;
import com.evolveum.midpoint.prism.Containerable;

import com.evolveum.midpoint.web.application.ActionType;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.gui.impl.page.admin.certification.component.CommentPanel;

import com.evolveum.midpoint.xml.ns._public.common.common_3.DisplayType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.GuiActionType;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import java.io.Serial;
import java.util.List;

//TODO probably not needed anymore. should be more generic -> confirmation popup + list of attributes to fill
@ActionType(
        identifier = "comment",
        parameterName = {"comment"},
        display = @PanelDisplay(label = "CommentPanel.title"))
public class CommentAction<C extends Containerable, AGA extends AbstractGuiAction<C>> extends AbstractGuiAction<C>
        implements PreAction<C, AGA> {

    public CommentAction() {
        super();
    }

    public CommentAction(GuiActionType actionDto) {
        super(actionDto);
    }

    @Override
    public void executeAction(List<C> objectsToProcess, PageBase pageBase, AjaxRequestTarget target) {
        showCommentPanel(null, objectsToProcess, pageBase, target);
    }

    @Override
    public void onActionPerformed(AGA mainAction, List<C> objectsToProcess, PageBase pageBase, AjaxRequestTarget target) {
        showCommentPanel(mainAction, objectsToProcess, pageBase, target);
    }

    private void showCommentPanel(AGA mainAction, List<C> objectsToProcess, PageBase pageBase, AjaxRequestTarget target) {
        CommentPanel commentPanel = new CommentPanel(pageBase.getMainPopupBodyId(), Model.of()) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected void savePerformed(AjaxRequestTarget target, String comment) {
//                addParameterValue("comment", comment);
                commentActionPerformed(objectsToProcess, comment, target);
                if (mainAction != null) {
                    mainAction.onActionPerformed(objectsToProcess, false, pageBase, target);
                }
                pageBase.hideMainPopup(target);
            }

            @Override
            protected IModel<String> createInformationLabelModel() {
                return getCommentPanelInformationLabelModel();
            }

            protected boolean isCommentRequired() {
                return false;
//                return CommentAction.this.isParameterMandatory("comment");
            }
        };
        pageBase.showMainPopup(commentPanel, target);
    }

    protected void commentActionPerformed(List<C> objectsToProcess, String comment, AjaxRequestTarget target) {
    }

    protected IModel<String> getCommentPanelInformationLabelModel() {
        DisplayType display = getActionDisplayType();
        return Model.of(GuiDisplayTypeUtil.getTranslatedLabel(display));
    }
}

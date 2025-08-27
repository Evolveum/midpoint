/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 *    This work is dual-licensed under the Apache License 2.0
 *    and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.dialog;

import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.web.component.AjaxButton;

import java.io.Serial;

/**
 * Created by Honchar.
 *
 * class is created based on the ConfirmationDialog. ConfirmationPanel panel is
 * to be added to main popup (from PageBase class) as a content
 */
public class ConfirmationPanel extends BasePanel<String> implements Popupable {

    @Serial private static final long serialVersionUID = 1L;
    private static final String ID_CONFIRM_TEXT = "confirmText";

    private static final String ID_BUTTONS = "buttons";
    protected static final String ID_YES = "yes";
    private static final String ID_NO = "no";

    private Fragment footer;

    public ConfirmationPanel(String id) {
        this(id, null);
    }

    public ConfirmationPanel(String id, IModel<String> message) {
        super(id, message);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    protected void initLayout() {
        Label label = new Label(ID_CONFIRM_TEXT, getModel());
        label.setEscapeModelStrings(true);
        add(label);

        footer = initFooter();

        customInitLayout(footer);
    }

    private Fragment initFooter() {
        footer = new Fragment(Popupable.ID_FOOTER, ID_BUTTONS, this);

        footer.add(createYesButton());

        AjaxButton noButton = new AjaxButton(ID_NO, createNoLabel()) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                noPerformed(target);
            }
        };
        noButton.add(AttributeAppender.append("class", getNoButtonCssClass()));
        footer.add(noButton);

        return footer;
    }

    protected Component createYesButton() {
        AjaxButton yesButton = new AjaxButton(ID_YES, createYesLabel()) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                // todo this is wrong, this panel shouldn't know about it being used as content for dialog, also this hideMainPopup should be probably part of yesPerformed
                getPageBase().hideMainPopup(target);
                yesPerformed(target);
            }
        };
        yesButton.add(AttributeAppender.append("class", getYesButtonCssClass()));
        yesButton.add(new VisibleBehaviour(this::isYesButtonVisible));
        return yesButton;
    }

    protected boolean isYesButtonVisible() {
        return true;
    }

    @Override
    public Component getFooter() {
        return footer;
    }

    /**
     * this is not good way to extend confirmation panel.
     * whole html of parent panel has to be copied and maintained (it should be "internal" thing).
     *
     * @param panel
     */
    @Deprecated
    protected void customInitLayout(WebMarkupContainer panel) {

    }

    public void yesPerformed(AjaxRequestTarget target) {
    }

    public void noPerformed(AjaxRequestTarget target) {
        // todo this is wrong, this panel shouldn't know about it being used as content for dialog
        getPageBase().hideMainPopup(target);
    }

    @Override
    public int getWidth() {
        return 350;
    }

    @Override
    public int getHeight() {
        return 150;
    }

    @Override
    public String getWidthUnit() {
        return "px";
    }

    @Override
    public String getHeightUnit() {
        return "px";
    }

    @Override
    public IModel<String> getTitle() {
        return createStringResource("pageUsers.message.confirmActionPopupTitle");
    }

    @Override
    public Component getContent() {
        return this;
    }

    protected IModel<String> createYesLabel() {
        return createStringResource("confirmationDialog.yes");
    }

    protected IModel<String> createNoLabel() {
        return createStringResource("confirmationDialog.no");
    }

    protected String getYesButtonCssClass() {
        return "btn btn-primary";
    }

    protected String getNoButtonCssClass() {
        return "btn btn-default";
    }
}

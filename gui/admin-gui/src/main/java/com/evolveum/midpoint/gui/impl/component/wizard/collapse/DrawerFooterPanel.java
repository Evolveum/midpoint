/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.component.wizard.collapse;

import java.io.Serial;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

public class DrawerFooterPanel extends BasePanel<Void> {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final String ID_BUTTONS = "buttons";
    private static final String ID_REPEATER = "repeater";

    public DrawerFooterPanel(String id) {
        super(id);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        setOutputMarkupId(true);
        setOutputMarkupPlaceholderTag(true);

        add(createFooterPanel());
    }

    protected Component createFooterPanel() {
        WebMarkupContainer footerPanel =
                new WebMarkupContainer(ID_BUTTONS);

        footerPanel.setOutputMarkupId(true);
        footerPanel.setOutputMarkupPlaceholderTag(true);
        footerPanel.add(new VisibleBehaviour(this::isFooterPanelVisible));

        RepeatingView repeater = new RepeatingView(ID_REPEATER);
        customizeFooterButtons(repeater);
        footerPanel.add(repeater);

        return footerPanel;
    }

    protected void customizeFooterButtons(RepeatingView repeater) {
        repeater.add(createNoButton(repeater.newChildId()));
        repeater.add(createYesButton(repeater.newChildId()));
    }

    protected Component createNoButton(String id) {
        AjaxButton noButton = new AjaxButton(id, createNoLabel()) {

            @Serial
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                noPerformed(target);
            }
        };

        noButton.add(
                AttributeAppender.append("class", getNoButtonCssClass()));

        return noButton;
    }

    protected Component createYesButton(String id) {
        AjaxIconButton yesButton = new AjaxIconButton(
                id,
                Model.of("fa fa-check"),
                createYesLabel()) {

            @Serial
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                yesPerformed(target);
            }
        };

        yesButton.showTitleAsLabel(true);
        yesButton.add(
                AttributeAppender.append("class", getYesButtonCssClass()));
        yesButton.add(
                new VisibleBehaviour(this::isYesButtonVisible));

        return yesButton;
    }

    protected IModel<String> createYesLabel() {
        return createStringResource("ContainerDrawerPanel.yes");
    }

    protected IModel<String> createNoLabel() {
        return createStringResource("ContainerDrawerPanel.no");
    }

    protected String getYesButtonCssClass() {
        return "btn btn-primary ms-auto";
    }

    protected String getNoButtonCssClass() {
        return "btn btn-link";
    }

    protected boolean isYesButtonVisible() {
        return true;
    }

    protected boolean isFooterPanelVisible() {
        return true;
    }

    public void yesPerformed(AjaxRequestTarget target) {
        hideDrawer(target);
    }

    public void noPerformed(AjaxRequestTarget target) {
        hideDrawer(target);
    }

    protected void hideDrawer(AjaxRequestTarget target) {
        getPageBase().hideDrawer(target);
    }
}

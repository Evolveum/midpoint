/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.component.wizard;

import java.io.Serial;

import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.AssignmentHolderDetailsModel;

import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.togglebutton.ToggleIconButton;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

/**
 * The Wizard step panel has a header and style similar to a standard Form Wizard step panel,
 * but within the body we can define a custom panel.
 */
public abstract class CustomFormStepPanel<AHDM extends AssignmentHolderDetailsModel> extends AbstractWizardStepPanel<AHDM> {

    @Serial private static final long serialVersionUID = 1L;

    public static final String PANEL_TYPE = "rw-attributes-inbound-range";

    private static final String ID_EXPAND_COLLAPSE_BUTTON = "expandCollapseButton";
    private static final String ID_HEADER = "header";
    private static final String ID_HEADER_LABEL = "headerLabel";
    private static final String ID_PANEL = "panel";

    private boolean expanded = true;

    public CustomFormStepPanel(AHDM model) {
        super(model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        setOutputMarkupId(true);
        add(createHeaderPanel());
        WebMarkupContainer panel = createPanel(ID_PANEL);
        panel.setOutputMarkupId(true);
        panel.add(new VisibleBehaviour(() -> expanded));
        add(panel);
    }

    protected abstract WebMarkupContainer createPanel(String idPanel);

    private WebMarkupContainer createHeaderPanel() {
        WebMarkupContainer header = new WebMarkupContainer(ID_HEADER);
        header.add(new AjaxEventBehavior("click") {

            @Override
            protected void onEvent(AjaxRequestTarget target) {
                onHeaderClick(target);
            }
        });

        header.add(createExpandCollapseButton());
        header.add(new Label(ID_HEADER_LABEL, getTitle()));
        header.setOutputMarkupId(true);

        return header;
    }

    private ToggleIconButton createExpandCollapseButton() {
        ToggleIconButton<?> expandCollapseButton = new ToggleIconButton<Void>(ID_EXPAND_COLLAPSE_BUTTON,
                GuiStyleConstants.CLASS_ICON_EXPAND_CONTAINER, GuiStyleConstants.CLASS_ICON_COLLAPSE_CONTAINER) {

            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
            }

            @Override
            public boolean isOn() {
                return expanded;
            }
        };
        expandCollapseButton.setOutputMarkupId(true);
        return expandCollapseButton;
    }

    private void onHeaderClick(AjaxRequestTarget target) {
        expanded = !expanded;
        refreshPanel(target);
    }

    private void refreshPanel(AjaxRequestTarget target) {
        target.add(get(ID_HEADER));
        target.add(get(ID_PANEL));
        target.add(CustomFormStepPanel.this);
    }
}

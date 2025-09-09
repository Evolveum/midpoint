/*
 * Copyright (c) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component;

import com.evolveum.midpoint.gui.api.component.BasePanel;

import com.evolveum.midpoint.gui.impl.component.ButtonBar;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.web.component.util.SelectableRow;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;
import org.jetbrains.annotations.NotNull;

import java.io.Serial;
import java.util.List;

/**
 * Panel displayed when no resource object specific values are not yet defined.
 * <p>
 * Renders a simple card layout with a title, subtitle, and optional toolbar buttons.
 * Buttons are dynamically built from the {@link NoValuePanelDto} model.
 * <p>
 */
public abstract class NoValuePanel extends BasePanel<NoValuePanelDto> {

    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_PANEL = "panel";
    private static final String ID_TITLE = "title";
    private static final String ID_SUBTITLE = "subtitle";

    private static final String ID_BUTTON_TOOLBAR = "buttonToolbar";
    private static final String ID_BUTTON_BAR = "buttonBar";
    private static final String ID_BUTTON = "button";

    public NoValuePanel(String id, IModel<NoValuePanelDto> model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        WebMarkupContainer panel = new WebMarkupContainer(ID_PANEL);
        panel.setOutputMarkupId(true);
        add(panel);

        Label titleComponent = createLabelComponent(ID_TITLE, getModelObject().getTitle());
        panel.add(titleComponent);

        Label subtitleComponent = createLabelComponent(ID_SUBTITLE, getModelObject().getSubtitle());
        panel.add(subtitleComponent);

        ButtonBar<Containerable, SelectableRow<?>> components = new ButtonBar<>(ID_BUTTON_TOOLBAR, ID_BUTTON_BAR,
                NoValuePanel.this, createToolbarButtons(ID_BUTTON));
        components.setOutputMarkupId(true);
        panel.add(components);
    }

    /**
     * Creates the list of toolbar button components for this panel.
     *
     * @param buttonsId the Wicket ID to assign to each created button component
     * @return a list of components representing the toolbar buttons (never {@code null})
     */
    protected abstract List<Component> createToolbarButtons(String buttonsId);

    /**
     * Creates a label component with common output markup settings.
     *
     * @param id Component ID
     * @param title The label model (typically a localized string resource)
     * @return Configured {@link Label} instance
     */
    private @NotNull Label createLabelComponent(String id, StringResourceModel title) {
        Label label = new Label(id, title);
        label.setOutputMarkupId(true);
        label.setOutputMarkupPlaceholderTag(true);
        return label;
    }

}

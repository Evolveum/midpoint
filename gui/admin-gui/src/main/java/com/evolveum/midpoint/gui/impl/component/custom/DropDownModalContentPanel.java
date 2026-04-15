/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */
package com.evolveum.midpoint.gui.impl.component.custom;

import com.evolveum.midpoint.gui.api.component.BasePanel;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;

/**
 * Base panel for dropdown components with modal-like content layout.
 *
 * <p>Provides a common dropdown shell with toggle, optional title icon,
 * title, close button, and footer. Subclasses supply label model, sizing,
 * and optional header/footer customization.</p>
 */
public abstract class DropDownModalContentPanel extends BasePanel<String> {

    private static final String ID_TOGGLE = "toggle";
    private static final String ID_LABEL = "label";
    private static final String ID_CLOSE = "close";

    private static final String ID_CONTENT_CONTAINER = "contentContainer";
    private static final String ID_TITLE_ICON = "titleIcon";
    private static final String ID_TITLE = "title";
    private static final String ID_FOOTER = "footer";

    public DropDownModalContentPanel(String id) {
        super(id);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        add(createToggleComponent());
        add(createContentContainer());
    }

    private @NotNull Component createToggleComponent() {
        WebMarkupContainer toggle = new WebMarkupContainer(ID_TOGGLE);
        toggle.add(new Label(ID_LABEL, getLabelModel()));
        return toggle;
    }

    private @NotNull WebMarkupContainer createContentContainer() {
        WebMarkupContainer contentContainer = new WebMarkupContainer(ID_CONTENT_CONTAINER);
        contentContainer.setOutputMarkupId(true);
        contentContainer.setOutputMarkupPlaceholderTag(true);
        contentContainer.add(AttributeModifier.append("style", buildContentContainerStyle()));

        contentContainer.add(createTitleIconComponent(ID_TITLE_ICON));
        contentContainer.add(createTitleComponent(ID_TITLE));
        contentContainer.add(createCloseComponent(ID_CLOSE));
        contentContainer.add(createFooterComponent(ID_FOOTER));

        return contentContainer;
    }

    private @NotNull String buildContentContainerStyle() {
        String widthStyle = normalizeStyle(getWidthCssStyle());
        String heightStyle = normalizeStyle(getHeightCssStyle());

        if (widthStyle.isEmpty()) {
            return heightStyle;
        }
        if (heightStyle.isEmpty()) {
            return widthStyle;
        }
        return widthStyle + " " + heightStyle;
    }

    private @NotNull String normalizeStyle(String style) {
        if (style == null || style.isBlank()) {
            return "";
        }
        String trimmed = style.trim();
        return trimmed.endsWith(";") ? trimmed : trimmed + ";";
    }

    protected abstract IModel<String> getLabelModel();

    protected Component createTitleIconComponent(String id) {
        return new EmptyPanel(id).setVisible(false);
    }

    protected Component createTitleComponent(String id) {
        return new EmptyPanel(id).setVisible(false);
    }

    protected Component createCloseComponent(String id) {
        return new WebMarkupContainer(id);
    }

    protected Component createFooterComponent(String id) {
        return new EmptyPanel(id).setVisible(false);
    }

    protected WebMarkupContainer getContentContainer() {
        return (WebMarkupContainer) get(ID_CONTENT_CONTAINER);
    }

    protected abstract String getWidthCssStyle();

    protected abstract String getHeightCssStyle();
}

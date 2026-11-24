/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */
package com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.objectclass;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

/**
 * Alert of a wizard step with an icon, an optional title and a text.
 */
public class WizardAlertPanel extends BasePanel<String> {

    private static final String ID_ICON = "icon";
    private static final String ID_TITLE = "title";
    private static final String ID_TEXT = "text";
    private static final String ID_CLOSE = "close";

    /**
     * Type of the alert, defining its color and icon.
     */
    private final AlertType type;

    /**
     * Title shown above the text; no title is shown when the model is empty.
     */
    private final IModel<String> titleModel;

    public WizardAlertPanel(String id, AlertType type, IModel<String> titleModel, IModel<String> textModel) {
        super(id, textModel);
        this.type = type;
        this.titleModel = titleModel;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        add(AttributeAppender.append("class", createCssClass()));
        add(AttributeModifier.replace("role", "alert"));
        add(createIcon());
        add(createTitle());
        add(createText());
        add(createCloseButton());
    }

    /**
     * Tells whether the user can close the alert.
     *
     * @return true if the alert has the close button
     */
    protected boolean isDismissible() {
        return false;
    }

    private String createCssClass() {
        String cssClass = "alert alert-opaque d-flex px-3 " + type.getCssClass();
        return isDismissible() ? cssClass + " alert-dismissible fade show" : cssClass;
    }

    private WebMarkupContainer createIcon() {
        WebMarkupContainer icon = new WebMarkupContainer(ID_ICON);
        icon.add(AttributeAppender.append("class", type.getIconCssClass()));
        return icon;
    }

    private Label createTitle() {
        Label title = new Label(ID_TITLE, titleModel);
        title.add(new VisibleBehaviour(() -> titleModel.getObject() != null));
        return title;
    }

    private Label createText() {
        return new Label(ID_TEXT, getModel());
    }

    private WebMarkupContainer createCloseButton() {
        WebMarkupContainer close = new WebMarkupContainer(ID_CLOSE);
        close.add(new VisibleBehaviour(this::isDismissible));
        return close;
    }

    /**
     * Type of the alert.
     */
    public enum AlertType {

        /**
         * Information for the user.
         */
        INFO("alert-info", "fa fa-info-circle"),

        /**
         * Warning about a situation the user should handle.
         */
        WARNING("alert-warning", "fa fa-exclamation-triangle"),

        /**
         * Confirmation of a successful result.
         */
        SUCCESS("alert-success", "fa fa-check-circle");

        private final String cssClass;
        private final String iconCssClass;

        AlertType(String cssClass, String iconCssClass) {
            this.cssClass = cssClass;
            this.iconCssClass = iconCssClass;
        }

        public String getCssClass() {
            return cssClass;
        }

        public String getIconCssClass() {
            return iconCssClass;
        }
    }
}

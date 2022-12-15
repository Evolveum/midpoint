/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.api.component.result;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;

public class MessagePanel extends BasePanel<String> {

    private static final String ID_MESSAGE = "message";

    private static final String ID_DETAILS_BOX = "detailsBox";

    private static final String ID_ICON_TYPE = "iconType";

    private static final String ID_CLOSE = "close";

    public enum MessagePanelType {INFO, WARN, SUCCESS, ERROR}

    private MessagePanelType type;

    private boolean closeVisible;

    public MessagePanel(String id, MessagePanelType type, IModel<String> model) {
        this(id, type, model, true);
    }

    public MessagePanel(String id, MessagePanelType type, IModel<String> model, boolean closeVisible) {
        super(id, model);

        this.type = type;
        this.closeVisible = closeVisible;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    public void initLayout() {
        WebMarkupContainer detailsBox = new WebMarkupContainer(ID_DETAILS_BOX);
        detailsBox.setOutputMarkupId(true);
        detailsBox.add(AttributeModifier.append("class", createHeaderCss()));
        add(detailsBox);

        initHeader(detailsBox);
    }

    private IModel<String> createHeaderCss() {
        return () -> {
            switch (type) {
                case INFO:
                    return " box-info";
                case SUCCESS:
                    return " box-success";
                case ERROR:
                    return " box-danger";
                case WARN:
                default:
                    return " box-warning";
            }
        };
    }

    private void initHeader(WebMarkupContainer box) {
        WebMarkupContainer iconType = new WebMarkupContainer(ID_ICON_TYPE);
        iconType.setOutputMarkupId(true);
        iconType.add(AttributeAppender.append("class", () -> {
            switch (type) {
                case INFO:
                    return " fa-info";
                case SUCCESS:
                    return " fa-check";
                case ERROR:
                    return " fa-ban";
                case WARN:
                default:
                    return " fa-warning";
            }
        }));

        box.add(iconType);

        Label message = new Label(ID_MESSAGE, getModel());
        box.add(message);

        AjaxLink<Void> close = new AjaxLink<>(ID_CLOSE) {

            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                if (this.getParent() != null) {
                    target.add(this.getParent().setVisible(false));
                }
                // close(target);
            }
        };
        close.setOutputMarkupId(true);
        close.setVisible(closeVisible);

        box.add(close);
    }

    public void close(AjaxRequestTarget target) {
        this.setVisible(false);
        target.add(this);
    }
}

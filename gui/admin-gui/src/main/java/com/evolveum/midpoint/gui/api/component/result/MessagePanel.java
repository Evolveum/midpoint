/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.api.component.result;

import com.evolveum.midpoint.gui.api.util.LocalizationUtil;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;

import org.apache.wicket.model.Model;

import java.io.Serializable;

public class MessagePanel<T extends Serializable> extends BasePanel<T> {

    private static final String ID_MESSAGE = "message";

    private static final String ID_DETAILS_BOX = "detailsBox";

    private static final String ID_ICON_TYPE = "iconType";

    private static final String ID_CLOSE = "close";

    public enum MessagePanelType {INFO, WARN, SUCCESS, ERROR}

    private IModel<MessagePanelType> type;

    private boolean closeVisible;

    public MessagePanel(String id, MessagePanelType type, IModel<T> model) {
        this(id, type, model, true);
    }

    public MessagePanel(String id, MessagePanelType type, IModel<T> model, boolean closeVisible) {
        this(id, Model.of(type), model, closeVisible);
    }

    public MessagePanel(String id, IModel<MessagePanelType> type, IModel<T> model) {
        this(id, type, model, true);
    }

    public MessagePanel(String id, IModel<MessagePanelType> type, IModel<T> model, boolean closeVisible) {
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

    protected IModel<String> createHeaderCss() {
        return () -> {
            switch (type.getObject()) {
                case INFO:
                    return "card-info";
                case SUCCESS:
                    return "card-success";
                case ERROR:
                    return "card-danger";
                case WARN:
                default:
                    return "card-warning";
            }
        };
    }

    protected Object getIconTypeCss() {
        return switch (type.getObject()) {
            case INFO -> "fa-info";
            case SUCCESS -> "fa-check";
            case ERROR -> "fa-ban";
            default -> "fa-exclamation-triangle";
        };
    }

    private void initHeader(WebMarkupContainer box) {
        WebMarkupContainer iconType = new WebMarkupContainer(ID_ICON_TYPE);
        iconType.setOutputMarkupId(true);
        iconType.add(AttributeAppender.append("class", () -> getIconTypeCss()));

        box.add(iconType);

        Label message = new Label(ID_MESSAGE, getModel());
        message.setRenderBodyOnly(true);
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
        close.add(AttributeModifier.append("aria-label", LocalizationUtil.translate("OperationResultPanel.button.close")));

        box.add(close);
    }

    public void close(AjaxRequestTarget target) {
        this.setVisible(false);
        target.add(this);
    }
}

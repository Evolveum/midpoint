/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.api.component.result;

import java.io.Serializable;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;

public class MessagePanel extends BasePanel<String> {

    private static final String ID_MESSAGE = "message";

    public enum MessagePanelType {INFO, WARN, SUCCESS, ERROR}

    private MessagePanelType type;

    public MessagePanel(String id, MessagePanelType type, IModel<String> model) {
        super(id, model);
        this.type = type;

    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    public void initLayout() {

        WebMarkupContainer detailsBox = new WebMarkupContainer("detailsBox");
        detailsBox.setOutputMarkupId(true);
        detailsBox.add(AttributeModifier.append("class", createHeaderCss()));
        add(detailsBox);

        initHeader(detailsBox);

    }

    private IModel<String> createHeaderCss() {

        return (IModel<String>) () -> {
            switch (type) {
                case INFO:
                    return " box-info";
                case SUCCESS:
                    return " box-success";
                case ERROR:
                    return " box-danger";
                case WARN: // TODO:
                default:
                    return " box-warning";
                }
            };
    }

    private void initHeader(WebMarkupContainer box) {
        WebMarkupContainer iconType = new WebMarkupContainer("iconType");
        iconType.setOutputMarkupId(true);
        iconType.add(new AttributeAppender("class", (IModel) () -> {

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

        Label message =  new Label(ID_MESSAGE, getModel());
        box.add(message);

        AjaxLink<Void> close = new AjaxLink<Void>("close") {

            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                close(target);

            }
        };

        box.add(close);
    }

    public void close(AjaxRequestTarget target){
        this.setVisible(false);
        target.add(this);
    }
}

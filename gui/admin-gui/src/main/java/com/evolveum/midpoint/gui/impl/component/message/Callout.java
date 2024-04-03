/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.component.message;

import com.evolveum.midpoint.gui.api.component.BasePanel;

import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;

/**
 * Callout panel with message. Possible types are info, success, warning and danger.
 */
public class Callout extends BasePanel<String> {

    private final static String ID_CARD = "card";
    private final static String ID_CLOSE_BUTTON = "closeButton";
    private final static String ID_BADGE = "badgeContainer";
    private final static String ID_BADGE_ICON = "badgeIcon";
    private final static String ID_BADGE_LABEL = "badgeLabel";
    private final static String ID_TITLE = "title";
    private final static String ID_MESSAGE = "message";

    private final Type type;
    private boolean visible = true;

    private Callout(String id, IModel<String> model, Type type) {
        super(id, model);
        this.type = type;
    }

    public static Callout createInfoCallout(String id, IModel<String> message){
        return new Callout(id, message, Type.INFO);
    }

    public static Callout createWarningCallout(String id, IModel<String> message){
        return new Callout(id, message, Type.WARNING);
    }

    public static Callout createDangerCallout(String id, IModel<String> message){
        return new Callout(id, message, Type.DANGER);
    }

    public static Callout createSuccessCallout(String id, IModel<String> message){
        return new Callout(id, message, Type.SUCCESS);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        setOutputMarkupId(true);
        add(new VisibleBehaviour(() -> visible));

        WebMarkupContainer card = new WebMarkupContainer(ID_CARD);
        add(card);
        card.add(AttributeAppender.append("class", type.cardClass));

        AjaxLink<Void> closeButton = new AjaxLink<>(ID_CLOSE_BUTTON) {
            private static final long serialVersionUID = 1L;
            @Override
            public void onClick(AjaxRequestTarget target) {
                visible = !visible;
                target.add(Callout.this);
            }
        };
        card.add(closeButton);

        WebMarkupContainer badgeContainer = new WebMarkupContainer(ID_BADGE);
        badgeContainer.add(AttributeAppender.append("class", type.badgeClass));
        card.add(badgeContainer);

        WebMarkupContainer icon = new WebMarkupContainer(ID_BADGE_ICON);
        icon.add(AttributeAppender.append("class", type.icon));
        badgeContainer.add(icon);

        Label label = new Label(ID_BADGE_LABEL, getPageBase().createStringResource(type.badgeLabelKey));
        badgeContainer.add(label);

        Label title = new Label(ID_TITLE, getPageBase().createStringResource("Callout.title"));
        card.add(title);

        Label message = new Label(ID_MESSAGE, getModel());
        card.add(message);
    }

    private enum Type {
        INFO("card-outline-left-info", "Callout.badge.info", "bg-info", "fa fa-circle-info"),
        WARNING("card-outline-left-warning", "Callout.badge.warning", "bg-warning", "fa fa-circle-info"),
        DANGER("card-outline-left-danger", "Callout.badge.danger", "bg-danger", "fa fa-circle-info"),
        SUCCESS("card-outline-left-success", "Callout.badge.success", "bg-success", "fa fa-circle-info");

        final String cardClass;
        final String badgeClass;
        final String icon;
        final String badgeLabelKey;

        Type(String cardClass, String badgeLabelKey, String badgeClass, String icon) {
            this.cardClass = cardClass;
            this.badgeClass = badgeClass;
            this.icon = icon;
            this.badgeLabelKey = badgeLabelKey;
        }
    }
}

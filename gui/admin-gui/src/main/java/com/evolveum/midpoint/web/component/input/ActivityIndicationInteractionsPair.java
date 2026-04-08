/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 *
 */

package com.evolveum.midpoint.web.component.input;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.model.IDetachable;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.web.component.AjaxIconButton;

/**
 * Implementation of `ComponentInteractionsPair` that provides activity indication for `AjaxIconButton` components.
 *
 * This class manages the visual indication of an ongoing activity by temporarily changing the icon and title of a
 * button. When an activity starts, the button's icon and title are replaced with activity-specific values. After the
 * activity completes, the original icon and title are restored.
 *
 * @see ComponentInteractionsPair
 * @see AjaxEventBasedInteractionsLinker
 * @see AjaxIconButton
 */
public class ActivityIndicationInteractionsPair implements ComponentInteractionsPair<AjaxIconButton>, IDetachable {

    private final IModel<String> activityIndicationIcon;
    private final IModel<String> activityIndicationTitle;
    private final boolean disableOnActivity;
    private IModel<String> originalIcon;
    private IModel<String> originalTitle;

    public ActivityIndicationInteractionsPair(IModel<String> activityIndicationIcon,
            IModel<String> activityIndicationTitle, boolean disableOnActivity) {
        this.activityIndicationIcon = activityIndicationIcon;
        this.activityIndicationTitle = activityIndicationTitle;
        this.disableOnActivity = disableOnActivity;
        this.originalTitle = new Model<>();
        this.originalIcon = new Model<>();
    }

    /**
     * Saves the original button icon and title and replaces them with the activity indication icon and title.
     *
     * @param component The button on which you want to indicate the activity.
     * @param request The target of the Ajax request, in which you want to add the activity indication to the button.
     */
    @Override
    public void action(AjaxIconButton component, AjaxRequestTarget request) {
        this.originalIcon = Model.of(component.getModel().getObject());
        this.originalTitle = Model.of(component.getTitle().getObject());
        component.getModel().setObject(this.activityIndicationIcon.getObject());
        component.getTitle().setObject(this.activityIndicationTitle.getObject());
        if (this.disableOnActivity) {
            component.add(AttributeAppender.append("disabled", "disabled"));
        }
        request.add(component);
    }

    /**
     * Restores the original icon and title back to the button.
     *
     * @param component The button on which you want to restore the icon and title.
     * @param request The target of the Ajax request, in which you want to restore the icon and text of the button.
     */
    @Override
    public void reaction(AjaxIconButton component, AjaxRequestTarget request) {
        component.getModel().setObject(this.originalIcon.getObject());
        component.getTitle().setObject(this.originalTitle.getObject());
        if (this.disableOnActivity) {
            component.add(AttributeAppender.remove("disabled"));
        }
        request.add(component);
    }

    @Override
    public void detach() {
        this.activityIndicationIcon.detach();
        this.activityIndicationTitle.detach();
        this.originalTitle.detach();
        this.originalIcon.detach();
    }
}

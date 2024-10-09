/*
 * Copyright (c) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.widgets.model;

import java.io.Serializable;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

import org.jetbrains.annotations.Nullable;

import static com.evolveum.midpoint.web.component.data.column.ColumnUtils.createStringResource;

public class IdentifyWidgetItem implements Serializable {

    private final IModel<String> image;
    private final IModel<String> value;
    private final IModel<String> title;
    private final IModel<String> score;
    private final IModel<String> navigationReference;
    boolean isLoading = false;

    transient ComponentType type;

    public enum ComponentType {
        PATTERN, OUTLIER, STATISTIC;
    }

    public IdentifyWidgetItem(
            @NotNull ComponentType type,
            @Nullable IModel<String> image,
            @NotNull IModel<String> title,
            @NotNull IModel<String> description,
            @NotNull IModel<String> score,
            @NotNull IModel<String> navigationReference) {
        this.type = type;
        this.image = image;
        this.value = description;
        this.score = score;
        this.title = title;
        this.navigationReference = navigationReference;
    }

    public Component createImageComponent(String id) {
        Label label = new Label(id);
        label.add(new VisibleBehaviour(() -> getImage() != null));
        if (getImage() != null) {
            label.add(AttributeModifier.append("class", getImage().getObject()));
        }
        label.setOutputMarkupId(true);
        return label;
    }

    public Component createTitleComponent(String id) {
        Label label = new Label(id, getTitle());
        label.add(new VisibleBehaviour(() -> getTitle() != null));
        label.setOutputMarkupId(true);
        return label;
    }

    public Component createValueTitleComponent(String id) {
        Label label = new Label(id,
                createStringResource("RoleAnalysisIdentifyWidgetPanel.value.title.score"));
        label.setOutputMarkupId(true);
        label.add(new VisibleBehaviour(() -> getDescription() != null));
        return label;
    }

    public Component createActionComponent(String id){
        Label labelAction = new Label(id);
        labelAction.setOutputMarkupId(true);
        labelAction.add(new VisibleBehaviour(() -> getScore() != null));
        labelAction.add(AttributeAppender.append("class", "fa fa-angle-right"));
        labelAction.add(new AjaxEventBehavior("click") {
            @Override
            protected void onEvent(AjaxRequestTarget target) {
                onActionComponentClick(target);
            }
        });
        return labelAction;
    }

    public void onActionComponentClick(AjaxRequestTarget target){

    }

    public Component createDescriptionComponent(String id) {
        Label label = new Label(id, getDescription());
        label.add(new VisibleBehaviour(() -> getDescription() != null));
        label.setOutputMarkupId(true);
        return label;
    }

    public Component createScoreComponent(String id) {
        Label label = new Label(id, getScore());
        label.add(new VisibleBehaviour(() -> getScore() != null));
        label.setOutputMarkupId(true);
        label.add(AttributeModifier.append("class", "text-danger"));
        return label;
    }

    public VisibleBehaviour isVisible() {
        return null;
    }

    public IModel<String> getImage() {
        return image;
    }

    public IModel<String> getDescription() {
        return value;
    }

    public IModel<String> getScore() {
        return score;
    }

    public IModel<String> getTitle() {
        return title;
    }

    public IModel<String> getNavigationReference() {
        return navigationReference;
    }

    public ComponentType getType() {
        return type;
    }

    public boolean isLoading() {
        return isLoading;
    }
}

/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.api.component;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Created by Viliam Repan (lazyman).
 */
public class TogglePanel<O extends Serializable> extends BasePanel<List<Toggle<O>>> {

    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_BUTTONS = "buttons";
    private static final String ID_BUTTON = "button";
    private static final String ID_CONTENT = "content";
    private static final String ID_DEFAULT_BUTTON_CONTENT = "defaultButtonContent";
    private static final String ID_ICON = "icon";
    private static final String ID_LABEL = "label";
    private static final String ID_BADGE = "badge";

    public TogglePanel(String id, IModel<List<Toggle<O>>> items) {
        super(id, items);

        initLayout();
    }

    private void initLayout() {
        add(AttributeAppender.append("class", this::getDefaultCssClass));
        setOutputMarkupId(true);

        ListView<Toggle<O>> buttons = new ListView<>(ID_BUTTONS, getModel()) {

            @Override
            protected void populateItem(ListItem<Toggle<O>> item) {
                AjaxLink<Void> button = new AjaxLink<>(ID_BUTTON) {
                    @Serial private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        itemSelected(target, item.getModel());
                    }
                };

                button.add(AttributeAppender.replace("class", getButtonCssClass()));
                button.add(AttributeAppender.append("class", () -> item.getModelObject().isActive()
                        ? getActiveCssClass() : getInactiveCssClass()));
                button.add(AttributeAppender.replace("aria-pressed", () -> item.getModelObject().isActive() ? "true" : "false"));
                button.add(AttributeAppender.append("title", getTitleModel(item)));
                button.add(AttributeAppender.append("aria-label", getTitleModel(item)));

                item.add(button);

                Component content = createButtonContent(ID_CONTENT, item.getModel());
                button.add(content);
            }
        };
        buttons.setOutputMarkupId(true);
        buttons.add(AttributeAppender.append("class", getDefaultCssClass()));
        add(buttons);
    }

    public @NotNull String getActiveCssClass() {
        return "active";
    }

    public @Nullable String getInactiveCssClass() {
        return null;
    }
    protected String getDefaultCssClass() {
        return "btn-group";
    }

    protected String getButtonCssClass() {
        return "btn btn-default";
    }

    private IModel<String> getTitleModel(ListItem<Toggle<O>> item) {
        return () -> {
            String title = item.getModelObject().getTitle();
            if (StringUtils.isBlank(title)) {
                return null;
            }
            return getString(title, null, title);
        };
    }

    protected Component createButtonContent(String id, IModel<Toggle<O>> model) {
        Fragment defaultButtonContent = new Fragment(id, ID_DEFAULT_BUTTON_CONTENT, this);

        WebMarkupContainer icon = new WebMarkupContainer(ID_ICON);
        icon.add(AttributeAppender.append("class", () -> model.getObject().getIconCss()));
        icon.add(new VisibleBehaviour(() -> StringUtils.isNotEmpty(model.getObject().getIconCss())));
        defaultButtonContent.add(icon);

        Label label = new Label(ID_LABEL, () -> getString(model.getObject().getLabel(), null, model.getObject().getLabel()));
        label.add(new VisibleBehaviour(() -> StringUtils.isNotEmpty(model.getObject().getLabel())));
        defaultButtonContent.add(label);

        Label badge = new Label(ID_BADGE, () -> model.getObject().getBadge());
        badge.add(AttributeAppender.append("class", () -> model.getObject().getBadgeCss()));
        badge.add(new VisibleBehaviour(() -> StringUtils.isNotEmpty(model.getObject().getBadge())));
        defaultButtonContent.add(badge);

        return defaultButtonContent;
    }

    protected boolean showTitle() {
        return false;
    }

    protected void itemSelected(AjaxRequestTarget target, IModel<Toggle<O>> item) {
        Toggle<O> toggle = item.getObject();

        boolean wasActiveBefore = toggle.isActive();

        List<Toggle<O>> list = getModelObject();
        list.forEach(t -> t.setActive(false));

        toggle.setActive(!wasActiveBefore);

        target.add(this);
    }
}

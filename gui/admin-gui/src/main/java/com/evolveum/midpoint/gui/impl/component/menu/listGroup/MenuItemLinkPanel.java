/*
 * Copyright (c) 2010-2022 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.component.menu.listGroup;

import java.io.Serial;
import java.io.Serializable;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

import org.apache.wicket.model.LoadableDetachableModel;

/**
 * Created by Viliam Repan (lazyman).
 */
public abstract class MenuItemLinkPanel<T extends Serializable> extends BasePanel<ListGroupMenuItem<T>> {

    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_LINK = "link";
    private static final String ID_ICON = "icon";
    private static final String ID_LABEL = "label";
    private static final String ID_BADGE = "badge";
    private static final String ID_CHEVRON = "chevron";

    private final int level;

    public MenuItemLinkPanel(String id, IModel<ListGroupMenuItem<T>> model, int level) {
        super(id, model);

        this.level = level;

        initLayout();
    }

    private void initLayout() {
        add(AttributeAppender.append("class", "item " + "level-" + (level + 1)));
        add(AttributeAppender.append("class", () -> getModelObject().isActive() ? "active" : null));
        add(AttributeAppender.append("class", () -> getModelObject().isDisabled() ? "disabled" : null));

        AjaxLink<?> link = new AjaxLink<>(ID_LINK) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                onClickPerformed(target, MenuItemLinkPanel.this.getModelObject());
                target.appendJavaScript(String.format("MidPointTheme.saveFocus('%s');", this.getPageRelativePath()));
                target.appendJavaScript("MidPointTheme.restoreFocus();");
            }
        };
        link.add(new AttributeModifier("data-component-id", () ->
                MenuItemLinkPanel.this.getModelObject().isActive() ? link.getPageRelativePath() : null));
        link.add(AttributeAppender.append("aria-pressed", () ->
                getModelObject().isActive() ? "true" : "false"
        ));
        link.setOutputMarkupId(true);
        add(link);

        WebMarkupContainer icon = new WebMarkupContainer(ID_ICON);
        icon.add(AttributeAppender.append("class",
                () -> StringUtils.isNotEmpty(getModelObject().getIconCss()) ? getModelObject().getIconCss() : "far fa-fw fa-circle"));
        link.add(icon);

        Label label = new Label(ID_LABEL, createLabelModel());
        link.add(label);

        Label badge = new Label(ID_BADGE, () -> getModelObject().getBadge());
        badge.add(AttributeAppender.replace("class", () -> getModelObject().getBadgeCss()));
        badge.add(new VisibleBehaviour(() -> StringUtils.isNotEmpty(getModelObject().getBadge())));
        link.add(badge);

        WebMarkupContainer chevron = new WebMarkupContainer(ID_CHEVRON);
        IModel<String> chevronTitleModel = LoadableDetachableModel.of(() -> {
            if (isChevronLinkVisible()) {
                String labelValue = getModelObject().getLabel();
                if (labelValue != null) {
                    String labelTitle = getString(labelValue, null, labelValue);
                    return String.format("%s - %s", labelTitle, getString("MenuItemLinkPanel.chevron"));
                }
                return getString("MenuItemLinkPanel.chevron");
            }
            return null;
        });
        chevron.add(new AjaxEventBehavior("click") {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected void onEvent(AjaxRequestTarget target) {
                onChevronClickPerformed(target, MenuItemLinkPanel.this.getModelObject());
                target.appendJavaScript(String.format("MidPointTheme.saveFocus('%s');", chevron.getPageRelativePath()));
                target.appendJavaScript("MidPointTheme.restoreFocus();");
            }
        });

        chevron.add(AttributeAppender.append("aria-label", chevronTitleModel));
        chevron.add(AttributeAppender.append("title", chevronTitleModel));
        chevron.add(AttributeAppender.append("aria-pressed", () -> getModelObject().isOpen() ? "true" : "false"));
        chevron.setOutputMarkupId(true);
        chevron.add(AttributeAppender.append("class",
                () -> getModelObject().isOpen() ? "fa fa-chevron-down" : "fa fa-chevron-left"));
        link.add(chevron);
    }

    protected boolean isChevronLinkVisible() {
        ListGroupMenuItem<T> item = getModelObject();
        if (!item.isLoaded()) {
            item.getItems();
        }
        return StringUtils.isEmpty(item.getBadge()) && !item.isEmpty();
    }

    private IModel<String> createLabelModel() {
        return () -> {
            String key = getModelObject().getLabel();
            return getString(key, null, key);
        };
    }

    protected void onClickPerformed(AjaxRequestTarget target, ListGroupMenuItem item) {

    }

    protected void onChevronClickPerformed(AjaxRequestTarget target, ListGroupMenuItem item) {

    }
}

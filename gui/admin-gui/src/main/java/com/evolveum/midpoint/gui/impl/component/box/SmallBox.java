/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.component.box;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Viliam Repan (lazyman).
 */
public class SmallBox extends BasePanel<SmallBoxData> {

    private static final long serialVersionUID = 1L;

    private static final String ID_TITLE = "title";
    private static final String ID_DESCRIPTION = "description";
    private static final String ID_ICON = "icon";
    private static final String ID_LINK = "link";
    private static final String ID_LINK_ICON = "linkIcon";
    private static final String ID_LINK_TEXT = "linkText";

    private static final String ICON_SMALL_FONT_STYLE = "small-box-default-icon";

    public SmallBox(String id, @NotNull IModel<SmallBoxData> model) {
        super(id, model);

        initLayout();
    }

    private void initLayout() {
        add(AttributeAppender.append("class", () -> {
            List<String> classes = new ArrayList<>();
            classes.add("small-box");

            SmallBoxData data = getModelObject();
            if (data.getSmallBoxCssClass() != null) {
                classes.add(data.getSmallBoxCssClass());
            }

            return StringUtils.join(classes, " ");
        }));

        addLabel(ID_TITLE, () -> getModelObject().getTitle());
        addLabel(ID_DESCRIPTION, () -> getModelObject().getDescription());

        WebMarkupContainer icon = new WebMarkupContainer(ID_ICON);
        icon.add(AttributeAppender.append("class", () -> getIconStyle(getModelObject().getIcon())));
        add(icon);

        AjaxLink link = new AjaxLink<>(ID_LINK) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                onClickLink(target);
            }
        };
        link.add(new VisibleBehaviour(() -> isLinkVisible()));
        add(link);

        Label linkText = new Label(ID_LINK_TEXT, () -> {
            String text = getModelObject().getLinkText();
            if (text == null) {
                return null;
            }

            return getString(text, new Model<>(), text);
        });
        linkText.setRenderBodyOnly(true);
        link.add(linkText);

        WebMarkupContainer linkIcon = new WebMarkupContainer(ID_LINK_ICON);
        linkIcon.add(AttributeAppender.append("class", () -> getModelObject().getLinkIcon()));
        link.add(linkIcon);
    }

    private void addLabel(String id, IModel model) {
        Label label = new Label(id, model);
        label.add(new VisibleBehaviour(() -> model.getObject() != null));
        add(label);
    }

    protected void onClickLink(AjaxRequestTarget target) {
        setResponsePage(SmallBox.this.getModelObject().getLink());
    }

    protected boolean isLinkVisible() {
        return getModelObject().getLink() != null;
    }

    private String getIconStyle(String iconClass) {
        return isLinkVisible() ? iconClass : iconClass + " " + ICON_SMALL_FONT_STYLE;
    }
}

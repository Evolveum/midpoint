/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.component.box;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Viliam Repan (lazyman).
 */
public class InfoBox extends BasePanel<InfoBoxData> {

    private static final long serialVersionUID = 1L;

    private static final String ID_TEXT = "text";
    private static final String ID_NUMBER = "number";
    private static final String ID_PROGRESS = "progress";
    private static final String ID_DESCRIPTION = "description";
    public static final String ID_DESCRIPTION_2 = "description2";
    private static final String ID_ICON = "icon";
    private static final String ID_ICON_CONTAINER = "iconContainer";

    public InfoBox(String id, @NotNull IModel<InfoBoxData> model) {
        super(id, model);

        initLayout();
    }

    private void initLayout() {
        add(AttributeAppender.append("class", () -> {
            List<String> classes = new ArrayList<>();
            classes.add("info-box");

            InfoBoxData data = getModelObject();
            if (data.getLink() != null) {
                classes.add("info-box-link");
            }

            if (data.getInfoBoxCssClass() != null) {
                classes.add(data.getInfoBoxCssClass());
            }

            return StringUtils.join(classes, " ");
        }));

        AjaxEventBehavior linkClick = new AjaxEventBehavior("click") {

            private static final long serialVersionUID = 1L;

            @Override
            public boolean isEnabled(Component component) {
                return getModelObject().getLink() != null;
            }

            @Override
            protected void onEvent(AjaxRequestTarget target) {
                setResponsePage(getModelObject().getLink());
            }
        };
        add(linkClick);

        add(createLabel(ID_TEXT, () -> getModelObject().getText()));
        add(createLabel(ID_NUMBER, () -> getModelObject().getNumber()));
        add(createLabel(ID_DESCRIPTION, () -> getModelObject().getDescription()));
        add(createLabel(ID_DESCRIPTION_2, () -> getModelObject().getDescription2()));

        WebMarkupContainer progress = new WebMarkupContainer(ID_PROGRESS);
        progress.add(new VisibleBehaviour(() -> getModelObject().getProgress() != null));
        progress.add(AttributeModifier.replace("style", () -> "width: " + getModelObject().getProgress() + "%;"));
        add(progress);

        WebMarkupContainer iconContainer = new WebMarkupContainer(ID_ICON_CONTAINER);
        iconContainer.add(AttributeAppender.append("class", () -> getModelObject().getIconCssClass()));
        iconContainer.add(new VisibleBehaviour(() -> getModelObject().getIcon() != null));
        add(iconContainer);

        WebMarkupContainer icon = new WebMarkupContainer(ID_ICON);
        icon.add(AttributeAppender.append("class", () -> getModelObject().getIcon()));
        iconContainer.add(icon);
    }

    protected Component createLabel(String id, IModel model) {
        Label label = new Label(id, model);
        label.add(new VisibleBehaviour(() -> model.getObject() != null));
        return label;
    }
}

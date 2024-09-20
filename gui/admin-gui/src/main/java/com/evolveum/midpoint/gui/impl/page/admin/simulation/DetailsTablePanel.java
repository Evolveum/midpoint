/*
 * Copyright (c) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.simulation;

import java.io.Serial;
import java.util.List;

import com.evolveum.midpoint.gui.api.util.GuiDisplayTypeUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DisplayType;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.WebComponent;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

/**
 * Created by Viliam Repan (lazyman).
 */
public class DetailsTablePanel extends BasePanel<List<DetailsTableItem>> {

    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_ICON = "icon";
    private static final String ID_TITLE = "title";
    private static final String ID_DETAILS = "details";
    private static final String ID_LABEL = "label";
    private static final String ID_DESCRIPTION = "description";
    private static final String ID_VALUE = "value";

    private IModel<DisplayType> display;

    public DetailsTablePanel(String id, IModel<DisplayType> display, IModel<List<DetailsTableItem>> model) {
        super(id, model);
        this.display = display;

        initLayout();
    }

    @Override
    protected void onComponentTag(ComponentTag tag) {
        super.onComponentTag(tag);

        checkComponentTag(tag, "div");
    }

    private void initLayout() {
        add(AttributeModifier.append("class", initDefaultCssClass()));

        IModel<String> titleModel = getTitleModel();

        IModel<String> iconCssClassModel = getIconCssClassModel();

        WebComponent icon = new WebComponent(ID_ICON);
        icon.add(new VisibleBehaviour(() -> iconCssClassModel.getObject() != null));
        icon.add(AttributeModifier.append("class", iconCssClassModel));
//        icon.add(AttributeAppender.append("style", getIconBackgroundColor()));
        add(icon);

        Label title = new Label(ID_TITLE, titleModel);
//        title.setRenderBodyOnly(true);
        title.add(AttributeModifier.append("class", GuiDisplayTypeUtil.getDisplayCssClass(
                display != null ? display.getObject() : null)));
        add(title);

        IModel<String> descriptionModel = getDescriptionModel();
        Label description = new Label(ID_DESCRIPTION, descriptionModel);
//        description.add(new VisibleBehaviour(() -> descriptionModel.getObject() != null));
        add(description);

        ListView<DetailsTableItem> details = new ListView<>(ID_DETAILS, getModel()) {

            @Override
            protected void populateItem(ListItem<DetailsTableItem> item) {
                DetailsTableItem data = item.getModelObject();
                item.add(data.createLabelComponent(ID_LABEL));
                item.add(data.createValueComponent(ID_VALUE));

                if (data.isVisible() != null) {
                    item.add(data.isVisible());
                }
            }
        };
        add(details);
    }

    private IModel<String> getIconCssClassModel() {
        return () -> {
            if (display == null || display.getObject() == null) {
                return null;
            }

            String cssClass = GuiDisplayTypeUtil.getIconCssClass(display.getObject());
            String iconColor = GuiDisplayTypeUtil.getIconColor(display.getObject());
            if (StringUtils.isNotEmpty(iconColor)) {
                return cssClass + " bg-" + iconColor;
            }
            return cssClass;
        };
    }

    private IModel<String> getIconBackgroundColor() {
        return () -> {
            if (display == null || display.getObject() == null) {
                return null;
            }

            String iconColor = GuiDisplayTypeUtil.getIconColor(display.getObject());
            if (StringUtils.isNotEmpty(iconColor)) {
                return "background-color: " + iconColor + ";";
            }
            return "";
        };
    }

    private IModel<String> getTitleModel() {
        return () -> {
            if (display == null || display.getObject() == null) {
                return null;
            }
            return GuiDisplayTypeUtil.getTranslatedLabel(display.getObject());
        };

    }

    private IModel<String> getDescriptionModel() {
        return () -> {
            if (display == null || display.getObject() == null) {
                return null;
            }
            return GuiDisplayTypeUtil.getHelp(display.getObject());
        };

    }

    ;

    protected String initDefaultCssClass() {
        return "card";
    }
}

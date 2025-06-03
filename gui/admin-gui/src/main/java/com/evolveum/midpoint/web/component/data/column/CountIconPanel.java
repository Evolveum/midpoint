/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.data.column;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.model.ReadOnlyModel;
import com.evolveum.midpoint.gui.api.util.GuiDisplayTypeUtil;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DisplayType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.IconType;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;

import java.util.*;

/**
 * @author skublik
 */
public class CountIconPanel extends BasePanel<List<DisplayType>> {

    private static final String ID_ICONS = "icons";
    private static final String ID_COUNT = "count";
    private static final String ID_ICON = "icon";

    private Map<DisplayType, Integer> icons;

    public CountIconPanel(String id, Map<DisplayType, Integer> icons) {
        super(id, new Model());
        this.icons = icons;
        getModel().setObject(new ArrayList<>(icons.keySet()));
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        getModelObject().sort((d1, d2) -> {
            if (d1 == null || d1.getIcon() == null || d1.getIcon().getCssClass() == null
                    || d2 == null || d2.getIcon() == null || d2.getIcon().getCssClass() == null) {
                return 0;
            }
            return d1.getIcon().getCssClass().compareTo(d2.getIcon().getCssClass());
        });

        ListView<DisplayType> iconsPanel = new ListView<>(ID_ICONS, getModel()) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(ListItem<DisplayType> item) {
                Label icon = new Label(ID_ICON);
                icon.add(AttributeModifier.replace("class", new PropertyModel<>(item.getModel(), "icon.cssClass")));
                if (item.getModelObject() != null && item.getModelObject().getTooltip() != null) {
                    icon.add(AttributeModifier.replace("title", () -> getString(item.getModelObject().getTooltip().getOrig())));
                }
                icon.add(AttributeAppender.append("style", () -> StringUtils.isNotBlank(getColor(item.getModelObject())) ? "color: " + getColor(item.getModelObject()) + ";" : ""));
                icon.setOutputMarkupId(true);
                icon.add(new VisibleBehaviour(() -> item.getModelObject() != null && item.getModelObject().getIcon() != null && StringUtils.isNotEmpty(item.getModelObject().getIcon().getCssClass())));
                icon.add(AttributeAppender.append("tabindex", "0"));
                icon.add(AttributeAppender.append("aria-label", () -> getString(item.getModelObject().getTooltip().getOrig())));
                item.add(icon);

                Integer count = icons.get(item.getModelObject());
                Label countPanel = new Label(ID_COUNT, Model.of(count));
                item.add(countPanel);
            }
        };
        add(iconsPanel);
    }

    private String getColor(DisplayType displayType) {
        if (displayType == null) {
            return null;
        }

        IconType icon = displayType.getIcon();
        if (icon == null) {
            return null;
        }

        return GuiDisplayTypeUtil.removeStringAfterSemicolon(icon.getColor());
    }
}

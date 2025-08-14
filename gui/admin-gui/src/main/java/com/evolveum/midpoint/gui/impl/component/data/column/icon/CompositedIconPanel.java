/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component.data.column.icon;

import com.evolveum.midpoint.gui.api.util.GuiDisplayTypeUtil;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebComponent;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIcon;
import com.evolveum.midpoint.gui.impl.component.icon.LayerIcon;
import com.evolveum.midpoint.xml.ns._public.common.common_3.IconType;

/**
 * @author skublik
 */
public class CompositedIconPanel extends BasePanel<CompositedIcon> {

    private static final long serialVersionUID = 1L;

    private static final String ID_LAYERED_ICON = "layeredIcon";
    private static final String ID_BASIC_ICON = "basicIcon";
    private static final String ID_LAYER_ICONS = "layerIcons";
    private static final String ID_LAYER_ICON = "layerIcon";

    public CompositedIconPanel(String id, IModel<CompositedIcon> compositedIcon) {
        super(id, compositedIcon);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        WebMarkupContainer layeredIcon = new WebMarkupContainer(ID_LAYERED_ICON);
        layeredIcon.add(AttributeAppender.append("title", () -> {
            if (getModelObject() != null && StringUtils.isNotBlank(getModelObject().getTitle())) {
                return getModelObject().getTitle();
            }
            return null;
        }));
        add(layeredIcon);

        WebComponent basicIcon = new WebComponent(ID_BASIC_ICON);
        basicIcon.add(AttributeAppender.append("class", () -> {
            if (getModelObject() != null && getModelObject().hasBasicIcon()) {
                return getModelObject().getBasicIcon();
            }
            return null;
        }));
        basicIcon.add(AttributeAppender.append("style", () -> {
            if (getModelObject() != null && getModelObject().hasBasicIcon() && getModelObject().hasBasicIconHtmlColor()) {
                return "color:" + getModelObject().getBasicIconHtmlColor();
            }
            return null;
        }));

        basicIcon.add(AttributeAppender.append("aria-label", () -> {
            if (getModelObject() != null && StringUtils.isNotBlank(getModelObject().getTitle())) {
                return getModelObject().getTitle();
            }
            return null;
        }));
        basicIcon.add(AttributeAppender.append("tabindex",  () -> {
            if (getModelObject() != null && StringUtils.isNotBlank(getModelObject().getTitle())) {
                return "0";
            }
            return "-1";
        }));

        layeredIcon.add(basicIcon);

        ListView<LayerIcon> validationItems = new ListView<LayerIcon>(ID_LAYER_ICONS, new PropertyModel(getModel(), CompositedIcon.F_LAYER_ICONS)) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(ListItem<LayerIcon> item) {
                LayerIcon layerIcon = item.getModelObject();
                if (layerIcon == null) {
                    return;
                }

                IconType iconType = layerIcon.getIconType();
                if (StringUtils.isEmpty(iconType.getCssClass())) {
                    return;
                }

                WebComponent icon = new WebComponent(ID_LAYER_ICON);
                icon.add(AttributeAppender.append("class", iconType.getCssClass()));
                if (StringUtils.isNotEmpty(iconType.getColor())) {
                    icon.add(AttributeAppender.append(
                            "style",
                            "color: " + GuiDisplayTypeUtil.removeStringAfterSemicolon(iconType.getColor())));
                }
                item.add(icon);
            }
        };
        layeredIcon.add(validationItems);
    }
}

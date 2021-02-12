/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component.data.column;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.password.StringLimitationPanel;
import com.evolveum.midpoint.gui.impl.component.icon.LayerIcon;

import com.evolveum.midpoint.model.api.validator.StringLimitationResult;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebComponent;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.RepeatingView;

import com.evolveum.midpoint.gui.impl.component.icon.CompositedIcon;
import com.evolveum.midpoint.xml.ns._public.common.common_3.IconType;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

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
        layeredIcon.add(AttributeAppender.append("title", (IModel<String>) () -> {
            if (getModelObject() != null && org.apache.commons.lang3.StringUtils.isNotBlank(getModelObject().getTitle())) {
                return getModelObject().getTitle();
            }
            return null;
        }));

        add(layeredIcon);
        WebComponent basicIcon = new WebComponent(ID_BASIC_ICON);
        basicIcon.add(AttributeAppender.append("class", (IModel<String>) () -> {
            if (getModelObject() != null && getModelObject().hasBasicIcon()) {
                return getModelObject().getBasicIcon();
            }
            return null;
        }));
        basicIcon.add(AttributeAppender.append("style", (IModel<String>) () -> {
            if (getModelObject() != null && getModelObject().hasBasicIcon() && getModelObject().hasBasicIconHtmlColor()) {
                return "color:" + getModelObject().getBasicIconHtmlColor();
            }
            return null;
        }));
        layeredIcon.add(basicIcon);

        ListView<LayerIcon> validationItems = new ListView<LayerIcon>(ID_LAYER_ICONS, new PropertyModel(getModel(), CompositedIcon.F_LAYER_ICONS)) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(ListItem<LayerIcon> item) {
                if (item.getModelObject() == null) {
                    return;
                }
                if (StringUtils.isNotEmpty(item.getModelObject().getIconType().getCssClass())) {
                    WebComponent icon = new WebComponent(ID_LAYER_ICON);
                    icon.add(AttributeAppender.append("class", item.getModelObject().getIconType().getCssClass()));
                    if (StringUtils.isNotEmpty(item.getModelObject().getIconType().getColor())) {
                        icon.add(AttributeAppender.append("style", "color: " + item.getModelObject().getIconType().getColor()));
                    }
                    item.add(icon);
                }
            }
        };
        layeredIcon.add(validationItems);

        add(new VisibleEnableBehaviour() {
            @Override
            public boolean isVisible() {
                return getModelObject() != null;
            }
        });
    }
}

/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component.data.column;

import com.evolveum.midpoint.gui.impl.component.icon.LayerIcon;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebComponent;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.RepeatingView;

import com.evolveum.midpoint.gui.impl.component.icon.CompositedIcon;
import com.evolveum.midpoint.xml.ns._public.common.common_3.IconType;

/**
 * @author skublik
 */
public class CompositedIconPanel extends Panel {

    private static final long serialVersionUID = 1L;

    private static final String ID_LAYERED_ICON = "layeredIcon";
    private static final String ID_BASIC_ICON = "basicIcon";
    private static final String ID_LAYER_ICONS = "layerIcons";

    private final CompositedIcon compositedIcon;

    public CompositedIconPanel(String id, CompositedIcon compositedIcon) {
        super(id);
        this.compositedIcon = compositedIcon;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        WebMarkupContainer layeredIcon = new WebMarkupContainer(ID_LAYERED_ICON);
        if (org.apache.commons.lang3.StringUtils.isNotBlank(compositedIcon.getTitle())) {
            layeredIcon.add(AttributeAppender.append("title", compositedIcon.getTitle()));
        }
        add(layeredIcon);
        WebComponent basicIcon = new WebComponent(ID_BASIC_ICON);
        if (compositedIcon.hasBasicIcon()) {
            basicIcon.add(AttributeAppender.append("class", compositedIcon.getBasicIcon()));
            if (compositedIcon.hasBasicIconHtmlColor()) {
                basicIcon.add(AttributeAppender.append("style", "color:" + compositedIcon.getBasicIconHtmlColor()));
            }
        }
        layeredIcon.add(basicIcon);

        RepeatingView listItems = new RepeatingView(ID_LAYER_ICONS);
        for (LayerIcon layerIcon : compositedIcon.getLayerIcons()) {
            if (layerIcon == null) {
                continue;
            }
            if (StringUtils.isNotEmpty(layerIcon.getIconType().getCssClass())) {
                WebComponent icon = new WebComponent(listItems.newChildId());
                icon.add(AttributeAppender.append("class", layerIcon.getIconType().getCssClass()));
                if (StringUtils.isNotEmpty(layerIcon.getIconType().getColor())) {
                    icon.add(AttributeAppender.append("style", "color: " + layerIcon.getIconType().getColor()));
                }
                listItems.add(icon);
            }
        }
        layeredIcon.add(listItems);

//        ListView<IconType> layerIcons = new ListView<IconType>(ID_LAYER_ICONS, new IModel<List<IconType>>(){
//
//                @Override
//                public List<IconType> getObject() {
//                    return compositedIcon.getLayerIcons();
//                }
//
//            })
//        {
//
//            private static final long serialVersionUID = 1L;
//
//            @Override
//            protected void populateItem(ListItem<IconType> item) {
//                if (item.getModelObject() == null){
//                    return;
//                }
//                if (StringUtils.isNotEmpty(item.getModelObject().getCssClass())) {
//                    add(AttributeAppender.append("class", item.getModelObject().getCssClass()));
//                    if (StringUtils.isNotEmpty(item.getModelObject().getColor())) {
//                        add(AttributeAppender.append("style", item.getModelObject().getColor()));
//                    }
//                }
//            }
//        };
    }
}

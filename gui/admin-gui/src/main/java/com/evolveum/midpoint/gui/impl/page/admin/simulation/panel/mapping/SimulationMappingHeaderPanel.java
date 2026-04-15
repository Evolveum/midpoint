/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */
package com.evolveum.midpoint.gui.impl.page.admin.simulation.panel.mapping;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.util.GuiDisplayTypeUtil;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.panel.IconWithLabel;
import com.evolveum.midpoint.gui.impl.page.admin.simulation.util.MappingUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DisplayType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.IconType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingStrengthType;

import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.Nullable;

/**
 * Header panel showing basic mapping information in simulation details.
 */
public class SimulationMappingHeaderPanel extends BasePanel<MappingUtil.MappingInfo> {

    private static final String ID_INFO = "info";
    private static final String ID_MAPPING_NAME = "mappingName";
    private static final String ID_SOURCE = "source";
    private static final String ID_TARGET = "target";
    private static final String ID_STRENGTH = "strength";

    public SimulationMappingHeaderPanel(String id, IModel<MappingUtil.MappingInfo> model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        MappingUtil.MappingInfo info = getModelObject();

        add(createInfoBox(info));
        add(new Label(ID_MAPPING_NAME, Model.of(getMappingName(info))));
        add(new Label(ID_SOURCE, Model.of(getSource(info))));
        add(new Label(ID_TARGET, Model.of(getTarget(info))));
        add(createStrengthComponent(info));
    }

    private WebMarkupContainer createInfoBox(MappingUtil.MappingInfo info) {
        WebMarkupContainer infoBox = new WebMarkupContainer(ID_INFO);
        infoBox.setVisible(isNormalStrength(info));
        return infoBox;
    }

    private IconWithLabel createStrengthComponent(MappingUtil.MappingInfo info) {
        return new IconWithLabel(ID_STRENGTH, Model.of(resolveStrengthLabel(info))) {
            @Override
            protected @Nullable String getIconCssClass() {
                DisplayType display = GuiDisplayTypeUtil.getDisplayTypeForStrengthOfMapping(
                        "text-muted", info != null ? info.mappingStrength() : null);
                IconType icon = display != null ? display.getIcon() : null;
                return icon != null ? icon.getCssClass() : null;
            }
        };
    }

    private boolean isNormalStrength(MappingUtil.MappingInfo info) {
        return info != null && MappingStrengthType.NORMAL.equals(info.mappingStrength());
    }

    private String getMappingName(MappingUtil.MappingInfo info) {
        return info != null ? info.mappingName() : null;
    }

    private String getSource(MappingUtil.MappingInfo info) {
        return info != null ? info.source() : null;
    }

    private String getTarget(MappingUtil.MappingInfo info) {
        return info != null ? info.target() : null;
    }

    private String resolveStrengthLabel(MappingUtil.MappingInfo info) {
        MappingStrengthType strength = info != null ? info.mappingStrength() : null;
        return strength != null ? strength.value() : MappingStrengthType.NORMAL.value();
    }
}

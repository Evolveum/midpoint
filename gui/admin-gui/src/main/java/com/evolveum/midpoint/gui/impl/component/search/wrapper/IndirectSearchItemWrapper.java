/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component.search.wrapper;

import com.evolveum.midpoint.gui.api.util.GuiDisplayTypeUtil;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.search.panel.IndirectSearchItemPanel;
import com.evolveum.midpoint.util.DisplayableValue;
import com.evolveum.midpoint.gui.impl.component.search.SearchValue;
import com.evolveum.midpoint.xml.ns._public.common.common_3.IndirectSearchItemConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SearchBoxModeType;

import org.apache.commons.lang3.StringUtils;

public class IndirectSearchItemWrapper extends AbstractSearchItemWrapper<Boolean> {

    private IndirectSearchItemConfigurationType indirectConfig;
    public IndirectSearchItemWrapper(IndirectSearchItemConfigurationType indirectConfig) {
        super();
        this.indirectConfig = indirectConfig;
    }

    @Override
    public boolean isVisible() {
        return indirectConfig == null
                || WebComponentUtil.getElementVisibility(indirectConfig.getVisibility());
    }

    @Override
    public Class<IndirectSearchItemPanel> getSearchItemPanelClass() {
        return IndirectSearchItemPanel.class;
    }

    @Override
    public DisplayableValue<Boolean> getDefaultValue() {
        return new SearchValue<>(indirectConfig.isIndirect());
    }

    @Override
    public String getName() {
        var display = indirectConfig == null ? null : indirectConfig.getDisplay();
        var name = GuiDisplayTypeUtil.getTranslatedLabel(display);
        return StringUtils.isEmpty(name) ? "abstractRoleMemberPanel.indirectMembers" : name;
    }

    @Override
    public String getHelp() {
        var display = indirectConfig == null ? null : indirectConfig.getDisplay();
        var help = GuiDisplayTypeUtil.getHelp(display);
        return StringUtils.isEmpty(help) ? "abstractRoleMemberPanel.indirectMembers.tooltip" : help;
    }

    @Override
    public String getTitle() {
        var display = indirectConfig == null ? null : indirectConfig.getDisplay();
        return GuiDisplayTypeUtil.getTooltip(display);
    }

    @Override
    public boolean isApplyFilter(SearchBoxModeType searchBoxMode) {
        return isVisible();
    }
}

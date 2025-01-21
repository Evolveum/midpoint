/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component.search.wrapper;

import com.evolveum.midpoint.gui.api.util.GuiDisplayTypeUtil;
import com.evolveum.midpoint.gui.api.util.LocalizationUtil;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.search.panel.ScopeSearchItemPanel;
import com.evolveum.midpoint.util.DisplayableValue;
import com.evolveum.midpoint.gui.impl.component.search.SearchValue;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ScopeSearchItemConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SearchBoxScopeType;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;

public class ScopeSearchItemWrapper extends AbstractSearchItemWrapper<SearchBoxScopeType> {

    private ScopeSearchItemConfigurationType scopeConfig;

    public ScopeSearchItemWrapper(ScopeSearchItemConfigurationType scopeConfig) {
        super();
        this.scopeConfig = scopeConfig;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    public boolean isVisible() {
        return scopeConfig == null || WebComponentUtil.getElementVisibility(scopeConfig.getVisibility());
    }

    @Override
    public Class<ScopeSearchItemPanel> getSearchItemPanelClass() {
        return ScopeSearchItemPanel.class;
    }

    @Override
    public DisplayableValue<SearchBoxScopeType> getDefaultValue() {
        return new SearchValue<>(scopeConfig.getDefaultValue());
    }

    @Override
    public IModel<String> getName() {
        return new LoadableDetachableModel<>() {
            @Override
            protected String load() {
                var display = scopeConfig == null ? null : scopeConfig.getDisplay();
                var name = GuiDisplayTypeUtil.getTranslatedLabel(display);
                return StringUtils.isEmpty(name) ? LocalizationUtil.translate("abstractRoleMemberPanel.searchScope") : name;
            }
        };
    }

    @Override
    public IModel<String> getHelp() {
        return new LoadableDetachableModel<>() {
            @Override
            protected String load() {
                var display = scopeConfig == null ? null : scopeConfig.getDisplay();
                var help = GuiDisplayTypeUtil.getHelp(display);
                return StringUtils.isEmpty(help) ? LocalizationUtil.translate("abstractRoleMemberPanel.searchScope.tooltip") : help;
            }
        };
    }

    @Override
    public IModel<String> getTitle() {
        return new LoadableDetachableModel<>() {
            @Override
            protected String load() {
                var display = scopeConfig == null ? null : scopeConfig.getDisplay();
                return GuiDisplayTypeUtil.getTooltip(display);
            }
        };
    }
}

/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.search.refactored;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.util.DisplayableValue;
import com.evolveum.midpoint.web.component.search.SearchValue;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

import org.apache.commons.lang3.StringUtils;

public class ProjectSearchItemWrapper extends AbstractRoleSearchItemWrapper{

    public ProjectSearchItemWrapper(SearchConfigurationWrapper searchConfig) {
        super(searchConfig);
    }

    @Override
    public boolean isEnabled() {
        return !getSearchConfig().isIndirect();
    }

    public boolean isVisible() {
        return !getSearchConfig().isIndirect();
    }

    @Override
    public Class<ProjectSearchItemPanel> getSearchItemPanelClass() {
        return ProjectSearchItemPanel.class;
    }

    @Override
    public DisplayableValue<ObjectReferenceType> getDefaultValue() {
        return new SearchValue<>(new ObjectReferenceType());
    }

    @Override
    public String getName() {
        return WebComponentUtil.getTranslatedPolyString(getSearchConfig().getConfig().getProjectConfiguration().getDisplay().getLabel());
    }

    @Override
    public String getHelp() {
//        if (getSearchConfig().getConfig().getProjectConfiguration().getDisplay().getHelp() != null){
        return WebComponentUtil.getTranslatedPolyString(getSearchConfig().getConfig().getProjectConfiguration().getDisplay().getHelp());
//        }
//        String help = projectRefDef.getHelp();
//        if (StringUtils.isNotEmpty(help)) {
//            return getPageBase().createStringResource(help);
//        }
//        return projectRefDef.getDocumentation();
    }

    @Override
    public String getTitle() {
        return ""; //todo
    }
}

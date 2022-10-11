/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component.search;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismReferenceDefinition;
import com.evolveum.midpoint.util.DisplayableValue;
import com.evolveum.midpoint.web.component.search.SearchValue;

import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SearchBoxModeType;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.model.IModel;

public class ProjectSearchItemWrapper extends AbstractRoleSearchItemWrapper{

    public ProjectSearchItemWrapper(SearchConfigurationWrapper searchConfig) {
        super(searchConfig);
    }

    @Override
    public boolean isEnabled() {
        return !getSearchConfig().isIndirect();
    }

    @Override
    public boolean isVisible() {
        return !getSearchConfig().isIndirect();
    }

    @Override
    public Class<ProjectSearchItemPanel> getSearchItemPanelClass() {
        return ProjectSearchItemPanel.class;
    }

    @Override
    public DisplayableValue<ObjectReferenceType> getDefaultValue() {
        ObjectReferenceType ref = new ObjectReferenceType();
        ref.setType(OrgType.COMPLEX_TYPE);
        return new SearchValue<>(ref);
    }

    @Override
    public DisplayableValue<ObjectReferenceType> getValue() {
        if (getSearchConfig().getProjectRef() == null) {
            return getDefaultValue();
        }
        return new SearchValue<>(getSearchConfig().getProjectRef());
    }

    @Override
    protected String getNameResourceKey() {
        return "abstractRoleMemberPanel.project";
    }

    @Override
    protected String getHelpResourceKey() {
        return "";
    }

    @Override
    public String getHelp() {
        if (StringUtils.isNotEmpty(help)) {
             return help;
        }
        String help = getProjectRefDef().getHelp();
        if (StringUtils.isNotEmpty(help)) {
            return help;
        }
        return getProjectRefDef().getDocumentation();
    }

    @Override
    public String getTitle() {
        return ""; //todo
    }

    @Override
    public boolean isApplyFilter(SearchBoxModeType searchBoxMode) {
        return true;
    }

    public PrismReferenceDefinition getProjectRefDef() {
        return getReferenceDefinition(AssignmentType.F_ORG_REF);
    }
}

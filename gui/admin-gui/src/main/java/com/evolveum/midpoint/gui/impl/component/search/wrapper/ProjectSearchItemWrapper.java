/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component.search.wrapper;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.search.panel.ProjectSearchItemPanel;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismReferenceDefinition;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.util.DisplayableValue;
import com.evolveum.midpoint.web.component.search.SearchValue;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.lang3.StringUtils;

public class ProjectSearchItemWrapper extends AbstractSearchItemWrapper<ObjectReferenceType> {

    private UserInterfaceFeatureType projectConfig;

    public ProjectSearchItemWrapper(UserInterfaceFeatureType projectConfig) {
        super();
        this.projectConfig = projectConfig;
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
    public String getName() {
        return "abstractRoleMemberPanel.project";
    }


    @Override
    public String getHelp() {
        String help = projectConfig.getDisplay() != null ? WebComponentUtil.getTranslatedPolyString(projectConfig.getDisplay().getHelp()) : null;
        if (StringUtils.isNotEmpty(help)) {
             return help;
        }
        help = getProjectRefDef().getHelp();
        if (StringUtils.isNotEmpty(help)) {
            return help;
        }
        return getProjectRefDef().getDocumentation();
    }

    @Override
    public String getTitle() {
        return ""; //todo
    }

    public PrismReferenceDefinition getProjectRefDef() {
        return getReferenceDefinition(AssignmentType.F_ORG_REF);
    }

    protected PrismReferenceDefinition getReferenceDefinition(ItemName refName) {
        return PrismContext.get().getSchemaRegistry()
                .findContainerDefinitionByCompileTimeClass(AssignmentType.class)
                .findReferenceDefinition(refName);
    }

    @Override
    public boolean isVisible() {
        return true;
    }
}

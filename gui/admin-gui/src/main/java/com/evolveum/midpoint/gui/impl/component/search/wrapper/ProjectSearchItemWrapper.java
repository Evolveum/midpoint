/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.component.search.wrapper;

import com.evolveum.midpoint.gui.api.util.GuiDisplayTypeUtil;
import com.evolveum.midpoint.gui.api.util.LocalizationUtil;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.search.panel.ProjectSearchItemPanel;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismReferenceDefinition;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.util.DisplayableValue;
import com.evolveum.midpoint.gui.impl.component.search.SearchValue;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.jetbrains.annotations.NotNull;

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
    public @NotNull IModel<String> getName() {
        return new LoadableDetachableModel<>() {
            @Override
            protected String load() {
                var display = projectConfig == null ? null : projectConfig.getDisplay();
                var name = GuiDisplayTypeUtil.getTranslatedLabel(display);
                return StringUtils.isEmpty(name) ? LocalizationUtil.translate("abstractRoleMemberPanel.project") : name;
            }
        };
    }


    @Override
    public @NotNull IModel<String> getHelp() {
        return new LoadableDetachableModel<>() {
            @Override
            protected String load() {
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
        };
    }

    @Override
    public @NotNull IModel<String> getTitle() {
        return new LoadableDetachableModel<>() {
            @Override
            protected String load() {
                var display = projectConfig == null ? null : projectConfig.getDisplay();
                return GuiDisplayTypeUtil.getTooltip(display);
            }
        };
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
        return projectConfig == null
                || WebComponentUtil.getElementVisibility(projectConfig.getVisibility());
    }
}

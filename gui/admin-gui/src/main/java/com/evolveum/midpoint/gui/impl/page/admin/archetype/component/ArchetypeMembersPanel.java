/*
 * Copyright (C) 2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.admin.archetype.component;

import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.impl.page.admin.abstractrole.component.AbstractRoleMemberPanel;
import com.evolveum.midpoint.gui.impl.page.admin.focus.FocusDetailsModels;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ArchetypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ContainerPanelConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationTypeType;

@PanelType(name = "archetypeMembers")
@PanelInstance(identifier = "archetypeMembers",
        applicableForType = ArchetypeType.class,
        applicableForOperation = OperationTypeType.MODIFY,
        display = @PanelDisplay(label = "pageRole.members", icon = GuiStyleConstants.CLASS_GROUP_ICON, order = 80))
@PanelInstance(identifier = "archetypeGovernance",
        applicableForType = ArchetypeType.class,
        applicableForOperation = OperationTypeType.MODIFY,
        display = @PanelDisplay(label = "pageRole.governance", icon = GuiStyleConstants.CLASS_GROUP_ICON, order = 90))
public class ArchetypeMembersPanel extends AbstractRoleMemberPanel<ArchetypeType> {

    public ArchetypeMembersPanel(String id, FocusDetailsModels model, ContainerPanelConfigurationType config) {
        super(id, model, config);
    }

    @Override
    protected List<InlineMenuItem> createRowActions() {
        List<InlineMenuItem> menu = new ArrayList<>();
        createAssignMemberRowAction(menu);
        createRecomputeMemberRowAction(menu);
        return menu;
    }

    @Override
    protected String getStorageKeyTabSuffix() {
        return "archetypeMembers";
    }
}

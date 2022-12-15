/*
 * Copyright (C) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.archetype.component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.page.admin.abstractrole.component.AbstractRoleMemberPanel;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.FocusDetailsModels;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelInstances;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ArchetypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ContainerPanelConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationTypeType;

@PanelType(name = "archetypeMembers")
@PanelInstances(value = {
        @PanelInstance(identifier = "archetypeMembers",
                applicableForType = ArchetypeType.class,
                applicableForOperation = OperationTypeType.MODIFY,
                display = @PanelDisplay(label = "pageRole.members", icon = GuiStyleConstants.CLASS_GROUP_ICON, order = 80)),
        @PanelInstance(identifier = "archetypeGovernance",
                applicableForType = ArchetypeType.class,
                applicableForOperation = OperationTypeType.MODIFY,
                display = @PanelDisplay(label = "pageRole.governance", icon = GuiStyleConstants.CLASS_GROUP_ICON, order = 90))
})
public class ArchetypeMembersPanel extends AbstractRoleMemberPanel<ArchetypeType> {

    public ArchetypeMembersPanel(String id, FocusDetailsModels model, ContainerPanelConfigurationType config) {
        super(id, model, config);
    }

    @Override
    protected List<QName> getSupportedRelations() {
        return Arrays.asList(SchemaConstants.ORG_DEFAULT);
    }

    @Override
    protected List<QName> getDefaultSupportedObjectTypes(boolean includeAbstractTypes) {
        return WebComponentUtil.createAssignmentHolderTypeQnamesList();
    }

    @Override
    protected List<InlineMenuItem> createRowActions() {
        List<InlineMenuItem> menu = new ArrayList<>();
        createAssignMemberRowAction(menu);
        createRecomputeMemberRowAction(menu);
        return menu;
    }

    @Override
    protected <AH extends AssignmentHolderType> Class<AH> getChoiceForAllTypes() {
        return (Class<AH>) AssignmentHolderType.class;
    }

    @Override
    protected String getStorageKeyTabSuffix() {
        return "archetypeMembers";
    }
}

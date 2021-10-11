/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.archetype;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;

import com.evolveum.midpoint.web.page.admin.roles.MemberOperationsHelper;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.page.admin.roles.AbstractRoleMemberPanel;
import com.evolveum.midpoint.web.page.admin.roles.AvailableRelationDto;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ArchetypeType;

import javax.xml.namespace.QName;

public class ArchetypeMembersPanel extends AbstractRoleMemberPanel<ArchetypeType> {

    public ArchetypeMembersPanel(String id, IModel<ArchetypeType> model) {
        super(id, model);
    }

    private static final long serialVersionUID = 1L;

    @Override
    protected AvailableRelationDto getSupportedRelations() {
        return new AvailableRelationDto(Arrays.asList(SchemaConstants.ORG_DEFAULT));
    }

    @Override
    protected List<QName> getSupportedObjectTypes(boolean includeAbstractTypes) {
        return WebComponentUtil.createAssignmentHolderTypeQnamesList();
    }

    protected void assignMembers(AjaxRequestTarget target, AvailableRelationDto availableRelationList,
            List<QName> objectTypes, List<ObjectReferenceType> archetypeRefList, boolean isOrgTreePanelVisible) {
        MemberOperationsHelper.assignArchetypeMembers(getPageBase(), getModelObject(), target, availableRelationList,
                objectTypes, archetypeRefList);
    }



    @Override
    protected List<InlineMenuItem> createRowActions() {
        List<InlineMenuItem> menu =  new ArrayList<InlineMenuItem>();
        createAssignMemberRowAction(menu);
        createRecomputeMemberRowAction(menu);
        return menu;

    }
}

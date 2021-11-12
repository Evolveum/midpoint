/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.abstractrole.component;

import java.util.ArrayList;
import java.util.List;
import javax.xml.namespace.QName;

import org.apache.wicket.ajax.AjaxRequestTarget;

import com.evolveum.midpoint.gui.api.component.ChooseArchetypeMemberPopup;
import com.evolveum.midpoint.gui.api.component.ChooseMemberPopup;
import com.evolveum.midpoint.gui.api.component.ChooseOrgMemberPopup;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Helps with GUI aspects of member operations on abstract roles.
 */
public class MemberOperationsGuiHelper {

    private static final Trace LOGGER = TraceManager.getTrace(AbstractRoleMemberPanel.class);

    public static <R extends AbstractRoleType> void assignMembers(PageBase pageBase, R targetRefObject, AjaxRequestTarget target,
            RelationSearchItemConfigurationType relationConfig, List<QName> objectTypes) {
        assignMembers(pageBase, targetRefObject, target, relationConfig, objectTypes, true);
    }

    public static <R extends AbstractRoleType> void assignMembers(PageBase pageBase, R targetRefObject, AjaxRequestTarget target,
            RelationSearchItemConfigurationType relationConfig, List<QName> objectTypes, boolean isOrgTreePanelVisible) {
        assignMembers(pageBase, targetRefObject, target, relationConfig, objectTypes, new ArrayList<>(), isOrgTreePanelVisible);
    }

    public static <O extends ObjectType, R extends AbstractRoleType> void assignMembers(PageBase pageBase, R targetRefObject, AjaxRequestTarget target,
            RelationSearchItemConfigurationType relationConfig, List<QName> objectTypes, List<ObjectReferenceType> archetypeRefList, boolean isOrgTreePanelVisible) {

        ChooseMemberPopup<O, R> browser = new ChooseMemberPopup<>(pageBase.getMainPopupBodyId(), relationConfig, null) {
            private static final long serialVersionUID = 1L;

            @Override
            protected R getAssignmentTargetRefObject(){
                return targetRefObject;
            }

            @Override
            protected List<QName> getAvailableObjectTypes(){
                return objectTypes;
            }

            @Override
            protected List<ObjectReferenceType> getArchetypeRefList(){
                return archetypeRefList;
            }

            @Override
            protected boolean isOrgTreeVisible(){
                return isOrgTreePanelVisible;
            }
        };
        browser.setOutputMarkupId(true);
        pageBase.showMainPopup(browser, target);
    }

    public static <O extends ObjectType> void assignOrgMembers(PageBase pageBase, OrgType targetRefObject, AjaxRequestTarget target,
            RelationSearchItemConfigurationType relationConfig, List<QName> objectTypes, List<ObjectReferenceType> archetypeRefList) {
        ChooseOrgMemberPopup<O> browser = new ChooseOrgMemberPopup<>(pageBase.getMainPopupBodyId(), relationConfig) {

            private static final long serialVersionUID = 1L;

            @Override
            protected OrgType getAssignmentTargetRefObject(){
                return targetRefObject;
            }

            @Override
            protected List<QName> getAvailableObjectTypes(){
                return objectTypes;
            }

            @Override
            protected List<ObjectReferenceType> getArchetypeRefList(){
                return archetypeRefList;
            }
        };

        browser.setOutputMarkupId(true);
        pageBase.showMainPopup(browser, target);
    }

    public static <O extends AssignmentHolderType> void assignArchetypeMembers(PageBase pageBase, ArchetypeType targetRefObject, AjaxRequestTarget target,
            RelationSearchItemConfigurationType relationConfig, List<QName> objectTypes, List<ObjectReferenceType> archetypeRefList) {
        ChooseArchetypeMemberPopup<O> browser = new ChooseArchetypeMemberPopup<>(pageBase.getMainPopupBodyId(), relationConfig) {

            private static final long serialVersionUID = 1L;

            @Override
            protected ArchetypeType getAssignmentTargetRefObject(){
                return targetRefObject;
            }

            @Override
            protected List<QName> getAvailableObjectTypes(){
                return objectTypes;
            }

            @Override
            protected List<ObjectReferenceType> getArchetypeRefList(){
                return archetypeRefList;
            }
        };

        browser.setOutputMarkupId(true);
        pageBase.showMainPopup(browser, target);
    }
}

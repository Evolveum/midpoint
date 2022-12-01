/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.roles;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.xml.namespace.QName;

import org.apache.wicket.ajax.AjaxRequestTarget;

import com.evolveum.midpoint.gui.api.component.ChooseArchetypeMemberPopup;
import com.evolveum.midpoint.gui.api.component.ChooseMemberPopup;
import com.evolveum.midpoint.gui.api.component.ChooseOrgMemberPopup;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.page.admin.abstractrole.component.MemberOperationsHelper;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.page.admin.roles.AbstractRoleMemberPanel.QueryScope;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Temporary implementation. Delegates everything (except for pure GUI things) to the new {@link MemberOperationsHelper}.
 */
public class MemberOperationsHelperOld {

    static <R extends AbstractRoleType> Task createUnassignMembersTask(PageBase pageBase, R targetObject, QueryScope scope,
            ObjectQuery query, Collection<QName> relations, QName type, AjaxRequestTarget target) {
        return MemberOperationsHelper.createUnassignMembersTask(
                targetObject, convert(scope), type, query, relations, target, pageBase);
    }

    static <R extends AbstractRoleType> void unassignMembersPerformed(PageBase pageBase, R targetObject, QueryScope scope,
            ObjectQuery query, Collection<QName> relations, QName type, AjaxRequestTarget target) {
        MemberOperationsHelper.createAndSubmitUnassignMembersTask(
                targetObject, convert(scope), type, query, relations, target, pageBase);
    }

    static <R extends AbstractRoleType> void deleteMembersPerformed(R targetObject,
            PageBase pageBase, QueryScope scope, ObjectQuery query, AjaxRequestTarget target) {
        MemberOperationsHelper.createAndSubmitDeleteMembersTask(
                targetObject, convert(scope), AssignmentHolderType.COMPLEX_TYPE, query, target, pageBase);
    }

    static <R extends AbstractRoleType> void recomputeMembersPerformed(R targetObject,
            PageBase pageBase, QueryScope scope, ObjectQuery query, AjaxRequestTarget target) {
        MemberOperationsHelper.createAndSubmitRecomputeMembersTask(
                targetObject, convert(scope), AssignmentHolderType.COMPLEX_TYPE, query, target, pageBase);
    }

    static <R extends AbstractRoleType> Task createRecomputeMembersTask(R targetObject, PageBase pageBase, QueryScope scope,
            ObjectQuery query, AjaxRequestTarget target) {
        return MemberOperationsHelper.createRecomputeMembersTask(
                targetObject, convert(scope), AssignmentHolderType.COMPLEX_TYPE, query, target, pageBase);
    }

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
            protected R getAssignmentTargetRefObject() {
                return targetRefObject;
            }

            @Override
            protected List<QName> getAvailableObjectTypes() {
                return objectTypes;
            }

            @Override
            protected List<ObjectReferenceType> getArchetypeRefList() {
                return archetypeRefList;
            }

            @Override
            protected boolean isOrgTreeVisible() {
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
            protected OrgType getAssignmentTargetRefObject() {
                return targetRefObject;
            }

            @Override
            protected List<QName> getAvailableObjectTypes() {
                return objectTypes;
            }

            @Override
            protected List<ObjectReferenceType> getArchetypeRefList() {
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
            protected ArchetypeType getAssignmentTargetRefObject() {
                return targetRefObject;
            }

            @Override
            protected List<QName> getAvailableObjectTypes() {
                return objectTypes;
            }

            @Override
            protected List<ObjectReferenceType> getArchetypeRefList() {
                return archetypeRefList;
            }
        };

        browser.setOutputMarkupId(true);
        pageBase.showMainPopup(browser, target);
    }

    static <R extends AbstractRoleType> ObjectQuery createDirectMemberQuery(R targetObject, QName objectType,
            Collection<QName> relations, ObjectReferenceType tenant, ObjectReferenceType project) {
        return MemberOperationsHelper.createDirectMemberQuery(targetObject, objectType, relations, tenant, project);
    }

    private static com.evolveum.midpoint.gui.impl.page.admin.abstractrole.component.AbstractRoleMemberPanel.QueryScope convert(
            QueryScope scope) {
        if (scope == null) {
            return null;
        }
        switch (scope) {
            case SELECTED:
                return com.evolveum.midpoint.gui.impl.page.admin.abstractrole.component.AbstractRoleMemberPanel.QueryScope.SELECTED;
            case ALL:
                return com.evolveum.midpoint.gui.impl.page.admin.abstractrole.component.AbstractRoleMemberPanel.QueryScope.ALL;
            case ALL_DIRECT:
                return com.evolveum.midpoint.gui.impl.page.admin.abstractrole.component.AbstractRoleMemberPanel.QueryScope.ALL_DIRECT;
            default:
                throw new AssertionError();
        }
    }
}

/*
 * Copyright (c) 2015-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.users.component;

import java.util.Collection;
import java.util.List;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.web.session.MemberPanelStorage;
import com.evolveum.midpoint.web.session.PageStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.page.admin.roles.AbstractRoleMemberPanel;
import com.evolveum.midpoint.web.page.admin.roles.AvailableRelationDto;
import com.evolveum.midpoint.web.page.admin.roles.MemberOperationsHelper;

public class OrgMemberPanel extends AbstractRoleMemberPanel<OrgType> {

    private static final Trace LOGGER = TraceManager.getTrace(OrgMemberPanel.class);


    protected static final String ID_SEARCH_BY_TYPE = "searchByType";

    protected static final ObjectTypes OBJECT_TYPES_DEFAULT = ObjectTypes.USER;



    protected static final String DOT_CLASS = OrgMemberPanel.class.getName() + ".";

    private static final long serialVersionUID = 1L;

    public OrgMemberPanel(String id, IModel<OrgType> model) {
        super(id, model);
        setOutputMarkupId(true);
    }

    @Override
    protected void initLayout() {
        super.initLayout();
    }

    @Override
    protected ObjectQuery createMemberQuery(boolean indirect, Collection<QName> relations) {
        ObjectTypes searchType = getSearchType();
        if (SearchBoxScopeType.ONE_LEVEL.equals(getSearchScope())) {
            if (AssignmentHolderType.class.isAssignableFrom(searchType.getClassDefinition())) {
                return super.createMemberQuery(indirect, relations);
            }
            else {
                ObjectReferenceType ref = MemberOperationsHelper.createReference(getModelObject(), getSelectedRelation());
                return getPageBase().getPrismContext().queryFor(searchType.getClassDefinition())
                        .type(searchType.getClassDefinition())
                        .isDirectChildOf(ref.asReferenceValue()).build();
            }
        }
        return getSubtreeScopeMembersQuery();
    }

    @Override
    protected ObjectQuery getActionQuery(QueryScope scope, Collection<QName> relations) {
        if (SearchBoxScopeType.ONE_LEVEL.equals(getSearchScope()) ||
                (SearchBoxScopeType.SUBTREE.equals(getSearchScope()) && !QueryScope.ALL.equals(scope))) {
            return super.getActionQuery(scope, relations);
        } else {
            return getSubtreeScopeMembersQuery();
        }
    }

    @Override
    protected ObjectQuery createAllMemberQuery(Collection<QName> relations) {
        return getPrismContext().queryFor(AssignmentHolderType.class)
                .item(AssignmentHolderType.F_ROLE_MEMBERSHIP_REF).ref(MemberOperationsHelper.createReferenceValuesList(getModelObject(), relations))
                .build();
    }

    private ObjectQuery getSubtreeScopeMembersQuery(){
        String oid = getModelObject().getOid();
        ObjectTypes searchType = getSearchType();

        ObjectReferenceType ref = MemberOperationsHelper.createReference(getModelObject(), getSelectedRelation());
        ObjectQuery query = getPageBase().getPrismContext().queryFor(searchType.getClassDefinition())
                .type(searchType.getClassDefinition())
                .isChildOf(ref.asReferenceValue()).build();

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Searching members of org {} with query:\n{}", oid, query.debugDump());
        }
        return query;
    }

    @Override
    protected void assignMembers(AjaxRequestTarget target, AvailableRelationDto availableRelationList,
                                 List<QName> objectTypes, List<ObjectReferenceType> archetypeRefList, boolean isOrgTreePanelVisible) {
        MemberOperationsHelper.assignOrgMembers(getPageBase(), getModelObject(), target, availableRelationList, objectTypes, archetypeRefList);
    }

    @Override
    protected List<QName> getSupportedObjectTypes(boolean includeAbstractTypes) {
            List<QName> objectTypes = WebComponentUtil.createAssignmentHolderTypeQnamesList();
            objectTypes.remove(ShadowType.COMPLEX_TYPE);
            objectTypes.remove(ObjectType.COMPLEX_TYPE);
            if (!includeAbstractTypes){
                objectTypes.remove(AssignmentHolderType.COMPLEX_TYPE);
            }
            return objectTypes;
    }

    @Override
    protected List<QName> getNewMemberObjectTypes() {
        List<QName> objectTypes = WebComponentUtil.createFocusTypeList();
        objectTypes.add(ResourceType.COMPLEX_TYPE);
        return objectTypes;
    }

    @Override
    protected QName getObjectTypesListParentType(){
        return AssignmentHolderType.COMPLEX_TYPE;
    }

    @Override
    protected <O extends ObjectType> Class<O> getDefaultObjectType() {
        return getMemberPanelStorage().getType() != null ? (Class) WebComponentUtil.qnameToClass(getPageBase().getPrismContext(),
                getMemberPanelStorage().getType().getTypeQName()) : (Class) UserType.class;
    }

    @Override
    protected AvailableRelationDto getSupportedRelations() {
        AvailableRelationDto availableRelationDto =
                new AvailableRelationDto(WebComponentUtil.getCategoryRelationChoices(AreaCategoryType.ORGANIZATION, getPageBase()));
        availableRelationDto.setDefaultRelation(PrismConstants.Q_ANY);
        return availableRelationDto;
    }

    @Override
    protected MemberPanelStorage getMemberPanelStorage(){
        String storageKey = WebComponentUtil.getStorageKeyForTableId(getTableId(getComplexTypeQName()));
        PageStorage storage = null;
        if (StringUtils.isNotEmpty(storageKey)) {
            storage = getPageBase().getSessionStorage().getPageStorageMap().get(storageKey);
            if (storage == null) {
                storage = getPageBase().getSessionStorage().initPageStorage(storageKey);
            }
        }
        return (MemberPanelStorage) storage;
    }

}

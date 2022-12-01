/*
 * Copyright (c) 2015-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.users.component;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import javax.xml.namespace.QName;

import org.apache.cxf.common.util.CollectionUtils;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.page.admin.abstractrole.component.MemberOperationsHelper;
import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.page.admin.roles.AbstractRoleMemberPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

public class OrgMemberPanel extends AbstractRoleMemberPanel<OrgType> {
    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(OrgMemberPanel.class);

    public OrgMemberPanel(String id, IModel<OrgType> model, PageBase parentPage) {
        super(id, model);
        setOutputMarkupId(true);
    }

    @Override
    protected void initLayout() {
        super.initLayout();
    }

    @Override
    protected ObjectQuery getActionQuery(QueryScope scope, Collection<QName> relations) {
        if (getSearchBoxConfiguration().isSearchScope(SearchBoxScopeType.ONE_LEVEL) ||
                (getSearchBoxConfiguration().isSearchScope(SearchBoxScopeType.SUBTREE)
                        && !QueryScope.ALL.equals(scope))) {
            return super.getActionQuery(scope, relations);
        } else {
            String oid = getModelObject().getOid();

            ObjectReferenceType ref = ObjectTypeUtil.createObjectRef(getModelObject(), getSearchBoxConfiguration().getDefaultRelation());
            ObjectQuery query = getPageBase().getPrismContext().queryFor(getSearchTypeClass())
                    .type(getSearchTypeClass())
                    .isChildOf(ref.asReferenceValue()).build();

            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Searching members of org {} with query:\n{}", oid, query.debugDump());
            }
            return query;
        }
    }

    @Override
    protected ObjectQuery createAllMemberQuery(Collection<QName> relations) {
        return getPrismContext().queryFor(AssignmentHolderType.class)
                .item(AssignmentHolderType.F_ROLE_MEMBERSHIP_REF).ref(MemberOperationsHelper.createReferenceValuesList(getModelObject(), relations))
                .build();
    }

    @Override
    protected Class<? extends ObjectType> getChoiceForAllTypes() {
        return AssignmentHolderType.class;
    }

    @Override
    protected List<QName> getDefaultSupportedObjectTypes(boolean includeAbstractTypes) {
        List<QName> objectTypes = WebComponentUtil.createAssignmentHolderTypeQnamesList();
        objectTypes.remove(ShadowType.COMPLEX_TYPE);
        objectTypes.remove(ObjectType.COMPLEX_TYPE);
        if (!includeAbstractTypes) {
            objectTypes.remove(AssignmentHolderType.COMPLEX_TYPE);
        }
        return objectTypes;
    }

    @Override
    protected List<QName> getNewMemberObjectTypes() {
        List<QName> objectTypes = WebComponentUtil.createAvailableNewObjectsTypesList();
        return objectTypes;
    }

    @Override
    protected Class<? extends ObjectType> getDefaultObjectType() {
        return UserType.class;
    }

    @Override
    protected List<QName> getSupportedRelations() {
        return WebComponentUtil.getCategoryRelationChoices(AreaCategoryType.ORGANIZATION, getPageBase());
    }

    private Class<? extends AssignmentHolderType> getSearchTypeClass() {
        return getMemberPanelStorage().getSearch().getTypeClass();
    }

    @Override
    protected boolean reloadPageOnRefresh() {
        return true;
    }

    @Override
    protected String getStorageKeyTabSuffix() {
        return "orgTreeMembers";
    }

    @Override
    protected List<QName> getRelationsForRecomputeTask() {
        if (CollectionUtils.isEmpty(getSearchBoxConfiguration().getSupportedRelations())) {
            return Collections.singletonList(PrismConstants.Q_ANY);
        }
        return super.getRelationsForRecomputeTask();
    }
}

/*
 * Copyright (C) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.roles;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.model.api.authentication.CompiledGuiProfile;
import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismReferenceDefinition;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.builder.S_AtomicFilterExit;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.search.Search;
import com.evolveum.midpoint.web.component.search.SearchItem;

import com.evolveum.midpoint.web.session.MemberPanelStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.wicket.model.PropertyModel;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AbstractRoleCompositedSearchItem extends SearchItem {

    private static final Trace LOGGER = TraceManager.getTrace(RelationSearchItem.class);

    public static final String F_SEARCH_ITEMS = "searchItems";
    private List<SearchItem> searchItems = new ArrayList<>();

    private MemberPanelStorage memberPanelStorage;

    public AbstractRoleCompositedSearchItem(Search search, MemberPanelStorage memberPanelStorage) {
        super(search);
        this.memberPanelStorage = memberPanelStorage;
        create();
    }


    public void create() {
        if (memberPanelStorage.isRelationVisible()) {
            searchItems.add(createRelationItem(getSearch()));
        }
        if (memberPanelStorage.isIndirectVisible()) {
            searchItems.add(createIndirectItem(getSearch()));
        }
        if (isOrg() && memberPanelStorage.isSearchScopeVisible()) {
            searchItems.add(createScopeItem(getSearch()));
        }
        if (isRole()) {
            if (memberPanelStorage.isTenantVisible()) {
                searchItems.add(createTenantItem(getSearch()));
            }
            if (memberPanelStorage.isProjectVisible()) {
                searchItems.add(createProjectItem(getSearch()));
            }
        }
    }

    private SearchItem createScopeItem(Search search) {
        return new ScopeSearchItem(search, new PropertyModel<>(memberPanelStorage, MemberPanelStorage.F_ORG_SEARCH_SCOPE_ITEM));
    }

    private SearchItem createIndirectItem(Search search) {
        return new IndirectSearchItem(search, getMemberPanelStorage()) {
            @Override
            public boolean isApplyFilter() {
                return !memberPanelStorage.isSearchScopeVisible()
                        || !memberPanelStorage.isSearchScope(SearchBoxScopeType.SUBTREE);
            }

            @Override
            protected boolean isPanelVisible() {
                return getMemberPanelStorage() == null
                        || (memberPanelStorage.getSupportedRelations() != null
                        && !getMemberPanelStorage().isSearchScope(SearchBoxScopeType.SUBTREE));
            }
        };
    }

    private SearchItem createTenantItem(Search search) {
        return new TenantSearchItem(search, getMemberPanelStorage()) {

            @Override
            public boolean isApplyFilter() {
                return !memberPanelStorage.isSearchScopeVisible()
                        || (!memberPanelStorage.isSearchScope(SearchBoxScopeType.SUBTREE)
                        && !memberPanelStorage.isRelationVisible()
                        && !memberPanelStorage.isIndirect());
            }

            @Override
            public PrismReferenceDefinition getTenantDefinition() {
                return getReferenceDefinition(AssignmentType.F_TENANT_REF);
            }
        };
    }

    private SearchItem createProjectItem(Search search) {
        return new ProjectSearchItem(search, getMemberPanelStorage()) {
            @Override
            public boolean isApplyFilter() {
                return !memberPanelStorage.isSearchScopeVisible()
                        || (!memberPanelStorage.isSearchScope(SearchBoxScopeType.SUBTREE)
                        && !memberPanelStorage.isRelationVisible()
                        && !memberPanelStorage.isTenantVisible()
                        && !memberPanelStorage.isIndirect());
            }

            @Override
            public PrismReferenceDefinition getProjectRefDef() {
                return getReferenceDefinition(AssignmentType.F_ORG_REF);
            }
        };
    }

    private RelationSearchItem createRelationItem(Search search) {
        return new RelationSearchItem(search, getMemberPanelStorage()) {

            @Override
            public boolean isApplyFilter() {
                return !memberPanelStorage.isSearchScopeVisible()
                        || !memberPanelStorage.isSearchScope(SearchBoxScopeType.SUBTREE);
            }
        };
    }

    public ObjectFilter createFilter(PageBase pageBase, VariablesMap variables) {
        AbstractRoleType object = getParentVariables(variables);
        if (object == null) {
            return null;
        }

        Class type = getSearch().getTypeClass();
        SearchBoxScopeType scope = getMemberPanelStorage().getScopeSearchItem().getDefaultValue();
        if (SearchBoxScopeType.SUBTREE == scope) {
            ObjectReferenceType ref = MemberOperationsHelper.createReference(object, null);
            return pageBase.getPrismContext().queryFor(type).isChildOf(ref.asReferenceValue()).buildFilter();
        }

        PrismContext prismContext = pageBase.getPrismContext();
        List relations;
        QName relation = getMemberPanelStorage().getDefaultRelation();
        if (QNameUtil.match(relation, PrismConstants.Q_ANY)){
            relations = memberPanelStorage.getSupportedRelations();
        } else {
            relations = Collections.singletonList(relation);
        }

        ObjectFilter filter;
        Boolean indirect = getMemberPanelStorage().getIndirectSearchItem().isIndirect();

        if(BooleanUtils.isTrue(indirect)) {
            filter = prismContext.queryFor(type)
                    .item(FocusType.F_ROLE_MEMBERSHIP_REF).ref(MemberOperationsHelper.createReferenceValuesList(object, relations))
                    .buildFilter();
        } else {
            S_AtomicFilterExit q = prismContext.queryFor(type).exists(AssignmentHolderType.F_ASSIGNMENT)
                    .block()
                    .item(AssignmentType.F_TARGET_REF)
                    .ref(MemberOperationsHelper.createReferenceValuesList(object, relations));

            if (!getMemberPanelStorage().isTenantEmpty()) {
                q = q.and().item(AssignmentType.F_TENANT_REF).ref(getMemberPanelStorage().getTenant().getOid());
            }

            if (!getMemberPanelStorage().isProjectEmpty()) {
                q = q.and().item(AssignmentType.F_ORG_REF).ref(getMemberPanelStorage().getProject().getOid());
            }
            filter = q.endBlock().buildFilter();
        }
        return filter;
    }



    @Override
    public String getName() {
        return null;
    }

    @Override
    public Type getSearchItemType() {
        return null;
    }

    private <R extends AbstractRoleType> R getParentVariables(VariablesMap variables) {
        if (variables == null) {
            return null;
        }
        try {
            return (R) variables.getValue(ExpressionConstants.VAR_PARENT_OBJECT, AbstractRoleType.class);
        } catch (SchemaException e) {
            LOGGER.error("Couldn't load parent object.");
        }
        return null;
    }

    protected PrismReferenceDefinition getReferenceDefinition(ItemName refName) {
        return null;
    }

    private boolean isOrg() {
        return getAbstractRoleObject() instanceof OrgType;
    }

    private boolean isRole() {
        return getAbstractRoleObject() instanceof RoleType;
    }

    protected  <R extends AbstractRoleType> R getAbstractRoleObject() {
        return null;
    }

    protected MemberPanelStorage getMemberPanelStorage() {
        return memberPanelStorage;
    }
}

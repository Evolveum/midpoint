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
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.builder.S_AtomicFilterExit;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.search.Search;
import com.evolveum.midpoint.web.component.search.SearchItem;
import com.evolveum.midpoint.web.component.search.SearchSpecialItemPanel;
import com.evolveum.midpoint.web.component.search.SpecialSearchItem;

import com.evolveum.midpoint.web.session.MemberPanelStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.catalina.users.AbstractRole;
import org.apache.wicket.ajax.AjaxRequestTarget;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

public class AbstractRoleCompositedSearchItem extends SearchItem {

    private static final Trace LOGGER = TraceManager.getTrace(RelationSearchItem.class);

    public static final String F_SEARCH_ITEMS = "searchItems";
    private List<SearchItem> searchItems = new ArrayList<>();

    private AbstractRoleMemberSearchConfiguration searchConfig;

    public AbstractRoleCompositedSearchItem(Search search, GuiObjectListPanelConfigurationType additionalPanelConfig) {
        super(search);
        this.searchConfig = new AbstractRoleMemberSearchConfiguration(additionalPanelConfig);
        create();
    }


    public void create() {
        if (isRelationVisible()) {
            searchItems.add(createRelationItem(getSearch()));
//            search.addSpecialItem(createRelationItem(search));
        }
        if (isIndirectVisible()) {
            searchItems.add(createIndirectItem(getSearch()));
//            search.addSpecialItem(createIndirectItem(search));
        }
        if (isSearchScopeVisible()) {
            searchItems.add(createScopeItem(getSearch()));
//            search.addSpecialItem(createScopeItem(search));
        }
        if (isRole()) {
            if (isTenantVisible()) {
                searchItems.add(createTenantItem(getSearch()));
//                search.addSpecialItem(createTenantItem(search));
            }
            if (isProjectVisible()) {
                searchItems.add(createProjectItem(getSearch()));
//                search.addSpecialItem(createProjectItem(search));
            }
        }
    }

    private SearchItem createScopeItem(Search search) {
        return new ScopeSearchItem(search, getMemberPanelStorage(), searchConfig.getDefaultSearchScopeConfiguration());
    }

    private SearchItem createIndirectItem(Search search) {
        return new IndirectSearchItem(search, getMemberPanelStorage(), getSupportedRelations(), searchConfig.getDefaultIndirectConfiguration()) {
            @Override
            public boolean isApplyFilter() {
                return !isSearchScopeVisible()
                        || !SearchBoxScopeType.SUBTREE.equals(getMemberPanelStorage().getOrgSearchScope());
            }

            @Override
            protected boolean isPanelVisible() {
                return getMemberPanelStorage() == null
                        || (getSupportedRelations().getAvailableRelationList() != null
                        && !SearchBoxScopeType.SUBTREE.equals(getMemberPanelStorage().getOrgSearchScope()));
            }
        };
    }

    private SearchItem createTenantItem(Search search) {
        return new TenantSearchItem(search, getMemberPanelStorage(), searchConfig.getDefaultTenantConfiguration()) {

            @Override
            public boolean isApplyFilter() {
                return !isSearchScopeVisible()
                        || (!SearchBoxScopeType.SUBTREE.equals(getMemberPanelStorage().getOrgSearchScope())
                        && !isRelationVisible()
                        && !Boolean.TRUE.equals(getMemberPanelStorage().getIndirect()));
            }

            @Override
            public PrismReferenceDefinition getTenantDefinition() {
                return getReferenceDefinition(AssignmentType.F_TENANT_REF);
            }
        };
    }

    private SearchItem createProjectItem(Search search) {
        return new ProjectSearchItem(search, getMemberPanelStorage(), searchConfig.getDefaultProjectConfiguration()) {
            @Override
            public boolean isApplyFilter() {
                return !isSearchScopeVisible()
                        || (!SearchBoxScopeType.SUBTREE.equals(getMemberPanelStorage().getOrgSearchScope())
                        && !isRelationVisible()
                        && !isTenantVisible()
                        && !Boolean.TRUE.equals(getMemberPanelStorage().getIndirect()));
            }

            @Override
            public PrismReferenceDefinition getProjectRefDef() {
                return getReferenceDefinition(AssignmentType.F_ORG_REF);
            }
        };
    }

    private RelationSearchItem createRelationItem(Search search) {
        return new RelationSearchItem(search, getMemberPanelStorage(), getSupportedRelations(), searchConfig.getDefaultRelationConfiguration()) {

            @Override
            public boolean isApplyFilter() {
                return !isSearchScopeVisible()
                        || !SearchBoxScopeType.SUBTREE.equals(getMemberPanelStorage().getOrgSearchScope());
            }
        };
    }

    public ObjectFilter createFilter(PageBase pageBase, VariablesMap variables) {
        AbstractRoleType object = getParentVariables(variables);
        if (object == null) {
            return null;
        }
        PrismContext prismContext = pageBase.getPrismContext();
        List relations;
        QName relation = getMemberPanelStorage().getRelation();
        if (QNameUtil.match(relation, PrismConstants.Q_ANY)){
            relations = getSupportedRelations().getAvailableRelationList();
        } else {
            relations = Collections.singletonList(relation);
        }

        ObjectFilter filter;
        Boolean indirect = getMemberPanelStorage().getIndirect();
        Class type = getSearch().getTypeClass();
        if(!Boolean.TRUE.equals(indirect)) {
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
        } else {
            filter = prismContext.queryFor(type)
                    .item(FocusType.F_ROLE_MEMBERSHIP_REF).ref(MemberOperationsHelper.createReferenceValuesList(object, relations))
                    .buildFilter();
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

    protected AvailableRelationDto getSupportedRelations() {
        return null;
    }

    protected MemberPanelStorage getMemberPanelStorage() {
        return null;
    }

    private boolean isRelationVisible() {
        return isSearchItemVisible(searchConfig.getDefaultRelationConfiguration());
    }

    private boolean isIndirectVisible() {
        return isSearchItemVisible(searchConfig.getDefaultIndirectConfiguration());
    }

    private boolean isSearchScopeVisible() {
        if (!isOrg()) {
            return false;
        }
        return isSearchItemVisible(searchConfig.getDefaultSearchScopeConfiguration());
    }

    private boolean isTenantVisible() {
        return isSearchItemVisible(searchConfig.getDefaultTenantConfiguration());
    }

    private boolean isProjectVisible() {
        return isSearchItemVisible(searchConfig.getDefaultProjectConfiguration());
    }

    private boolean isSearchItemVisible(UserInterfaceFeatureType feature) {
        if (feature == null) {
            return true;
        }
        return CompiledGuiProfile.isVisible(feature.getVisibility(), null);
    }

}

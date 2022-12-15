/*
 * Copyright (C) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.roles;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.xml.namespace.QName;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.wicket.model.PropertyModel;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.page.admin.abstractrole.component.MemberOperationsHelper;
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
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

public class AbstractRoleCompositedSearchItem<R extends AbstractRoleType> extends SearchItem {

    private static final Trace LOGGER = TraceManager.getTrace(RelationSearchItem.class);

    public static final String F_SEARCH_ITEMS = "searchItems";
    private List<SearchItem> searchItems = new ArrayList<>();

    private SearchBoxConfigurationHelper searchBoxConfig;
    private boolean role;
    private boolean org;

    SearchBoxScopeType scope;
    QName relation;
    Boolean indirect;

    public AbstractRoleCompositedSearchItem(Search search, SearchBoxConfigurationHelper searchBoxConfig, boolean role, boolean org) {
        super(search);
        this.searchBoxConfig = searchBoxConfig;
        this.role = role;
        this.org = org;
        create();
    }

    public void create() {
        if (searchBoxConfig.isRelationVisible()) {
            searchItems.add(createRelationItem(getSearch()));
        }
        if (searchBoxConfig.isIndirectVisible()) {
            searchItems.add(createIndirectItem(getSearch()));
        }
        if (org && searchBoxConfig.isSearchScopeVisible()) {
            searchItems.add(createScopeItem(getSearch()));
        }
        if (role) {
            if (searchBoxConfig.isTenantVisible()) {
                searchItems.add(createTenantItem(getSearch()));
            }
            if (searchBoxConfig.isProjectVisible()) {
                searchItems.add(createProjectItem(getSearch()));
            }
        }
    }

    private SearchItem createScopeItem(Search search) {
        return new ScopeSearchItem(search, new PropertyModel<>(searchBoxConfig, SearchBoxConfigurationHelper.F_ORG_SEARCH_SCOPE_ITEM));
    }

    private SearchItem createIndirectItem(Search search) {
        return new IndirectSearchItem(search, searchBoxConfig);
    }

    private SearchItem createTenantItem(Search search) {
        return new TenantSearchItem(search, searchBoxConfig);
    }

    private SearchItem createProjectItem(Search search) {
        return new ProjectSearchItem(search, searchBoxConfig);
    }

    private RelationSearchItem createRelationItem(Search search) {
        return new RelationSearchItem(search, searchBoxConfig);
    }

    public ObjectFilter createFilter(PageBase pageBase, VariablesMap variables) {
        ObjectReferenceType parentRef = getParentVariables(variables);
        if (parentRef == null) {
            return null;
        }

        Class type = getSearch().getTypeClass();
        scope = searchBoxConfig.getDefaultSearchScopeConfiguration().getDefaultValue();
        if (SearchBoxScopeType.SUBTREE == scope) {
            return pageBase.getPrismContext().queryFor(type).isChildOf(parentRef.asReferenceValue()).buildFilter();
        }

        PrismContext prismContext = pageBase.getPrismContext();
        List relations;
        relation = searchBoxConfig.getDefaultRelationConfiguration().getDefaultValue();
        if (QNameUtil.match(relation, PrismConstants.Q_ANY)) {
            relations = searchBoxConfig.getSupportedRelations();
        } else {
            relations = Collections.singletonList(relation);
        }

        ObjectFilter filter;
        indirect = searchBoxConfig.getDefaultIndirectConfiguration().isIndirect();

        if (BooleanUtils.isTrue(indirect)) {
            filter = prismContext.queryFor(type)
                    .item(FocusType.F_ROLE_MEMBERSHIP_REF).ref(MemberOperationsHelper.createReferenceValuesList(parentRef, relations))
                    .buildFilter();
        } else {
            S_AtomicFilterExit q = prismContext.queryFor(type).exists(AssignmentHolderType.F_ASSIGNMENT)
                    .block()
                    .item(AssignmentType.F_TARGET_REF)
                    .ref(MemberOperationsHelper.createReferenceValuesList(parentRef, relations));

            if (!searchBoxConfig.isTenantEmpty()) {
                q = q.and().item(AssignmentType.F_TENANT_REF).ref(searchBoxConfig.getTenant().getOid());
            }

            if (!searchBoxConfig.isProjectEmpty()) {
                q = q.and().item(AssignmentType.F_ORG_REF).ref(searchBoxConfig.getProject().getOid());
            }
            filter = q.endBlock().buildFilter();
        }
        return filter;
    }

    public SearchBoxScopeType getScope() {
        return scope;
    }

    public QName getRelation() {
        return relation;
    }

    public Boolean getIndirect() {
        return indirect;
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public Type getSearchItemType() {
        return null;
    }

    private ObjectReferenceType getParentVariables(VariablesMap variables) {
        if (variables == null) {
            return null;
        }
        try {
            return variables.getValue(ExpressionConstants.VAR_PARENT_OBJECT, ObjectReferenceType.class);
        } catch (SchemaException e) {
            LOGGER.error("Couldn't load parent object.");
        }
        return null;
    }

    protected PrismReferenceDefinition getReferenceDefinition(ItemName refName) {
        return PrismContext.get().getSchemaRegistry()
                .findContainerDefinitionByCompileTimeClass(AssignmentType.class)
                .findReferenceDefinition(refName);
    }

    public List<SearchItem> getSearchItems() {
        return searchItems;
    }
}

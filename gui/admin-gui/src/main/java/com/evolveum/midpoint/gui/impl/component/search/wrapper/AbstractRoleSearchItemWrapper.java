/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component.search.wrapper;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.component.search.panel.AbstractSearchItemPanel;
import com.evolveum.midpoint.gui.impl.component.search.panel.MemberSearchPanel;
import com.evolveum.midpoint.gui.impl.page.admin.abstractrole.component.MemberOperationsHelper;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.builder.S_FilterExit;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.util.DisplayableValue;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;

import javax.xml.namespace.QName;
import java.util.List;

public class AbstractRoleSearchItemWrapper extends FilterableSearchItemWrapper {

    public static final String F_SCOPE = "scopeSearchItemWrapper";
    public static final String F_RELATION = "relationSearchItemWrapper";
    public static final String F_INDIRECT = "indirectSearchItemWrapper";
    public static final String F_TENANT = "tenantSearchItemWrapper";
    public static final String F_PROJECT = "projectSearchItemWrapper";
    private static final Trace LOGGER = TraceManager.getTrace(AbstractRoleSearchItemWrapper.class);

    protected String name;
    protected String help;

    private RelationSearchItemWrapper relationSearchItemWrapper;
    private ScopeSearchItemWrapper scopeSearchItemWrapper;
    private IndirectSearchItemWrapper indirectSearchItemWrapper;
    private TenantSearchItemWrapper tenantSearchItemWrapper;
    private ProjectSearchItemWrapper projectSearchItemWrapper;

//    private QName abstractRoleType;

    public AbstractRoleSearchItemWrapper(SearchBoxConfigurationType config) {
//        this.abstractRoleType = absctratRoleType;
        if (config.getRelationConfiguration() != null) {
            relationSearchItemWrapper = new RelationSearchItemWrapper(config.getRelationConfiguration());
        }

        if (config.getIndirectConfiguration() != null) {
            indirectSearchItemWrapper = new IndirectSearchItemWrapper(config.getIndirectConfiguration());
        }

        if (config.getScopeConfiguration() != null) {
            scopeSearchItemWrapper = new ScopeSearchItemWrapper(config.getScopeConfiguration());
        }
        if (config.getProjectConfiguration() != null) {
            projectSearchItemWrapper = new ProjectSearchItemWrapper(config.getProjectConfiguration());
        }
        if (config.getTenantConfiguration() != null) {
            tenantSearchItemWrapper = new TenantSearchItemWrapper(config.getTenantConfiguration());
        }

    }

    @Override
    public ObjectFilter createFilter(Class type, PageBase pageBase, VariablesMap variables) {
        ObjectReferenceType ref = getParentVariables(variables);
        if (ref == null) {
            return null;
        }

        ScopeSearchItemWrapper scope = getScopeSearchItemWrapper();
        if (scope != null && SearchBoxScopeType.SUBTREE.equals(scope.getValue().getValue())) {
            return pageBase.getPrismContext().queryFor(type).isChildOf(ref.asReferenceValue()).buildFilter();
        }

        PrismContext prismContext = pageBase.getPrismContext();

        List<QName> relations = relationSearchItemWrapper.getRelationsForSearch();

        ObjectFilter filter;
        Boolean indirect = indirectSearchItemWrapper.getValue().getValue();

        if(BooleanUtils.isTrue(indirect)) {
            filter = prismContext.queryFor(type)
                    .item(FocusType.F_ROLE_MEMBERSHIP_REF).ref(MemberOperationsHelper.createReferenceValuesList(ref, relations))
                    .buildFilter();
        } else {
            S_FilterExit q = prismContext.queryFor(type).exists(AssignmentHolderType.F_ASSIGNMENT)
                    .block()
                    .item(AssignmentType.F_TARGET_REF)
                    .ref(MemberOperationsHelper.createReferenceValuesList(ref, relations));

//            if (tenantSearchItemWrapper.getValue().getValue() != null) { //TODO is empty?
//                q = q.and().item(AssignmentType.F_TENANT_REF).ref(tenantSearchItemWrapper.getValue().getValue().getOid());
//            }
//
//            if (projectSearchItemWrapper.getValue().getValue() != null) {
//                q = q.and().item(AssignmentType.F_ORG_REF).ref(projectSearchItemWrapper.getValue().getValue().getOid());
//            }
            filter = q.endBlock().buildFilter();
        }
        return filter;
    }

    @Override
    public boolean canRemoveSearchItem() {
        return false;
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

//    public SearchConfigurationWrapper getSearchConfig() {
//        return searchConfig;
//    }

    private ScopeSearchItemWrapper getScopeSearchItemWrapper() {
        return scopeSearchItemWrapper;
    }

    @Override
    public Class<? extends AbstractSearchItemPanel> getSearchItemPanelClass() {
        return MemberSearchPanel.class;
    }

    @Override
    public String getName() {
        return StringUtils.isNotEmpty(name) ? name : null;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getHelp() {
        return StringUtils.isNotEmpty(help) ? help : null;
    }

    @Override
    public String getTitle() {
        return null;
    }

    @Override
    public DisplayableValue getDefaultValue() {
        return null;
    }

    public void setHelp(String help) {
        this.help = help;
    }

    public boolean isSearchScope(SearchBoxScopeType scope) {
        SearchBoxScopeType searchScope = scopeSearchItemWrapper.getValue().getValue();
        return scope == searchScope;
    }

    public boolean isSearchScopeVisible() {
        return scopeSearchItemWrapper != null;
//        return QNameUtil.match(OrgType.COMPLEX_TYPE, abstractRoleType);
    }

    public boolean isRelationVisible() {
        return CollectionUtils.isNotEmpty(getSupportedRelations()) || isSearchScope(SearchBoxScopeType.ONE_LEVEL);
    }

    public boolean isIndirectVisible() {
        return CollectionUtils.isNotEmpty(getSupportedRelations()) && isSearchScope(SearchBoxScopeType.ONE_LEVEL);
    }

    public boolean isParameterSearchVisible() {
        return !isIndirect();// && QNameUtil.match(RoleType.COMPLEX_TYPE, abstractRoleType);
    }

    public List<QName> getSupportedRelations() {
        return relationSearchItemWrapper.getRelationsForSearch();
    }

    public QName getRelationValue() {
        return relationSearchItemWrapper.getValue().getValue();
    }

    public SearchBoxScopeType getScopeValue() {
        return scopeSearchItemWrapper.getValue().getValue();
    }

    public boolean isIndirect() {
        return BooleanUtils.isTrue(indirectSearchItemWrapper.getValue().getValue());
    }

    @Override
    public boolean isVisible() {
        return isNotEmpty();
    }

    public boolean isNotEmpty() {
        return scopeSearchItemWrapper != null || relationSearchItemWrapper != null || indirectSearchItemWrapper != null || tenantSearchItemWrapper != null || projectSearchItemWrapper != null;
    }

}

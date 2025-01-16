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
import com.evolveum.midpoint.gui.impl.page.admin.abstractrole.component.MemberOperationsQueryUtil;
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
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import javax.xml.namespace.QName;
import java.util.List;

public class AbstractRoleSearchItemWrapper extends FilterableSearchItemWrapper {

    public static final String F_SCOPE = "scopeSearchItemWrapper";
    public static final String F_RELATION = "relationSearchItemWrapper";
    public static final String F_INDIRECT = "indirectSearchItemWrapper";
    public static final String F_TENANT = "tenantSearchItemWrapper";
    public static final String F_PROJECT = "projectSearchItemWrapper";
    private static final Trace LOGGER = TraceManager.getTrace(AbstractRoleSearchItemWrapper.class);

    protected IModel<String> name = Model.of();
    protected IModel<String> help = Model.of();

    private RelationSearchItemWrapper relationSearchItemWrapper;
    private ScopeSearchItemWrapper scopeSearchItemWrapper;
    private IndirectSearchItemWrapper indirectSearchItemWrapper;
    private TenantSearchItemWrapper tenantSearchItemWrapper;
    private ProjectSearchItemWrapper projectSearchItemWrapper;

    public AbstractRoleSearchItemWrapper(SearchBoxConfigurationType config) {
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
            return PrismContext.get().queryFor(type).isChildOf(ref.asReferenceValue()).buildFilter();
        }

        PrismContext prismContext = PrismContext.get();

        List<QName> relations = relationSearchItemWrapper.getRelationsForSearch();

        ObjectFilter filter;

        if(isIndirect()) {
            filter = prismContext.queryFor(type)
                    .item(FocusType.F_ROLE_MEMBERSHIP_REF)
                    .ref(MemberOperationsQueryUtil.createReferenceValuesList(ref, relations))
                    .buildFilter();
        } else {
            S_FilterExit q = prismContext.queryFor(type).exists(AssignmentHolderType.F_ASSIGNMENT)
                    .block()
                    .item(AssignmentType.F_TARGET_REF)
                    .ref(MemberOperationsQueryUtil.createReferenceValuesList(ref, relations));

            if (!isIndirect()) {
                if (tenantSearchDefined()) { //TODO is empty?
                    q = q.and().item(AssignmentType.F_TENANT_REF).ref(getTenantOid());
                }

                if (projectSearchDefined()) {
                    q = q.and().item(AssignmentType.F_ORG_REF).ref(getProjectOid());
                }
            }
            filter = q.endBlock().buildFilter();
        }
        return filter;
    }

    private boolean tenantSearchDefined() {
        return getTenantValue() != null;
    }

    public ObjectReferenceType getTenantValue() {
        if (tenantSearchItemWrapper == null) {
            return null;
        }

        return tenantSearchItemWrapper.getValue().getValue();
    }

    private String getTenantOid() {
        ObjectReferenceType tenantValue = getTenantValue();
        if (tenantValue == null) {
            return null;
        }
        return tenantValue.getOid();
    }

    private boolean projectSearchDefined() {
        return getProjectValue() != null;
    }

    public ObjectReferenceType getProjectValue() {
        if (projectSearchItemWrapper == null) {
            return null;
        }

        return projectSearchItemWrapper.getValue().getValue();
    }

    private String getProjectOid() {
        ObjectReferenceType projectValue = getProjectValue();
        if (projectValue == null) {
            return null;
        }
        return projectValue.getOid();
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

    public ScopeSearchItemWrapper getScopeSearchItemWrapper() {
        return scopeSearchItemWrapper;
    }

    @Override
    public Class<? extends AbstractSearchItemPanel> getSearchItemPanelClass() {
        return MemberSearchPanel.class;
    }

    @Override
    public IModel<String> getName() {
        return StringUtils.isNotEmpty(name.getObject()) ? name : Model.of();
    }

    public void setName(IModel<String> name) {
        this.name = name;
    }

    @Override
    public IModel<String> getHelp() {
        return StringUtils.isNotEmpty(help.getObject()) ? help : Model.of();
    }

    @Override
    public IModel<String> getTitle() {
        return Model.of();
    }

    @Override
    public DisplayableValue getDefaultValue() {
        return null;
    }

    public void setHelp(IModel<String> help) {
        this.help = help;
    }

    public boolean isSearchScope(SearchBoxScopeType scope) {
        SearchBoxScopeType searchScope = getScopeValue();
        return scope == searchScope;
    }

    public boolean isSearchScopeVisible() {
        return scopeSearchItemWrapper != null && scopeSearchItemWrapper.isVisible();
    }

    public boolean isRelationVisible() {
        boolean wrapperVisibility = relationSearchItemWrapper == null || relationSearchItemWrapper.isVisible();
        return wrapperVisibility && (CollectionUtils.isNotEmpty(getSupportedRelations()) || isSearchScope(SearchBoxScopeType.ONE_LEVEL));
    }

    public boolean isIndirectVisible() {
        boolean wrapperVisibility = indirectSearchItemWrapper == null || indirectSearchItemWrapper.isVisible();
        return wrapperVisibility && !isSearchScope(SearchBoxScopeType.SUBTREE);
    }

    public boolean isTenantVisible() {
        boolean wrapperVisibility = tenantSearchItemWrapper == null || tenantSearchItemWrapper.isVisible();
        return wrapperVisibility && isParameterSearchVisible();
    }

   public boolean isProjectVisible() {
        boolean wrapperVisibility = projectSearchItemWrapper == null || projectSearchItemWrapper.isVisible();
        return wrapperVisibility && isParameterSearchVisible();
    }

    public boolean isParameterSearchVisible() {
        return !isIndirect();
    }

    public List<QName> getSupportedRelations() {
        return relationSearchItemWrapper.getSupportedRelations();
    }

    public QName getRelationValue() {
        return relationSearchItemWrapper.getValue().getValue();
    }

    public RelationSearchItemConfigurationType getRelationSearchItemConfiguration() {
        return relationSearchItemWrapper.getRelationSearchItemConfigurationType();
    }

    public SearchBoxScopeType getScopeValue() {
        if (scopeSearchItemWrapper == null) {
            return null;
        }
        return scopeSearchItemWrapper.getValue().getValue();
    }

    public boolean isIndirect() {
        if (indirectSearchItemWrapper == null) {
            return false;
        }
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

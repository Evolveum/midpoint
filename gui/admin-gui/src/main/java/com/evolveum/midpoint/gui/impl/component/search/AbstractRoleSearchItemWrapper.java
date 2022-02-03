/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component.search;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.page.admin.abstractrole.component.MemberOperationsHelper;
import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.builder.S_AtomicFilterExit;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.lang3.BooleanUtils;

import javax.xml.namespace.QName;
import java.util.Collections;
import java.util.List;

public abstract class AbstractRoleSearchItemWrapper extends AbstractSearchItemWrapper {

    private static final Trace LOGGER = TraceManager.getTrace(AbstractRoleSearchItemWrapper.class);
    private SearchConfigurationWrapper searchConfig;

    public static final String F_SEARCH_CONFIG = "searchConfig";

    public AbstractRoleSearchItemWrapper(SearchConfigurationWrapper searchConfig) {
        this.searchConfig = searchConfig;
    }

    @Override
    public ObjectFilter createFilter(PageBase pageBase, VariablesMap variables) {
        AbstractRoleType object = getParentVariables(variables);
        if (object == null) {
            return null;
        }

        Class type = getSearchConfig().getTypeClass();
        SearchBoxScopeType scope = getSearchConfig().getConfig().getScopeConfiguration().getDefaultValue();
        if (SearchBoxScopeType.SUBTREE == scope) {
            ObjectReferenceType ref = ObjectTypeUtil.createObjectRef(object, (QName) null);
            return pageBase.getPrismContext().queryFor(type).isChildOf(ref.asReferenceValue()).buildFilter();
        }

        PrismContext prismContext = pageBase.getPrismContext();
        List relations;
        QName relation = getSearchConfig().getConfig().getRelationConfiguration().getDefaultValue();
        if (QNameUtil.match(relation, PrismConstants.Q_ANY)){
            relations = getSearchConfig().getConfig().getRelationConfiguration().getSupportedRelations();
        } else {
            relations = Collections.singletonList(relation);
        }

        ObjectFilter filter;
        Boolean indirect = getSearchConfig().getConfig().getIndirectConfiguration().isIndirect();

        if(BooleanUtils.isTrue(indirect)) {
            filter = prismContext.queryFor(type)
                    .item(FocusType.F_ROLE_MEMBERSHIP_REF).ref(MemberOperationsHelper.createReferenceValuesList(object, relations))
                    .buildFilter();
        } else {
            S_AtomicFilterExit q = prismContext.queryFor(type).exists(AssignmentHolderType.F_ASSIGNMENT)
                    .block()
                    .item(AssignmentType.F_TARGET_REF)
                    .ref(MemberOperationsHelper.createReferenceValuesList(object, relations));

            if (!getSearchConfig().isTenantEmpty()) {
                q = q.and().item(AssignmentType.F_TENANT_REF).ref(getSearchConfig().getTenantRef().getOid());
            }

            if (!getSearchConfig().isProjectEmpty()) {
                q = q.and().item(AssignmentType.F_ORG_REF).ref(getSearchConfig().getProjectRef().getOid());
            }
            filter = q.endBlock().buildFilter();
        }
        return filter;
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

    public SearchConfigurationWrapper getSearchConfig() {
        return searchConfig;
    }

//    public void setSearchConfig(SearchConfigurationWrapper searchConfig) {
//        this.searchConfig = searchConfig;
//    }
}
